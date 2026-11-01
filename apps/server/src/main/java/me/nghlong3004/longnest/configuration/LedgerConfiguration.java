package me.nghlong3004.longnest.configuration;

import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LedgerConfiguration {

  @Bean
  ZoneId ledgerTimeZone(@Value("${longnest.ledger.time-zone}") String timeZone) {
    return ZoneId.of(timeZone);
  }
}
