package de.adito.aditoweb.nbm.metrics.impl.handlers;

import de.adito.picoservice.IPicoRegistry;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.openide.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

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
   * @param pObject     Target object of the method call. NULL, if a static method was called.
   * @param pMethod     Method that got called
   * @param pArgs       Arguments of the method
   * @param pHints      Hints to be passed to the {@link IMetricHandler#afterMethod(Annotation, Object, Method, Object[], Object, Throwable, Map)} method
   */
  default void beforeMethod(@NonNull T pAnnotation, @Nullable Object pObject, @NonNull Method pMethod, Object @NonNull [] pArgs, @NonNull Map<String, Object> pHints)
      throws Exception
  {
  }

  /**
   * Gets called after the original method
   *
   * @param pAnnotation Annotation that caused the metricHandler got triggered
   * @param pObject     Target object of the method call. NULL, if a static method was called.
   * @param pMethod     Method that got called
   * @param pArgs       Arguments of the method
   * @param pResult     Return value of the method
   * @param pException  Exception if the method throwed one
   * @param pHints      Hints to get passed from the {@link IMetricHandler#beforeMethod(Annotation, Object, Method, Object[], Map)} method
   */
  default void afterMethod(@NonNull T pAnnotation, @Nullable Object pObject, @NonNull Method pMethod, Object @NonNull [] pArgs,
                           @Nullable Object pResult, @Nullable Throwable pException, @NonNull Map<String, Object> pHints) throws Exception
  {
  }

  /**
   * This class gives access to all currently registered metric handlers
   * and provides utility methods to be more efficient
   */
  class Accessor
  {
    private static final Logger _LOGGER = Logger.getLogger(Accessor.class.getName());
    private static final Map<Class<? extends Annotation>, Set<IMetricHandler<?>>> _METRIC_HANDLERS =
        IPicoRegistry.INSTANCE.find(IMetricHandler.class, MetricHandler.class)
            .entrySet()
            .stream()
            .map(pEntry -> {
              try
              {
                //noinspection unchecked
                Constructor<? extends IMetricHandler<?>> constr = (Constructor<? extends IMetricHandler<?>>) pEntry.getKey().getDeclaredConstructor();
                constr.setAccessible(true);
                return Map.entry(pEntry.getValue().metric(), Set.<IMetricHandler<?>>of(constr.newInstance()));
              }
              catch (Exception e)
              {
                return null;
              }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    /**
     * This method should be called before a metrified method will be called
     *
     * @param pProxy            Object whose method gets called afterwards. NULL, if a static method was called.
     * @param pMethod           Method that will be called
     * @param pAnnotatedElement Element that has the metric annotations on it
     * @param pArgs             Arguments of the method call
     * @param pHints            Hints to pass to the handlers
     */
    public void beforeMethodCall(@Nullable Object pProxy, @NonNull Method pMethod, @NonNull AnnotatedElement pAnnotatedElement,
                                 Object @NonNull [] pArgs, @NonNull Map<String, Object> pHints)
    {
      //noinspection unchecked,rawtypes
      _onEachHandlerNoThrow(pAnnotatedElement, pInvocation -> ((IMetricHandler) pInvocation.second()).beforeMethod(pInvocation.first(), pProxy, pMethod, pArgs, pHints));
    }

    /**
     * This method should be called after a metrified method was called
     *
     * @param pProxy            Object whose method was called. NULL, if a static method was called.
     * @param pMethod           Method that was called
     * @param pAnnotatedElement Element that has the metric annotations on it
     * @param pArgs             Arguments of the method call
     * @param pHints            Hints to pass to the handlers
     * @param pMethodResult     Result of the method call
     * @param pException        Exception, if any occured during the method call
     */
    public void afterMethodCall(@Nullable Object pProxy, @NonNull Method pMethod, @NonNull AnnotatedElement pAnnotatedElement, Object @NonNull [] pArgs,
                                @NonNull Map<String, Object> pHints, @Nullable Object pMethodResult, @Nullable Throwable pException)
    {
      //noinspection unchecked,rawtypes
      _onEachHandlerNoThrow(pAnnotatedElement, pInvocation ->
          ((IMetricHandler) pInvocation.second()).afterMethod(pInvocation.first(), pProxy, pMethod, pArgs, pMethodResult, pException, pHints));
    }

    /**
     * Searches all appropriate method handlers of the given method and calls pExecFn afterwards.
     * This method must not throw an exception, because the method caller will get intercepted and so the original
     * method won't get called corretly.
     *
     * @param pMethod Method that was called
     * @param pExecFn Function to call on every appropriate metric handler
     */
    private static void _onEachHandlerNoThrow(@NonNull AnnotatedElement pMethod, @NonNull HandlerFn pExecFn)
    {
      try
      {
        for (Annotation annotation : pMethod.getAnnotations())
        {
          Set<IMetricHandler<?>> handlers = _METRIC_HANDLERS.get(annotation.annotationType());
          if (handlers != null)
            for (IMetricHandler<?> handler : handlers)
              pExecFn.accept(Pair.of(annotation, handler));
        }
      }
      catch (Throwable t) // NOSONAR we do want to catch all exceptions, so the user does not notice this ones
      {
        _LOGGER.log(Level.WARNING, "", t);
      }
    }

    /**
     * Function to invoke on each handler
     */
    public static interface HandlerFn
    {
      /**
       * Function to invoke
       *
       * @param pObject Object to pass to the handler
       * @throws Exception exception
       */
      void accept(@NonNull Pair<Annotation, IMetricHandler<?>> pObject) throws Exception;
    }

  }
}
