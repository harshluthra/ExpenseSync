package org.example.controller;


import org.example.dto.RawBalanceResponse;
import org.example.dto.SimplifiedBalanceResponse;
import org.example.service.BalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/raw")
    public ResponseEntity<RawBalanceResponse> getRawBalance(@RequestParam String email) {
        return ResponseEntity.ok(balanceService.getRawBalance(email));
    }

    @GetMapping("/simplified")
    public ResponseEntity<SimplifiedBalanceResponse> getSimplifiedBalance(@RequestParam String email) {
        return ResponseEntity.ok(balanceService.getSimplifiedBalance(email));
    }

}
