package me.nghlong3004.longnest.ledger.config;

import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.service.TransactionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** One-shot operator command, never a normal ledger or Telegram capability. */
@Slf4j
@Configuration(proxyBeanMethods = false)
@Profile("ledger-init")
public class LedgerInitConfiguration {
  @Bean
  ApplicationRunner initializeLedgerOwner(PersonalOwner owner, TransactionService transactions,
      ConfigurableApplicationContext context) {
    return args -> {
      try {
        transactions.initializeOwner(new HostContext(owner.principal(), null, null, null));
        log.info("Ledger owner head initialized; restart with the ledger profile");
      } catch (Exception failure) {
        // Do not expose persistence exception text, URL, credentials or owner.
        throw new IllegalStateException("Ledger initialization failed; inspect storage before retry");
      } finally {
        context.close();
      }
    };
  }
}
