package aggregator.controller;

import aggregator.entity.Transaction;
import aggregator.logic.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/aggregate")
    public ResponseEntity<?> getAggregate(@RequestParam("account") String account) {
        try {
            List<Transaction> allTransactions = applicationService.getAllTransactionsOfAccount(account);
            return ResponseEntity.ok(allTransactions);
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Error fetching transactions: " + e.getMessage());
            return ResponseEntity.status(500).body("Error fetching transactions");
        }
    }
}
