package de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry.handlers;

import de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry.handlers.SentryTracedTransaction.SpanID;
import io.sentry.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Test for {@link SentryTracedTransaction}
 *
 * @author w.glanzer, 10.03.2023
 * @see SentryTracedTransaction
 */
@SuppressWarnings("UnstableApiUsage")
class SentryTracedTransactionTest
{

  private final List<ISpan> mockedSpans = new ArrayList<>();
  private MockedStatic<Sentry> sentryMockedStatic;

  /**
   * Mock static {@link Sentry} stuff to be able to
   * check, if spans are created correctly
   */
  @BeforeEach
  void setUp()
  {
    Map<ISpan, Map<String, Object>> spanData = new HashMap<>();
    sentryMockedStatic = mockStatic(Sentry.class);
    sentryMockedStatic.when(Sentry::getCurrentHub)
        .thenAnswer(pInv -> {
          IHub hub = mock(IHub.class);
          when(hub.getOptions()).thenReturn(new SentryOptions());
          return hub;
        });
    sentryMockedStatic.when(() -> Sentry.startTransaction(any(String.class), any(String.class)))
        .thenAnswer(pInv -> {
          ITransaction transaction = mock(ITransaction.class);
          when(transaction.getName()).thenReturn(pInv.getArgument(0));
          when(transaction.getLatestActiveSpan()).thenAnswer(pLASinv -> {
            if (!mockedSpans.isEmpty())
            {
              for (int i = mockedSpans.size() - 1; i >= 0; i--)
              {
                if (!mockedSpans.get(i).isFinished())
                {
                  return mockedSpans.get(i);
                }
              }
            }
            return null;
          });
          when(transaction.startChild(any(), any())).thenAnswer(new StartChildAnswer(spanData));
          return transaction;
        });
  }

  /**
   * Contains everything to check, if spans are combined correctly
   */
  @Nested
  class ShouldCombineSpans
  {
    /**
     * Check combination of simple method calls, one after another
     */
    @Test
    void onSimpleCalls()
    {
      SentryTracedTransaction transaction = new SentryTracedTransaction("myName");
      transaction.scheduleSpanFinish(transaction.startSpan("operation1", null), null);
      transaction.scheduleSpanFinish(transaction.startSpan("operation1", null), null);
      transaction.scheduleSpanFinish(transaction.startSpan("operation1", null), null);

      assertEquals(1, mockedSpans.size());
      assertEquals("operation1", mockedSpans.iterator().next().getOperation());
    }

    /**
     * Check combination of nested method calls
     */
    @Test
    void onNestedCalls()
    {
      SentryTracedTransaction transaction = new SentryTracedTransaction("myName");
      SpanID operation1 = transaction.startSpan("operation1", null);
      SpanID operation2 = transaction.startSpan("operation2", null);
      SpanID operation3 = transaction.startSpan("operation3", null);
      transaction.scheduleSpanFinish(operation3, null);
      operation3 = transaction.startSpan("operation3", null);
      transaction.scheduleSpanFinish(operation3, null);
      transaction.scheduleSpanFinish(operation2, null);
      transaction.scheduleSpanFinish(operation1, null);

      assertEquals(3, mockedSpans.size());
    }
  }

  /**
   * Checks, if different operations are not combined
   */
  @Test
  void shouldNotCombineSpans()
  {
    SentryTracedTransaction transaction = new SentryTracedTransaction("myName");
    transaction.scheduleSpanFinish(transaction.startSpan("operation1", null), null);
    transaction.scheduleSpanFinish(transaction.startSpan("operation2", null), null);
    transaction.scheduleSpanFinish(transaction.startSpan("operation1", null), null);

    assertEquals(3, mockedSpans.size());
  }

  /**
   * Close all static mocks and clear the spans to release memory
   */
  @AfterEach
  void tearDown()
  {
    mockedSpans.clear();
    sentryMockedStatic.close();
  }

  /**
   * Answer to {@link Span#startChild(String)}
   */
  @RequiredArgsConstructor
  private class StartChildAnswer implements Answer<Object>
  {
    /**
     * Currently known span data
     */
    @NotNull
    private final Map<ISpan, Map<String, Object>> spanData;

    @Override
    public Object answer(InvocationOnMock pChildInv)
    {
      ISpan span = mock(Span.class);
      doAnswer(pSetDataInv -> spanData.computeIfAbsent(span, pS -> new HashMap<>()).put(pSetDataInv.getArgument(0), pSetDataInv.getArgument(1)))
          .when(span).setData(any(), any());
      when(span.getData(any())).thenAnswer(pGetDataInv -> spanData.get(span).get(pGetDataInv.getArgument(0, String.class)));
      when(span.getOperation()).thenReturn(pChildInv.getArgument(0));
      when(span.getDescription()).thenReturn(pChildInv.getArgument(1));
      when(span.startChild(any(), any())).thenAnswer(this);
      mockedSpans.add(span);
      return span;
    }
  }
}
