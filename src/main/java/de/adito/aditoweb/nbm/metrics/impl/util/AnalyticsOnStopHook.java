package de.adito.aditoweb.nbm.metrics.impl.util;

import io.reactivex.rxjava3.disposables.*;
import lombok.NonNull;
import org.openide.modules.OnStop;

/**
 * "Root"-Hook that gets notified if the module gets unloaded
 */
@OnStop
public class AnalyticsOnStopHook implements Runnable
{
  /**
   * Contains everything to
   */
  private static final CompositeDisposable DISPOSABLE = new CompositeDisposable();

  /**
   * Adds the given disposable to the "queue" to dispose on module unload
   *
   * @param pDisposable Dispose to dispose on unload
   */
  public static void addDisposable(@NonNull Disposable pDisposable)
  {
    DISPOSABLE.add(pDisposable);
  }

  @Override
  public void run()
  {
    DISPOSABLE.clear();
  }

}