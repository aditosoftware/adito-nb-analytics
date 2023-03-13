package de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry.handlers;

import io.sentry.*;
import lombok.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Encapsulates a sentry {@link ITransaction} so that we are
 * able to "reuse" existing spans and combine them.
 *
 * @author w.glanzer, 10.03.2023
 */
class SentryTracedTransaction
{

  private static final String DATA_POSSIBLE_RESULT = "ADITO_result";
  private static final String DATA_POSSIBLE_RESULT_TIME = "ADITO_result_time";
  private static final String DATA_SPANID = "ADITO_spanID";
  private static final String DATA_INVOCATIONCOUNT = "ADITO_invocations";
  private final ITransaction transaction;
  private final AtomicReference<ISpan> lastActiveSpanRef = new AtomicReference<>(null);

  /**
   * Constructor that instantly starts a new transaction
   *
   * @param pTransactionName Name of the transaction to start
   */
  public SentryTracedTransaction(@NotNull String pTransactionName)
  {
    transaction = Sentry.startTransaction(pTransactionName, pTransactionName);
    transaction.scheduleFinish(); // autoclose if unused
  }

  /**
   * Starts a new span or reuses an old one
   *
   * @param pOperationName Name of the operation, that is about to be executed
   * @param pDescription   Longer description for the span
   * @return the ID of the started / reused span
   */
  @NotNull
  public SpanID startSpan(@NotNull String pOperationName, @Nullable String pDescription)
  {
    int spanID = Objects.hash(pOperationName, pDescription);
    ISpan span = null;

    synchronized (lastActiveSpanRef)
    {
      ISpan lastActiveSpan = lastActiveSpanRef.get();
      if (lastActiveSpan != null && !lastActiveSpan.isFinished())
      {
        // First check if we are the last active one and if it can be reused
        if (Objects.equals(lastActiveSpan.getData(DATA_SPANID), spanID))
          span = reuseSpan(lastActiveSpan, pDescription);

          // It can not be reused - maybe it was scheduled for finish? Then close it. Otherwise do nothing with it.
        else if (isScheduledForFinishing(lastActiveSpan))
          executeFinish(lastActiveSpan, null, true);
      }
    }

    if (span == null)
    {
      //noinspection UnstableApiUsage
      ISpan latestActiveSpan = transaction.getLatestActiveSpan();
      if (latestActiveSpan == null)
        latestActiveSpan = transaction;

      span = latestActiveSpan.startChild(pOperationName, pDescription);
      span.setData(DATA_SPANID, spanID);
      lastActiveSpanRef.set(span);
    }

    return new SpanID(span);
  }

  /**
   * Schedules, that a span should be finished.
   * This finish will be delayed, so that we can reuse it if necessary
   *
   * @param pID        ID of the span to finish
   * @param pException Exception of the span
   */
  public void scheduleSpanFinish(@NotNull SpanID pID, @Nullable Throwable pException)
  {
    synchronized (lastActiveSpanRef)
    {
      ISpan spanToFinish = pID.span;
      ISpan lastActiveSpan = lastActiveSpanRef.get();
      if (Objects.equals(lastActiveSpan, spanToFinish))
      {
        spanToFinish.setData(DATA_POSSIBLE_RESULT, Optional.ofNullable(pException));

        //noinspection UnstableApiUsage
        spanToFinish.setData(DATA_POSSIBLE_RESULT_TIME, Sentry.getCurrentHub().getOptions().getDateProvider().now());
      }
      else
        executeFinish(spanToFinish, pException, false);
    }
  }

  /**
   * Determines, if the whole transaction is finished
   *
   * @return true, if the transaction is finished
   */
  public boolean isFinished()
  {
    return transaction.isFinished();
  }

  /**
   * Finishes the whole transaction, including all sub-spans inside
   *
   * @param pException Exception that indicates the "result" of the transaction
   */
  public void finish(@Nullable Throwable pException)
  {
    executeFinish(transaction, pException, false);
  }

  /**
   * Determines, if the given span is scheduled to be finished.
   * This schedulation is possible due to keeping the last span open
   * and be able to {@link SentryTracedTransaction#reuseSpan(ISpan, String)} it
   *
   * @param pSpan span to check
   * @return true, if it was scheduled to finish
   */
  private boolean isScheduledForFinishing(@NotNull ISpan pSpan)
  {
    return pSpan.getData(DATA_POSSIBLE_RESULT) != null || pSpan.getData(DATA_POSSIBLE_RESULT_TIME) != null;
  }

  /**
   * Executes a real and undelayed finish of a span
   *
   * @param pSpan      Span to finish
   * @param pException Exception which indicates the "result" of the span
   */
  private void executeFinish(@NotNull ISpan pSpan, @Nullable Throwable pException, boolean pUsePersistedData)
  {
    // Set exception based on input parameter and persisted result
    if (pUsePersistedData)
    {
      Object possibleResult = pSpan.getData(DATA_POSSIBLE_RESULT);
      if (possibleResult instanceof Optional)
      {
        //noinspection unchecked
        pException = ((Optional<Throwable>) possibleResult).orElse(null);
        pSpan.setData(DATA_POSSIBLE_RESULT, ""); // remove data field
      }
    }

    // update throwable
    pSpan.setThrowable(pException);

    // calculate status based on exception
    SpanStatus status = pException == null ? SpanStatus.OK : SpanStatus.UNKNOWN_ERROR;

    // Finish span based on persisted time if possible
    if (pUsePersistedData)
    {
      Object possibleTime = pSpan.getData(DATA_POSSIBLE_RESULT_TIME);
      if (possibleTime instanceof SentryDate)
      {
        pSpan.finish(status, (SentryDate) possibleTime);
        pSpan.setData(DATA_POSSIBLE_RESULT_TIME, "");
      }
    }

    // clear invocation count
    pSpan.setData(DATA_INVOCATIONCOUNT, "");

    // finish span, if not already finished
    if (!pSpan.isFinished())
      pSpan.finish(status);
  }

  /**
   * Prepares the given span, so that it will be reused afterwards
   *
   * @param pSpan        Span to reuse
   * @param pDescription Original description to be used for the given span
   * @return the reusable span
   */
  @NotNull
  private ISpan reuseSpan(@NotNull ISpan pSpan, @Nullable String pDescription)
  {
    // clear all unused data now
    pSpan.setData(DATA_POSSIBLE_RESULT, "");
    pSpan.setData(DATA_POSSIBLE_RESULT_TIME, "");

    // update invocation count
    Object invCountObj = pSpan.getData(DATA_INVOCATIONCOUNT);
    long invCount = 2;
    if (invCountObj instanceof AtomicLong)
      invCount = ((AtomicLong) invCountObj).incrementAndGet();
    else
      pSpan.setData(DATA_INVOCATIONCOUNT, new AtomicLong(invCount));

    // update description based on invocation count
    pSpan.setDescription("(" + invCount + " inv.) - " + pDescription);

    return pSpan;
  }

  /**
   * Identifier for a opened span, so that {@link SentryTracedTransaction#startSpan(String, String)} can return an object
   * that {@link SentryTracedTransaction#scheduleSpanFinish(SpanID, Throwable)} understands.
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class SpanID
  {
    /**
     * Inner span to finish, if possible.
     * Do not expose this to the public world, because we
     * want to handle it inside the {@link SentryTracedTransaction}
     */
    @NotNull
    private final ISpan span;
  }

}
