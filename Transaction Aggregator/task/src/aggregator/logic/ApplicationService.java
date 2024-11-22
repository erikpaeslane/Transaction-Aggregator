package aggregator.logic;

import aggregator.entity.Transaction;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ApplicationService {

    private static final int MAX_RETRIES = 5;

    @Async
    public CompletableFuture<List<Transaction>> fetchTransactionsFromService(String url, RestTemplate restTemplate) {
        return CompletableFuture.supplyAsync(() -> fetchTransactionsWithRetries(url, restTemplate));
    }

    @Cacheable(value = "transactionsCache", key = "#account")
    public List<Transaction> getAllTransactionsOfAccount(String account)
            throws ExecutionException, InterruptedException {

        RestTemplate restTemplate = new RestTemplate();
        String url1 = "http://localhost:8888/transactions?account=" + account;
        String url2 = "http://localhost:8889/transactions?account=" + account;

        CompletableFuture<List<Transaction>> future1 = fetchTransactionsFromService(url1, restTemplate);
        CompletableFuture<List<Transaction>> future2 = fetchTransactionsFromService(url2, restTemplate);

        CompletableFuture.allOf(future1, future2).join();

        List<Transaction> allTransactions = new ArrayList<>();
        allTransactions.addAll(future1.get());
        allTransactions.addAll(future2.get());

        allTransactions.sort(Comparator.comparing(Transaction::getTimestamp).reversed());

        return allTransactions;
    }

    public List<Transaction> fetchTransactionsWithRetries(String url, RestTemplate restTemplate) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                ResponseEntity<List<Transaction>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<Transaction>>() {
                        });
                if (response.getStatusCode() == HttpStatus.OK) {
                    return response.getBody() != null ? response.getBody() : new ArrayList<>();
                }

                if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ||
                        response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    attempt++;
                } else {
                    break;
                }

            } catch (Exception e) {
                System.err.println("Error during request to " + url + ": " + e.getMessage());
                attempt++;
            }
        }
        return new ArrayList<>();
    }
}
