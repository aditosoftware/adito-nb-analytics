package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.metrics.impl.handlers.IMetricHandler;
import org.jetbrains.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * MethodHandler that calls all methods async, so that the caller won't notice any sideeffects..
 *
 * @author w.glanzer, 13.07.2021
 */
abstract class AbstractAsyncMetricHandler<T extends Annotation> implements IMetricHandler<T>
{

  private static final Logger _LOGGER = Logger.getLogger(AbstractAsyncCodahaleMetricHandler.class.getName());
  private static final ExecutorService _EXECUTOR = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                                                                     .setDaemon(true)
                                                                                     .setNameFormat("tMetricHandler-%d")
                                                                                     .build());

  @Override
  public final void beforeMethod(@NotNull T pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs)
  {
    _EXECUTOR.execute(() -> {
      try
      {
        beforeMethod0(pAnnotation, pObject, pMethod, pArgs);
      }
      catch (Exception e)
      {
        _LOGGER.log(Level.WARNING, "Failed to execute 'before' task of metric handler " + getClass().getSimpleName() +
            " for annotation " + pAnnotation + " on object " + pObject + " on method " + pMethod +
            " with arguments " + Arrays.toString(pArgs), e);
      }
    });
  }

  @Override
  public final void afterMethod(@NotNull T pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                                @Nullable Object pResult)
  {
    _EXECUTOR.execute(() -> {
      try
      {
        afterMethod0(pAnnotation, pObject, pMethod, pArgs, pResult);
      }
      catch (Exception e)
      {
        _LOGGER.log(Level.WARNING, "Failed to execute 'after' task of metric handler " + getClass().getSimpleName() +
            " for annotation " + pAnnotation + " on object " + pObject + " on method " + pMethod +
            " with arguments " + Arrays.toString(pArgs), e);
      }
    });
  }

  /**
   * Gets called before the original method
   *
   * @param pAnnotation Annotation that caused the metricHandler got triggered
   * @param pObject     Target object of the method call
   * @param pMethod     Method that got called
   * @param pArgs       Arguments of the method
   */
  protected abstract void beforeMethod0(@NotNull T pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs)
      throws Exception;

  /**
   * Gets called after the original method
   *
   * @param pAnnotation Annotation that caused the metricHandler got triggered
   * @param pObject     Target object of the method call
   * @param pMethod     Method that got called
   * @param pArgs       Arguments of the method
   * @param pResult     Return value of the method
   */
  protected abstract void afterMethod0(@NotNull T pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                                       @Nullable Object pResult)
      throws Exception;

}
