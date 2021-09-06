package de.adito.aditoweb.nbm.metrics.impl.user;

import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;

/**
 * Contains all necessary information, what a user wants to send
 *
 * @author w.glanzer, 06.09.2021
 */
public interface IUserAgreement
{

  /**
   * @return the singleton instance
   */
  @NotNull
  static IUserAgreement getInstance()
  {
    return UserAgreement.INSTANCE.ensureInited();
  }

  /**
   * Returns an observable that contains the information, if sending the
   * anonymous usage statistics and analytics to ADITO is allowed
   *
   * @return true, if allowed, false otherwise
   */
  @NotNull
  Observable<Boolean> sendingAnalyticsAllowed();

}
