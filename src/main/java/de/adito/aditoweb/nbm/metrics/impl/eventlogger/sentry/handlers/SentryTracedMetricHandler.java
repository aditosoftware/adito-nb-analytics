package de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry.handlers;

import com.google.common.cache.*;
import de.adito.aditoweb.nbm.metrics.api.types.Traced;
import de.adito.aditoweb.nbm.metrics.impl.handlers.*;
import lombok.*;
import lombok.extern.java.Log;
import org.jetbrains.annotations.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Handler for all methods that should be {@link Traced}.
 * This is useful if you want to track down performance problems.
 *
 * @author w.glanzer, 02.03.2023
 */
@Log
@MetricHandler(metric = Traced.class)
class SentryTracedMetricHandler implements IMetricHandler<Traced>
{

  private static final String HINT_FINISHABLE = SentryTracedMetricHandler.class.getName() + "_finishable";

  private final Cache<String, SentryTracedTransaction> transactionCache = CacheBuilder.newBuilder()
      .weakValues()
      .build();

  @Override
  public void beforeMethod(@NonNull Traced pAnnotation, @Nullable Object pObject, @NonNull Method pMethod, Object @NonNull [] pArgs, @NonNull Map<String, Object> pHints)
  {
    String transactionKey = pAnnotation.transaction();
    boolean transactionStarted = false;

    // Add the threadid to the transaction key, if asynchronous calls should be handled as different transactions
    if (!pAnnotation.combineAsyncTransactions())
      transactionKey += "_" + Thread.currentThread().getId();

    // Get the current transaction, or start a new one if currently nothing is active and a transaction should be started
    SentryTracedTransaction transaction = transactionCache.getIfPresent(transactionKey);
    if ((transaction == null || transaction.isFinished()) && pAnnotation.startTransaction())
    {
      transaction = new SentryTracedTransaction(pAnnotation.transaction());
      transactionStarted = true;
      transactionCache.put(transactionKey, transaction);
    }

    if (transaction != null && !transaction.isFinished())
    {
      // trigger, that a new span should be started
      SentryTracedTransaction.SpanID spanID = transaction.startSpan(pMethod.getName(), pMethod.getDeclaringClass().getName());

      // Add a finishable to the hints so we can finish the span without caching it in a static cache
      Object previousFinishable = pHints.put(HINT_FINISHABLE, new Finishable(transaction, spanID, transactionStarted));

      // if we accidentally override a finishable, then log it -> we can't do anything to fix this, it is caused by the annotator
      if (previousFinishable != null)
        log.warning("Tried to override finishable for object " + pObject + " in method " + pMethod + " and arguments " + Arrays.toString(pArgs));
    }
  }

  @Override
  public void afterMethod(@NonNull Traced pAnnotation, @Nullable Object pObject, @NonNull Method pMethod, Object @NonNull [] pArgs, @Nullable Object pResult,
                          @Nullable Throwable pException, @NonNull Map<String, Object> pHints)
  {
    // Execute finishables, if anything has to be finished
    Object finishable = pHints.remove(HINT_FINISHABLE);
    if (finishable instanceof Finishable)
      ((Finishable) finishable).finish(pException);
  }

  /**
   * Object that can be finished
   */
  @RequiredArgsConstructor
  private static class Finishable
  {
    /**
     * Transaction that should be finished, if the transaction was started before
     */
    @NotNull
    private final SentryTracedTransaction transaction;

    /**
     * Span to finish
     */
    @NotNull
    private final SentryTracedTransaction.SpanID spanID;

    /**
     * true, if the transaction was started before
     */
    private final boolean transactionStarted;

    /**
     * Gets invoked if the finishable should finish its underlying objects
     *
     * @param pMethodException Exception that was thrown from the original method
     */
    public void finish(@Nullable Throwable pMethodException)
    {
      // Schedule a span finish, because we exited the annotated method
      transaction.scheduleSpanFinish(spanID, pMethodException);

      // finish the whole transaction, if we were the transaction-start trigger
      if (transactionStarted)
        transaction.finish(pMethodException);
    }
  }

}
