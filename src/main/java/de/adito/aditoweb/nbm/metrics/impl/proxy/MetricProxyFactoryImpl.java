package de.adito.aditoweb.nbm.metrics.impl.proxy;

import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.impl.handlers.*;
import de.adito.picoservice.IPicoRegistry;
import org.jetbrains.annotations.NotNull;
import org.openide.util.Pair;
import org.openide.util.lookup.ServiceProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * @author w.glanzer, 09.07.2021
 */
@ServiceProvider(service = IMetricProxyFactory.class)
public class MetricProxyFactoryImpl implements IMetricProxyFactory
{

  public static final boolean ENABLED = false; // currently disabled, because the data wont get reviewed afterwards
  private static final Logger _LOGGER = Logger.getLogger(MetricProxyFactoryImpl.class.getName());
  private final IInterceptableObjectFactory objectFactory = new InterceptableObjectFactory();

  @Override
  public boolean canCreateProxy(@NotNull Object pObject)
  {
    // proxy everything, if enabled
    return ENABLED;
  }

  @NotNull
  @Override
  public <T> T createProxy(@NotNull T pObject)
  {
    try
    {
      if (ENABLED)
        return objectFactory.createInterceptableProxy(pObject, new _MetricInvocationHandler<>(pObject));
      return pObject;
    }
    catch (Throwable e) //NOSONAR catch everything, including NoSuchMethodError
    {
      _LOGGER.log(Level.WARNING, "Failed to create a metric proxy for object " + pObject, e);

      // return the original value, if an error happens..
      return pObject;
    }
  }

  /**
   * Invocation-Handler that delegates all method calls to the appropriate handlers
   * and invokes the original method afterwards
   */
  private static class _MetricInvocationHandler<T> implements InvocationHandler
  {
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

    private final T object;

    public _MetricInvocationHandler(@NotNull T pObject)
    {
      object = pObject;
    }

    @Override
    public Object invoke(Object pProxy, Method pMethod, Object[] pArgs) throws Throwable
    {
      AtomicReference<Object> methodResult = new AtomicReference<>();

      try
      {
        // trigger handlers
        _onEachHandlerNoThrow(pMethod, pInvocation -> {
          try
          {
            if (pInvocation != null)
              //noinspection unchecked,rawtypes
              ((IMetricHandler) pInvocation.second()).beforeMethod(pInvocation.first(), pProxy, pMethod, pArgs);
          }
          catch (Throwable t) // NOSONAR we do want to catch all exceptions, so the user does not notice this ones
          {
            _LOGGER.log(Level.WARNING, "", t);
          }
        });

        // call method itself
        pMethod.setAccessible(true);
        methodResult.set(pMethod.invoke(object, pArgs));
        return methodResult.get();
      }
      finally
      {
        // trigger handlers
        _onEachHandlerNoThrow(pMethod, pInvocation -> {
          try
          {
            if (pInvocation != null)
              //noinspection unchecked,rawtypes
              ((IMetricHandler) pInvocation.second()).afterMethod(pInvocation.first(), pProxy, pMethod, pArgs, methodResult.get());
          }
          catch (Throwable t) // NOSONAR we do want to catch all exceptions, so the user does not notice this ones
          {
            _LOGGER.log(Level.WARNING, "", t);
          }
        });
      }
    }

    /**
     * Searches all appropriate method handlers of the given method and calls pExecFn afterwards.
     * This method must not throw an exception, because the method caller will get intercepted and so the original
     * method won't get called corretly.
     *
     * @param pMethod Method that was called
     * @param pExecFn Function to call on every appropriate metric handler
     */
    private void _onEachHandlerNoThrow(@NotNull Method pMethod, @NotNull Consumer<Pair<Annotation, IMetricHandler<?>>> pExecFn)
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
  }
}
