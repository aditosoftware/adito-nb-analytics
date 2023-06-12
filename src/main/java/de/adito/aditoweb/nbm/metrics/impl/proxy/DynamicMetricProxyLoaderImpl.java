package de.adito.aditoweb.nbm.metrics.impl.proxy;

import com.google.common.collect.*;
import de.adito.aditoweb.nbm.metrics.impl.handlers.IMetricHandler;
import de.adito.aditoweb.nbm.metrics.impl.proxy.dynamic.IDynamicMetricProxyLoader;
import lombok.*;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import org.jetbrains.annotations.*;
import org.openide.modules.Modules;
import org.openide.util.Pair;
import org.openide.util.lookup.ServiceProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Metric-Loader that injects metric annotations and its calculation during runtime
 *
 * @author w.glanzer, 06.03.2023
 */
@ServiceProvider(service = IDynamicMetricProxyLoader.class)
public class DynamicMetricProxyLoaderImpl implements IDynamicMetricProxyLoader
{

  private static final Logger LOGGER = Logger.getLogger(DynamicMetricProxyLoaderImpl.class.getName());
  private static final AtomicBoolean BYTEBUDDYAGENT_INSTALLED = new AtomicBoolean(false);
  private static final AtomicBoolean BYTEBUDDYAGENT_RETRANSFORMABLE = new AtomicBoolean(false);
  private static final Multimap<Class<?>, Pair<ElementMatcher<MethodDescription>, Annotation>> PROXIED_ANNOTATIONS = HashMultimap.create();

  @Override
  public void loadDynamicProxy(@NonNull Class<?> pClass, @NonNull Annotation pAnnotation, @Nullable ElementMatcher<MethodDescription> pMatcher)
  {
    try
    {
      // Install Agent if necessary
      if (!BYTEBUDDYAGENT_INSTALLED.getAndSet(true))
      {
        boolean retransformable = ByteBuddyAgent.install().isRetransformClassesSupported();
        BYTEBUDDYAGENT_RETRANSFORMABLE.set(retransformable);

        // Log once, that retransformation is not possible
        if (!retransformable)
          LOGGER.log(Level.WARNING, "Redefine of classes with the current JDK is not possible");
      }

      // Redefine class if possible and necessary
      if (BYTEBUDDYAGENT_RETRANSFORMABLE.get() && PROXIED_ANNOTATIONS.put(pClass, Pair.of(pMatcher, pAnnotation)))
      {
        // create matcher
        ElementMatcher.Junction<MethodDescription> matcher = isMethod()
            .and(not(isAbstract()));

        // append own matcher
        if (pMatcher != null)
          matcher = matcher.and(pMatcher);

        // install to agent
        new AgentBuilder.Default()
            .disableClassFormatChanges()

            // Retransform classes
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)

            // Select the given class and all of its inner and subclasses
            .type(named(pClass.getName())
                      .or(nameStartsWith(pClass.getName() + "$")))

            // Add advices
            .transform(new AgentBuilder.Transformer.ForAdvice()
                           .include(Thread.currentThread().getContextClassLoader(), pClass.getClassLoader())
                           .advice(matcher, DynamicMetricAdvice.class.getName()))

            // Install to agent
            .installOnByteBuddyAgent();
      }
    }
    catch (Throwable e) //NOSONAR catch everything, including NoSuchMethodError
    {
      LOGGER.log(Level.WARNING, "Failed to create a dynamic metric proxy for class " + pClass.getName(), e);
    }
  }

  /**
   * This advice is used by bytebuddy, to provide information about method calling.
   * See {@link Advice} for more information about using advices.
   * Be careful if you refactor this code - it will be "copy-pasted" into already living
   * objects (via byte code manipulation) and has to be completely standalone!
   */
  @SuppressWarnings("unused")
  private static class DynamicMetricAdvice
  {
    /**
     * Gets called by bytebuddy, if an instrumented method was entered.
     * This method delegates its invocation to our {@link DynamicMetricInvocation} class, so it can be handled during runtime.
     *
     * @param pTarget    Target object that was instrumented (mainly known as "this")
     * @param pMethod    Method that was instrumented
     * @param pArguments Arguments of the method
     */
    @SneakyThrows // will be suppressed by byte-buddy
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Map<String, Object> onMethodEnter(@Advice.This(typing = Assigner.Typing.DYNAMIC, optional = true) Object pTarget,
                                                    @Advice.Origin Method pMethod, @Advice.AllArguments Object[] pArguments)
    {
      Map<String, Object> hints = new HashMap<>();

      // invoke method
      Modules.getDefault().findCodeNameBase("de.adito.aditoweb.nbm.analytics").getClassLoader()
          .loadClass("de.adito.aditoweb.nbm.metrics.impl.proxy.DynamicMetricProxyLoaderImpl$DynamicMetricInvocation")
          .getDeclaredMethod("methodEntered", Object.class, Method.class, Object[].class, Map.class)
          .invoke(null, pTarget, pMethod, pArguments, hints);

      return hints;
    }

    /**
     * Gets called by bytebuddy, if an instrumented method was exited
     * This method delegates its invocation to our {@link DynamicMetricInvocation} class, so it can be handled during runtime.
     *
     * @param pTarget      Target object that was instrumented (mainly known as "this")
     * @param pMethod      Method that was instrumented
     * @param pArguments   Arguments of the method
     * @param pHints       Hints to be used in metric handlers
     * @param pReturnValue Object that the instrumented method returned. NULL if an exception occured
     * @param pThrowable   Exception that the instrumented method throwed. NULL if no exception was thrown during execution.
     */
    @SneakyThrows // will be suppressed by byte-buddy
    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onMethodExit(@Advice.This(typing = Assigner.Typing.DYNAMIC, optional = true) Object pTarget,
                                    @Advice.Origin Method pMethod, @Advice.AllArguments Object[] pArguments,
                                    @Advice.Enter Map<String, Object> pHints,
                                    @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object pReturnValue,
                                    @Advice.Thrown(typing = Assigner.Typing.DYNAMIC) Throwable pThrowable)
    {
      // invoke method
      Modules.getDefault().findCodeNameBase("de.adito.aditoweb.nbm.analytics").getClassLoader()
          .loadClass("de.adito.aditoweb.nbm.metrics.impl.proxy.DynamicMetricProxyLoaderImpl$DynamicMetricInvocation")
          .getDeclaredMethod("methodExited", Object.class, Method.class, Object[].class, Map.class, Object.class, Throwable.class)
          .invoke(null, pTarget, pMethod, pArguments, pHints, pReturnValue, pThrowable);
    }
  }

  /**
   * The static methods of this class will be called from
   * "hijacked" methods by {@link DynamicMetricAdvice}
   */
  @SuppressWarnings("unused") // Reflection by DynamicMetricAdvice
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  protected static class DynamicMetricInvocation
  {
    protected static final IMetricHandler.Accessor ACCESSOR = new IMetricHandler.Accessor();

    /**
     * Gets called by {@link DynamicMetricAdvice}, if an instrumented method was entered.
     *
     * @param pTarget    Target object that was instrumented (mainly known as "this")
     * @param pMethod    Method that was instrumented
     * @param pArguments Arguments of the method
     * @param pHints     Hints field to be used in metric handlers
     */
    public static void methodEntered(@Nullable Object pTarget, @NonNull Method pMethod, @NonNull Object[] pArguments, @NonNull Map<String, Object> pHints)
    {
      ACCESSOR.beforeMethodCall(pTarget, pMethod, new DynamicAnnotatedElement(pMethod, findAnnotationsForMethod(pMethod)), pArguments, pHints);
    }

    /**
     * Gets called by {@link DynamicMetricAdvice}, if an instrumented method was exited.
     *
     * @param pTarget      Target object that was instrumented (mainly known as "this")
     * @param pMethod      Method that was instrumented
     * @param pArguments   Arguments of the method
     * @param pHints       Hints field to be used in metric handlers
     * @param pReturnValue Object that the instrumented method returned. NULL if an exception occured
     * @param pThrowable   Exception that the instrumented method throwed. NULL if no exception was thrown during execution.
     */
    public static void methodExited(@Nullable Object pTarget, @NonNull Method pMethod, @NonNull Object[] pArguments, @NonNull Map<String, Object> pHints,
                                    @Nullable Object pReturnValue, @Nullable Throwable pThrowable)
    {
      ACCESSOR.afterMethodCall(pTarget, pMethod, new DynamicAnnotatedElement(pMethod, findAnnotationsForMethod(pMethod)), pArguments, pHints, pReturnValue, pThrowable);
    }

    /**
     * Returns all annotations that the given one should process additionally
     *
     * @param pMethod Method to get the annotations for
     * @return the annotations
     */
    @NonNull
    private static List<Annotation> findAnnotationsForMethod(@NonNull Method pMethod)
    {
      Collection<Pair<ElementMatcher<MethodDescription>, Annotation>> cached = PROXIED_ANNOTATIONS.get(pMethod.getDeclaringClass());
      if (cached == null)
        return List.of();

      return cached.stream()
          .filter(pPair -> pPair.first() == null || pPair.first().matches(new MethodDescription.ForLoadedMethod(pMethod)))
          .map(Pair::second)
          .collect(Collectors.toList());
    }

    /**
     * Represents an {@link AnnotatedElement} with dynamic additional elements, given in constructor
     */
    @RequiredArgsConstructor
    private static class DynamicAnnotatedElement implements AnnotatedElement
    {
      @NonNull
      private final AnnotatedElement base;

      @NonNull
      private final Collection<Annotation> additionalAnnotations;

      @Override
      public <T extends Annotation> T getAnnotation(@NonNull Class<T> annotationClass)
      {
        for (Annotation additionalAnnotation : additionalAnnotations)
          if (additionalAnnotation.annotationType().isAssignableFrom(annotationClass))
            //noinspection unchecked
            return (T) additionalAnnotation;

        return base.getAnnotation(annotationClass);
      }

      @Override
      public Annotation[] getAnnotations()
      {
        List<Annotation> annos = new ArrayList<>(Arrays.asList(base.getAnnotations()));
        annos.addAll(additionalAnnotations);
        return annos.toArray(new Annotation[0]);
      }

      @Override
      public Annotation[] getDeclaredAnnotations()
      {
        List<Annotation> annos = new ArrayList<>(Arrays.asList(base.getDeclaredAnnotations()));
        annos.addAll(additionalAnnotations);
        return annos.toArray(new Annotation[0]);
      }
    }
  }
}
