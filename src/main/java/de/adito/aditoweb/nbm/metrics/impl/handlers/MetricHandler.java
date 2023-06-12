package de.adito.aditoweb.nbm.metrics.impl.handlers;

import de.adito.picoservice.PicoService;
import lombok.NonNull;

import java.lang.annotation.*;

/**
 * Provides the possibility to annotated a metric handler so
 * that is possible that it triggers on specific method calls.
 *
 * @author w.glanzer, 12.07.2021
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PicoService
public @interface MetricHandler
{

  /**
   * @return Annotation that this metric handler can handle
   */
  @NonNull
  Class<? extends Annotation> metric();

}
