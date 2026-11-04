package me.nghlong3004.longnest.configuration;

import java.time.Clock;
import java.time.ZoneId;
import me.nghlong3004.longnest.conversation.ConversationHistory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LedgerConfiguration {

  @Bean
  ConversationHistory conversationHistory() {
    return new ConversationHistory();
  }

  @Bean
  Clock ledgerClock() {
    return Clock.systemUTC();
  }

  @Bean
  ZoneId ledgerTimeZone(@Value("${longnest.ledger.time-zone}") String timeZone) {
    return ZoneId.of(timeZone);
  }
}
