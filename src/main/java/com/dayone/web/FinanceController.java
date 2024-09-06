package com.dayone.web;

import com.dayone.model.ScrapedResult;
import com.dayone.service.FinanceService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/finance")
@AllArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/dividend/{companyName}")
    public ResponseEntity<?> searchFinance(@PathVariable String companyName) {
        try {
            ScrapedResult result = this.financeService.getDividendByCompanyName(companyName);
            if (result == null || result.getCompany() == null) {
                return ResponseEntity.badRequest().body("회사를 찾을 수 없습니다: " + companyName);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("400 Bad Request");
        }
    }
}
