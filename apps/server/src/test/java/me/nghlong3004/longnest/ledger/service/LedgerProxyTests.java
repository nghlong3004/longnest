package me.nghlong3004.longnest.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.ControlProof;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real Spring proxy mechanics with a test-only transaction manager, not database proof. */
class LedgerProxyTests {
  private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
  private static final Instant NOW = Instant.parse("2026-10-21T00:00:00Z");
  private final HostContext context = new HostContext("personal", "event-1", NOW, NOW);
  private final PersonalOwner owner = new PersonalOwner("personal");
  private final LedgerStore store = LedgerUnitTests.mock(LedgerStore.class);
  private final LedgerQueryService queries = LedgerUnitTests.mock(LedgerQueryService.class);
  private final UnitTransactions manager = new UnitTransactions();

  @AfterAll
  static void closeValidator() {
    VALIDATORS.close();
  }

  private LedgerClient client() {
    var inbox = new AtomicReference<LedgerStore.Inbox>();
    when(store.inbox(any(), any())).thenAnswer(call -> Optional.ofNullable(inbox.get()));
    when(store.controlByRequest(any(), any())).thenReturn(Optional.empty());
    when(store.controlEventFingerprint(any(), any())).thenReturn(Optional.empty());
    doAnswer(call -> { inbox.set(call.getArgument(0)); return null; }).when(store).freeze(any());
    when(store.insert(any(), any(), any(), any())).thenAnswer(call -> {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      return new LedgerStore.Entry(call.getArgument(0), 42, 1, call.getArgument(1),
          call.getArgument(2), call.getArgument(3), false);
    });
    var target = new TransactionServiceImpl(owner, store,
        new TransactionMapper(VALIDATORS.getValidator(),
            Mappers.getMapper(TransactionRepresentationMapper.class)),
        Clock.fixed(NOW, ZoneOffset.UTC), new ControlProofs());
    var interceptor = new TransactionInterceptor();
    interceptor.setTransactionManager(manager);
    interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
    var proxy = new ProxyFactory(target);
    proxy.setInterfaces(TransactionService.class);
    proxy.addAdvice(interceptor);
    return new LedgerClient(owner, (TransactionService) proxy.getProxy(), queries);
  }

  @Test
  void adapterReturnsOkOnlyAfterBothProxyTransactionsCommitAndCleansThreadState() {
    var result = client().create(context, LedgerUnitTests.input("45000"));
    assertThat(result).isInstanceOf(LedgerResult.Ok.class);
    assertThat(manager.events).containsExactly("begin", "commit", "begin", "commit");
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void devPreparationUsesProxyTransactionAndPropagatesUncertainCommit() {
    var target = new TransactionServiceImpl(owner, store,
        new TransactionMapper(VALIDATORS.getValidator(),
            Mappers.getMapper(TransactionRepresentationMapper.class)),
        Clock.fixed(NOW, ZoneOffset.UTC), new ControlProofs());
    var interceptor = new TransactionInterceptor();
    interceptor.setTransactionManager(manager);
    interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
    var proxy = new ProxyFactory(target);
    proxy.setInterfaces(TransactionService.class);
    proxy.addAdvice(interceptor);
    var transactions = (TransactionService) proxy.getProxy();
    doAnswer(call -> {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      return null;
    }).when(store).prepareOwner("personal");
    manager.failCommit = 1;
    assertThatThrownBy(() -> transactions.prepareOwner(
        new HostContext("personal", null, null, null))).isInstanceOf(TransactionSystemException.class);
    assertThat(manager.events).containsExactly("begin", "commit-failed");
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void l2AdapterSeparatesConfirmedRollbackFromUnknownCommitAndSharesProxyBoundary() {
    var proof = new ControlProofs().issue("tg:123:77", 101, "callback-1",
        "a".repeat(32), 42, 1);
    var transactions = mock(TransactionService.class);
    var adapter = new LedgerClient(owner, transactions, queries);
    var host = new HostContext("personal", "callback-1", NOW, NOW, "tg:123:77");
    when(transactions.update(host, "a".repeat(32), proof))
        .thenThrow(new DataIntegrityViolationException("synthetic rollback"))
        .thenThrow(new TransactionSystemException("synthetic uncertain commit"));
    var known = (LedgerResult.Failure<?>) adapter.update(host, "a".repeat(32), proof);
    assertThat(known.code()).isEqualTo(LedgerException.Code.NOT_SAVED);
    var unknown = (LedgerResult.Failure<?>) adapter.update(host, "a".repeat(32), proof);
    assertThat(unknown.code()).isEqualTo(LedgerException.Code.OUTCOME_UNKNOWN);
    assertThat(unknown.operationId()).contains("callback-1");
    try {
      var method = TransactionServiceImpl.class.getMethod("update", HostContext.class,
          String.class, ControlProof.class);
      var attribute = new AnnotationTransactionAttributeSource()
          .getTransactionAttribute(method, TransactionServiceImpl.class);
      assertThat(attribute).isNotNull();
      assertThat(attribute.getPropagationBehavior())
          .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    } catch (NoSuchMethodException failure) {
      throw new AssertionError(failure);
    }
  }

  @Test
  void initializerUsesProxyTransactionAndCommitFailureCannotReportSuccess() {
    var target = new TransactionServiceImpl(owner, store,
        new TransactionMapper(VALIDATORS.getValidator(),
            Mappers.getMapper(TransactionRepresentationMapper.class)),
        Clock.fixed(NOW, ZoneOffset.UTC), new ControlProofs());
    var interceptor = new TransactionInterceptor();
    interceptor.setTransactionManager(manager);
    interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
    var proxy = new ProxyFactory(target);
    proxy.setInterfaces(TransactionService.class);
    proxy.addAdvice(interceptor);
    var transactions = (TransactionService) proxy.getProxy();
    doAnswer(call -> {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      return null;
    }).when(store).initializeOwner("personal");
    manager.failCommit = 1;
    assertThatThrownBy(() -> transactions.initializeOwner(
        new HostContext("personal", null, null, null)))
        .isInstanceOf(TransactionSystemException.class);
    assertThat(manager.events).containsExactly("begin", "commit-failed");
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void createCommitFailureCannotEscapeAsSuccessOrNotSaved() {
    manager.failCommit = 2;
    var result = client().create(context, LedgerUnitTests.input("45000"));
    assertThat(result).isInstanceOf(LedgerResult.Failure.class);
    var failure = (LedgerResult.Failure<?>) result;
    assertThat(failure.code()).isEqualTo(LedgerException.Code.OUTCOME_UNKNOWN);
    assertThat(failure.operationId()).contains("event-1");
    assertThat(failure.retryable()).isTrue();
    assertThat(manager.events).containsExactly("begin", "commit", "begin", "commit-failed");
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void knownRolledBackConstraintFailureIsTranslatedOnlyAfterRollback() {
    var client = client();
    doThrow(new DataIntegrityViolationException("synthetic"))
        .when(store).complete(any(), any(), any());
    var result = client.create(context, LedgerUnitTests.input("45000"));
    assertThat(result).isInstanceOfSatisfying(LedgerResult.Failure.class,
        failure -> assertThat(failure.code()).isEqualTo(LedgerException.Code.NOT_SAVED));
    assertThat(manager.events).containsExactly("begin", "commit", "begin", "rollback");
  }

  @Test
  void foreignCallerIsRejectedBeforeEvenOpeningAProxyTransaction() {
    var result = client().create(new HostContext("foreign", "other", NOW, NOW), null);
    assertThat(result).isInstanceOfSatisfying(LedgerResult.Failure.class,
        failure -> assertThat(failure.code())
            .isEqualTo(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN));
    assertThat(manager.events).isEmpty();
  }

  @Test
  void adapterRejectsNestedCallerTransactionsInsteadOfSuspendingTheirLocks() {
    var client = client();
    try {
      TransactionSynchronizationManager.setActualTransactionActive(true);
      var result = client.create(context, LedgerUnitTests.input("45000"));
      assertThat(result).isInstanceOfSatisfying(LedgerResult.Failure.class,
          failure -> assertThat(failure.code()).isEqualTo(LedgerException.Code.INVALID_STATE));
      assertThat(manager.events).isEmpty();
    } finally {
      TransactionSynchronizationManager.clear();
    }
  }

  private static final class UnitTransactions extends AbstractPlatformTransactionManager {
    private final List<String> events = new ArrayList<>();
    private int commits;
    private int failCommit;

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      assertThat(definition.getIsolationLevel())
          .isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
      events.add("begin");
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
      if (++commits == failCommit) {
        events.add("commit-failed");
        throw new TransactionSystemException("synthetic commit outcome uncertainty");
      }
      events.add("commit");
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
      events.add("rollback");
    }
  }
}
