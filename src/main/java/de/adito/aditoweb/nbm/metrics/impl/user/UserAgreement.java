package de.adito.aditoweb.nbm.metrics.impl.user;

import de.adito.util.reactive.cache.ObservableCache;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import org.jetbrains.annotations.NotNull;
import org.openide.*;
import org.openide.util.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * @author w.glanzer, 06.09.2021
 */
class UserAgreement implements IUserAgreement
{
  private static final String _USER_AGREEMENT = "adito.designer.analytics.agreement.ok";
  protected static final UserAgreement INSTANCE = new UserAgreement();
  private final AtomicBoolean initLock = new AtomicBoolean(false);
  private final Preferences prefs = NbPreferences.forModule(UserAgreement.class); // use module preferences to ensure user agreement after EVERY installation
  private final ObservableCache observableCache = new ObservableCache();
  private final BehaviorSubject<Boolean> hasAgreedSendingAnalytics = BehaviorSubject.createDefault(false);

  @NotNull
  @Override
  public Observable<Boolean> sendingAnalyticsAllowed()
  {
    return observableCache.calculate("sendingAnalyticsAllowed", () -> hasAgreedSendingAnalytics);
  }

  /**
   * Ensures that this agreement is initied.
   * As sideeffect this method may cause a dialog to user, to agree the terms of service
   *
   * @return the agreement
   */
  @NotNull
  protected UserAgreement ensureInited()
  {
    synchronized (initLock) // lock, because we do not want to show the dialog twice
    {
      if (!initLock.get())
      {
        _init();
        initLock.set(true);
      }

      return this;
    }
  }

  /**
   * Sets, if the user has agreed to send analytics
   *
   * @param pAgreed true, if agreed
   */
  protected void setHasAgreedSendingAnalytics(boolean pAgreed)
  {
    hasAgreedSendingAnalytics.onNext(pAgreed);
  }

  /**
   * Initializes the userAgreement object.
   * It may block until the user decided to aggee / disagree.
   */
  private void _init()
  {
    if (prefs.get(_USER_AGREEMENT, "").trim().isEmpty())
      prefs.putBoolean(_USER_AGREEMENT, _askAgreeSendingAnalytics_Blocking());
    setHasAgreedSendingAnalytics(prefs.getBoolean(_USER_AGREEMENT, false));
  }

  /**
   * Shows the dialog for the user
   *
   * @return TRUE if the analytics should be sent - FALSE otherwise
   */
  private boolean _askAgreeSendingAnalytics_Blocking()
  {
    String aggreeOption = NbBundle.getMessage(UserAgreement.class, "Title_AgreeOption");
    DialogDescriptor descriptor = new DialogDescriptor(NbBundle.getMessage(UserAgreement.class, "TXT_AnalyticsAgreement"),
                                                       NbBundle.getMessage(UserAgreement.class, "Title_AnalyticsAgreement"),
                                                       true, new Object[]{aggreeOption, NbBundle.getMessage(UserAgreement.class, "Title_DeclineOption")},
                                                       aggreeOption, DialogDescriptor.DEFAULT_ALIGN, null, null);
    descriptor.setNoDefaultClose(true);
    Object result = DialogDisplayer.getDefault().notify(descriptor);
    return result == aggreeOption;
  }

}
