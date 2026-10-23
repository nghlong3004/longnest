package me.nghlong3004.longnest.input;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Local capture remains available independently of provider activation. */
@Configuration(proxyBeanMethods = false)
@Profile("ledger & !ledger-init & !smoke")
public class InputConfiguration {
  @Bean
  InputInterpreter rulesInterpreter() { return new TransactionInterpreter(); }
}
