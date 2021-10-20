package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.*;
import de.adito.aditoweb.nbm.metrics.api.types.MetricType;
import de.adito.aditoweb.nbm.metrics.impl.registry.IMetricRegistryProvider;
import org.jetbrains.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

/**
 * Implements all abstract basic stuff for codahale metrics
 *
 * @author w.glanzer, 13.07.2021
 */
abstract class AbstractAsyncCodahaleMetricHandler<T extends Annotation> extends AbstractAsyncMetricHandler<T>
{

  private static final LoadingCache<Class<? extends MetricType.IMetricNameFactory>, MetricType.IMetricNameFactory> _NAME_FACTORIES = CacheBuilder.newBuilder()
      .build(new CacheLoader<>()
      {
        @Override
        public MetricType.IMetricNameFactory load(@NotNull Class<? extends MetricType.IMetricNameFactory> pKey) throws Exception
        {
          Constructor<? extends MetricType.IMetricNameFactory> constr = pKey.getDeclaredConstructor();
          constr.setAccessible(true);
          return constr.newInstance();
        }
      });

  /**
   * Returns the name of the metric of an annotated method
   *
   * @param pAnnotationName Name of the annotation
   * @param pCalledMethod   Method that was annotated
   * @return Name of the metric
   */
  @NotNull
  protected String getMetricName(@Nullable String pAnnotationName, @Nullable Class<? extends MetricType.IMetricNameFactory> pNameFactory,
                                 @NotNull Method pCalledMethod, @Nullable Object[] pArguments)
  {
    String name = pAnnotationName;

    // Extend with name factory
    if (pNameFactory != null && pArguments != null)
      name = _NAME_FACTORIES.getUnchecked(pNameFactory).create(name, pArguments);

    // Empty string should not be used, but we should have implemented a fallback..
    if (name == null || name.trim().isEmpty())
      name = pCalledMethod.getDeclaringClass().getName() + "." + pCalledMethod.getName();

    return name;
  }

  @NotNull
  protected final MetricRegistry getRegistry()
  {
    return IMetricRegistryProvider.getDefault().getRegistry();
  }

}
