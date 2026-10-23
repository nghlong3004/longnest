package me.nghlong3004.longnest.ledger.config;

import java.security.SecureRandom;
import java.time.Clock;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("ledger")
public class LedgerConfiguration {
  @Bean
  PersonalOwner personalOwner(@Value("${longnest.ledger.owner}") String owner) {
    return new PersonalOwner(owner);
  }

  @Bean
  Clock ledgerClock() {
    return Clock.systemUTC();
  }

  @Bean
  LedgerCursors ledgerCursors(Clock clock) {
    return new LedgerCursors(clock, new SecureRandom());
  }
}
