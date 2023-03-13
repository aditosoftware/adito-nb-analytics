package de.adito.aditoweb.nbm.metrics.api.types;

import java.lang.annotation.*;

/**
 * Methods annotated with this annotation get traced into a "performance" chart.
 * The calls of methods annotated with this annotation get recorded as single
 * spans which belong to the transaction given in the annotation.
 *
 * Should be moved into our regular metrics-api package,
 * but for backwards compatibility reasons we include it here temporarily(!)
 *
 * @author w.glanzer, 02.03.2023
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@MetricType
@Repeatable(Traced.Multiple.class)
public @interface Traced
{

  /**
   * @return Name of the transaction that this method call belongs to
   */
  String transaction();

  /**
   * Determines, if a new transaction should be started.
   * If false, then no new transaction will be started and it will
   * only be traced, if an transaction is already open.
   * True opens a new one, if necessary - if a transaction was
   * already been started, then it will be appended to the currently opened one.
   *
   * @return true, if a new transaction should be startet
   */
  boolean startTransaction() default false;

  /**
   * Determines, if asynchronous calls of the traced methods
   * should be included in the same transaction.
   * Mainly this parameter indicates, if a transaction
   * should be unique for a single thread or not
   *
   * @return true, if asynchronous calls should be appended
   * to a already running transaction, regardless of the thread
   */
  boolean combineAsyncTransactions() default true;

  /**
   * Container for multiple {@link Traced} annotations
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Multiple
  {
    /**
     * @return all tracing annotation instances
     */
    Traced[] value();
  }

}
