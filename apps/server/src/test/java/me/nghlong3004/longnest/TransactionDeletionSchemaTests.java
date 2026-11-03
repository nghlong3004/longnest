package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.main.keep-alive=false",
      "longnest.telegram.enabled=false",
      "longnest.deepseek.enabled=false"
    })
@Import(TestcontainersConfiguration.class)
class TransactionDeletionSchemaTests {
  @Autowired JdbcTemplate jdbc;

  @Test
  void permitsTerminalDeletedDraftReceiptWithoutLedgerReference() {
    assertThatCode(() ->
            jdbc.execute(
                """
                DO $$ DECLARE owner_id uuid := gen_random_uuid(); BEGIN
                  INSERT INTO users(id) VALUES(owner_id);
                  INSERT INTO transaction_drafts(user_id, channel, chat_id, message_id, original_text, status)
                  VALUES(owner_id, 'TELEGRAM', 'schema-delete', '1', 'synthetic', 'DELETED');
                END $$;
                """))
        .doesNotThrowAnyException();
  }
}
