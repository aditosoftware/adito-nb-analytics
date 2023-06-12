package de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry.handlers;

import de.adito.aditoweb.nbm.metrics.api.types.Sampled;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import de.adito.aditoweb.nbm.metrics.impl.handlers.*;
import lombok.NonNull;
import org.jetbrains.annotations.*;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Logs all exceptions to the event logger
 *
 * @author w.glanzer, 02.03.2023
 */
@MetricHandler(metric = Sampled.class)
class SentrySampledMetricHandler implements IMetricHandler<Sampled>
{

  @Override
  public void beforeMethod(@NonNull Sampled pAnnotation, @Nullable Object pObject, @NonNull Method pMethod, @NonNull Object[] pArgs,
                           @NonNull Map<String, Object> pHints)
  {
    // Check if this execution should be handled by an external event logger
    for (Object arg : pArgs)
      if (arg instanceof Throwable)
        IEventLogger.getInstance().captureRegularException((Throwable) arg);
  }

}