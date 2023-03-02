package de.adito.aditoweb.nbm.metrics.impl.handlers.external;

import de.adito.aditoweb.nbm.metrics.api.types.Sampled;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import de.adito.aditoweb.nbm.metrics.impl.handlers.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Logs all exceptions to the event logger
 *
 * @author w.glanzer, 10.10.2022
 */
@MetricHandler(metric = Sampled.class)
public class ExternalSampledMetricHandler implements IMetricHandler<Sampled>
{

  @Override
  public void beforeMethod(@NotNull Sampled pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                           @NotNull Map<String, Object> pHints)
  {
    // Check if this execution should be handled by an external event logger
    for (Object arg : pArgs)
      if (arg instanceof Throwable)
        IEventLogger.getInstance().captureRegularException((Throwable) arg);
  }

}
