package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import com.google.common.cache.*;
import de.adito.aditoweb.nbm.metrics.api.types.Sampled;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import de.adito.aditoweb.nbm.metrics.impl.handlers.*;
import org.jetbrains.annotations.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Implements a metricHandler for {@link Sampled} annotation
 *
 * @author w.glanzer, 14.07.2021
 */
@MetricHandler(metric = Sampled.class)
class SampledMetricHandler extends AbstractAsyncCodahaleMetricHandler<Sampled>
{

  private static final LoadingCache<Class<? extends Sampled.IArgumentConverter>, Sampled.IArgumentConverter> _CONVERTERS = CacheBuilder.newBuilder()
      .build(new CacheLoader<>()
      {
        @Override
        public Sampled.IArgumentConverter load(@NotNull Class<? extends Sampled.IArgumentConverter> pKey) throws Exception
        {
          Constructor<? extends Sampled.IArgumentConverter> constr = pKey.getDeclaredConstructor();
          constr.setAccessible(true);
          return constr.newInstance();
        }
      });

  @Override
  protected void beforeMethod0(@NotNull Sampled pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs)
  {
    Map<String, String> labels = new HashMap<>();
    for (Object pArg : pArgs)
    {
      String argName = "arg" + labels.size(); //just a numbered arg-counter as key
      String argValue = _CONVERTERS.getUnchecked(pAnnotation.argumentConverter()).toString(pArg);
      if (argValue != null)
        labels.put(argName, argValue);
    }

    // Log Exceptions to external, if possible
    boolean wasHandled = false;
    for (Object arg : pArgs)
    {
      if (arg instanceof Throwable)
      {
        IEventLogger.getInstance().captureRegularException((Throwable) arg);
        wasHandled = true;
      }
    }

    // if not handled by eventlogger, export it to metric registry
    if (!wasHandled)
      ((MultiValueGauge) getRegistry().gauge(getMetricName(pAnnotation.name(), pAnnotation.nameFactory(), pMethod, pArgs), () -> new MultiValueGauge(true)))
          .addValue(labels, System.currentTimeMillis());
  }

  @Override
  protected void afterMethod0(@NotNull Sampled pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                              @Nullable Object pResult)
  {
    // not used
  }

}
