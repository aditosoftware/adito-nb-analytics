package de.adito.aditoweb.nbm.metrics.impl.bugreports;

import org.jetbrains.annotations.Nullable;

/**
 * Contains everything of {@link IBugReport}, but some of the fields are mutable
 *
 * @author w.glanzer, 22.06.2023
 * @see IBugReport
 */
public interface IMutableBugReport extends IBugReport
{

  /**
   * Sets the user defined comment, that should be included in this bug report
   *
   * @param pComment comment or null
   */
  void setComment(@Nullable String pComment);

  /**
   * Sets the user defined email adress
   *
   * @param pEMail adress or null
   */
  void setMail(@Nullable String pEMail);

}
