package com.dayone.scraper;

import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceScraper implements Scraper {

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=1mo";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";

    private static final long START_TIME = 86400;   // 60 * 60 * 24

    @Override
    public ScrapedResult scrap(Company company) {
        ScrapedResult scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);

        try {
            long now = System.currentTimeMillis() / 1000;

            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);
            Connection connection = Jsoup.connect(url);
            Document document = connection.get();

            Elements parsingDivs = document.getElementsByAttributeValue("data-test", "historical-prices");
            Element tableEle = parsingDivs.get(0);  // table 전체

            Element tbody = tableEle.children().get(1);


            List<Dividend> dividends = new ArrayList<>();
            for (Element e : tbody.children()) {
                String txt = e.text();
                if (!txt.endsWith("Dividend")) {
                    continue;
                }

                String[] splits = txt.split(" ");
                int month = Month.strToNumber(splits[0]);
                int day = Integer.valueOf(splits[1].replace(",", ""));
                int year = Integer.valueOf(splits[2]);
                String dividend = splits[3];

                if (month < 0) {
                    throw new RuntimeException("Unexpected Month enum value -> " + splits[0]);
                }

                dividends.add(Dividend.builder()
                                .date(LocalDateTime.of(year,month,day,0,0))
                                .dividend(dividend)
                                .build());

            }
            scrapResult.setDividends(dividends);

        } catch (IOException e) {
            // TODO error handling
            e.printStackTrace();
        }

        return scrapResult;
    }

    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Thread.sleep(3000); // 요청 전에 3초 대기
            
            //503 error 처리
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            Elements titleElements = document.getElementsByTag("h1");

            // h1 태그가 존재하는지 확인
            if (titleElements.size() > 1) { //두번째 h1 태그를 참조
                Element titleEle = titleElements.get(1); // 사이트 html 변경됨 0번은 "Yahoo Finance"를 가져옴
                String titleText = titleEle.text();
                String title;

                if (titleText.contains("(")) {
                    title = titleText.split("\\(")[0].trim(); // "(" 기준으로 분할
                } else {
                    title = titleText.trim(); // 없으면 그냥 trim 처리
                }

                return new Company(ticker, title);
            } else {
                System.err.println("No h1 tags found for ticker: " + ticker);
            }
        } catch (IOException e) {
            System.err.println("Error fetching company data for ticker: " + ticker);
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
        }
        return null; // 회사 정보를 가져오지 못한 경우 null 반환
    }



}
