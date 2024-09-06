package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistUserException;
import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRepository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import com.dayone.scraper.YahooFinanceScraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Trie<String, String> trie;
    private final YahooFinanceScraper yahooFinanceScraper; // 회사 정보를 스크래핑하는 스크래퍼

    public Company save(String ticker) {
        if (companyRepository.existsByTicker(ticker)) {
            throw new RuntimeException("회사가 이미 존재합니다");
        }

        // 회사 정보를 스크래핑
        Company company = yahooFinanceScraper.scrapCompanyByTicker(ticker);

        // 스크래핑 결과가 null인 경우 예외 처리
        if (company == null) {
            throw new RuntimeException("회사를 찾을 수 없습니다: " + ticker);
        }

        //데이터베이스에 저장
        CompanyEntity companyEntity = new CompanyEntity(company);
        companyRepository.save(companyEntity);

        return company;
    }

    //companycontroller -> searchCompany
    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {
        // 주어진 티커로 회사를 스크래핑
        Company company = yahooFinanceScraper.scrapCompanyByTicker(ticker);

        // 회사가 존재하는지 확인
        CompanyEntity companyEntity = companyRepository.findByTicker(ticker)
                .orElseGet(() -> new CompanyEntity(company));

        if (companyEntity.getId() == null) {
            companyRepository.save(companyEntity);
        }

        // 배당금 정보를 스크래핑하여 저장
        List<Dividend> dividends = scrapeDividendsForCompany(ticker);
        for (Dividend dividend : dividends) {
            DividendEntity dividendEntity = new DividendEntity(companyEntity.getId(), dividend);
            dividendRepository.save(dividendEntity);
        }

        return company;
    }

    private List<Dividend> scrapeDividendsForCompany(String ticker) {
        // 주어진 티커에 대한 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = yahooFinanceScraper.scrap(new Company(ticker, ""));
        return scrapedResult.getDividends();  // 스크래핑한 배당금 리스트 반환
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        return this.trie.prefixMap(keyword).keySet().stream()
                .filter(k -> k instanceof String) // 타입 확인
                .map(k -> (String) k) // Object를 String로
                .limit(10) // 10개
                .collect(Collectors.toList());
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream()
                .collect(Collectors.toList());
    }

    public String deleteCompany(String ticker) {
        // 회사 엔티티 찾기
        CompanyEntity companyEntity = companyRepository.findByTicker(ticker)
                .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다"));

        // 배당금 정보 삭제
        dividendRepository.deleteAllByCompanyId(companyEntity.getId());

        // 회사 정보 삭제
        companyRepository.delete(companyEntity);

        return ticker;
    }
}


