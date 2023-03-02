package de.adito.aditoweb.nbm.metrics.impl.handlers;

import org.jetbrains.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Handles a method that is annotated with a metric annotation
 *
 * @author w.glanzer, 12.07.2021
 */
public interface IMetricHandler<T extends Annotation>
{

  /**
   * Gets called before the original method
   *
   * @param pAnnotation Annotation that caused the metricHandler got triggered
   * @param pObject     Target object of the method call
   * @param pMethod     Method that got called
   * @param pArgs       Arguments of the method
   * @param pHints      Hints to be passed to the {@link IMetricHandler#afterMethod(Annotation, Object, Method, Object[], Object, Throwable, Map)} method
   */
  default void beforeMethod(@NotNull T pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs, @NotNull Map<String, Object> pHints)
      throws Exception
  {
  }

  /**
   * Gets called after the original method
   *
   * @param pAnnotation Annotation that caused the metricHandler got triggered
   * @param pObject     Target object of the method call
   * @param pMethod     Method that got called
   * @param pArgs       Arguments of the method
   * @param pResult     Return value of the method
   * @param pException  Exception if the method throwed one
   * @param pHints      Hints to get passed from the {@link IMetricHandler#beforeMethod(Annotation, Object, Method, Object[], Map)} method
   */
  default void afterMethod(@NotNull T pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                           @Nullable Object pResult, @Nullable Throwable pException, @NotNull Map<String, Object> pHints) throws Exception
  {
  }

}
