package de.adito.aditoweb.nbm.metrics.impl.bugreports;

import lombok.NonNull;

import java.io.File;

/**
 * Factory to create {@link IBugReport} instances
 *
 * @author w.glanzer, 13.06.2023
 */
public interface IBugReportFactory
{

  /**
   * @return the default factory to use
   */
  @NonNull
  static IBugReportFactory getInstance()
  {
    return DefaultBugReportFactory.INSTANCE;
  }

  /**
   * Creates a new bug report that can be transmitted via {@link de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger#captureBugReport(IBugReport, File)}.
   * Will collect all necessary data during this method, so the report will be (mostly) complete
   *
   * @return the report with all necessary data
   */
  @NonNull
  IMutableBugReport create();

}
