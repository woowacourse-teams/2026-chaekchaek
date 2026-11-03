package com.chaekchaek.book.cache;

import com.chaekchaek.book.service.BookSearchActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("perf-no-cache")
public class BookSearchNoCachePerformanceConfiguration {

    @Bean
    @Primary
    BookSearchActivityCountReader uncachedBookSearchActivityCountReader(BookActivityCountReader databaseReader) {
        return databaseReader::getActivityCounts;
    }
}
