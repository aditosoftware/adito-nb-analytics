package de.adito.aditoweb.nbm.metrics.impl.proxy;

import de.adito.aditoweb.nbm.metrics.api.types.MetricType;
import de.adito.aditoweb.nbm.metrics.impl.handlers.*;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.openide.modules.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Test for {@link DynamicMetricProxyLoaderImpl}
 *
 * @author w.glanzer, 07.03.2023
 */
class DynamicMetricProxyLoaderImplTest
{

  private final DynamicMetricProxyLoaderImpl proxyLoader = new DynamicMetricProxyLoaderImpl();
  private MockedStatic<Modules> modulesMock;

  /**
   * Mock the classloader, so that we do not need to load the whole netbeans module system for our test
   */
  @BeforeEach
  void setUp()
  {
    Modules modules = mock(Modules.class);
    ModuleInfo moduleInfo = mock(ModuleInfo.class);
    when(moduleInfo.getClassLoader()).thenReturn(ClassLoader.getSystemClassLoader());
    when(modules.findCodeNameBase("de.adito.aditoweb.nbm.analytics")).thenReturn(moduleInfo);
    modulesMock = mockStatic(Modules.class);
    modulesMock.when(Modules::getDefault).thenReturn(modules);
  }

  /**
   * Checks, if the proxy got injected correctly ...
   */
  @Nested
  @SuppressWarnings("ResultOfMethodCallIgnored")
  class ShouldInjectProxy
  {
    private MyTestMethodHandler methodHandler;

    /**
     * Inject the proxy into our test objects
     */
    @BeforeEach
    void setUp()
    {
      proxyLoader.loadDynamicProxy(MyTestClass.class, new MyTestMethod.Instance(), null);

      // Handler to spy on
      methodHandler = mock(MyTestMethodHandler.class);
      MyTestMethodHandler.delegate = methodHandler;
    }

    /**
     * ... on a regular method, without anything special
     */
    @Test
    @SneakyThrows
    void onRegularVoidMethod()
    {
      // create object and do a dummy call
      MyTestClass object = new MyTestClass();
      object.doDummyCall();

      // verify handler call
      InOrder order = inOrder(methodHandler);
      order.verify(methodHandler, times(1)).beforeMethod(notNull(), eq(object), notNull(), eq(new Object[]{}), eq(Map.of()));
      order.verify(methodHandler, times(1)).afterMethod(notNull(), eq(object), notNull(), eq(new Object[]{}), isNull(), isNull(), eq(Map.of()));
    }

    /**
     * ... on a value returning method
     */
    @Test
    @SneakyThrows
    void onReturningMethod()
    {
      // create object and do a dummy call
      MyTestClass object = new MyTestClass();
      object.doDummyCall(true);

      // verify handler call
      InOrder order = inOrder(methodHandler);
      order.verify(methodHandler, times(1)).beforeMethod(notNull(), eq(object), notNull(), eq(new Object[]{true}), eq(Map.of()));
      order.verify(methodHandler, times(1)).afterMethod(notNull(), eq(object), notNull(), eq(new Object[]{true}), eq(false), isNull(), eq(Map.of()));
    }

    /**
     * ... on a method that throws an exception
     */
    @Test
    @SneakyThrows
    void onThrowingMethod()
    {
      // create object and do a dummy call
      MyTestClass object = new MyTestClass();
      assertThrows(Exception.class, object::doDummyCallThrowing);

      // verify handler call
      InOrder order = inOrder(methodHandler);
      order.verify(methodHandler, times(1)).beforeMethod(notNull(), eq(object), notNull(), eq(new Object[]{}), eq(Map.of()));
      order.verify(methodHandler, times(1)).afterMethod(notNull(), eq(object), notNull(), eq(new Object[]{}), isNull(), isNotNull(), eq(Map.of()));
    }

    /**
     * ... on a regular static method
     */
    @Test
    @SneakyThrows
    void onStaticMethod()
    {
      // do a static dummy call
      MyTestClass.doStaticDummyCall();

      // verify handler call
      InOrder order = inOrder(methodHandler);
      order.verify(methodHandler, times(1)).beforeMethod(notNull(), isNull(), notNull(), eq(new Object[]{}), eq(Map.of()));
      order.verify(methodHandler, times(1)).afterMethod(notNull(), isNull(), notNull(), eq(new Object[]{}), isNull(), isNull(), eq(Map.of()));
    }
  }

  /**
   * Closes all static mocks afterwards
   */
  @AfterEach
  void tearDown()
  {
    modulesMock.closeOnDemand();
  }

  /**
   * Demo object to verify {@link MyTestMethod} annotation and its handler
   */
  private static class MyTestClass
  {
    /**
     * Simple method call, without anything special
     */
    public void doDummyCall()
    {
    }

    /**
     * Simple method call, with an input and output value
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean doDummyCall(boolean pInput)
    {
      return !pInput;
    }

    /**
     * Simple method call, which always throws an exception
     */
    public void doDummyCallThrowing() throws Exception
    {
      throw new Exception();
    }

    /**
     * Static method, without anything special
     */
    public static void doStaticDummyCall()
    {
    }
  }

  /**
   * Handler to react on {@link MyTestMethod} methods
   */
  @MetricHandler(metric = MyTestMethod.class)
  public static class MyTestMethodHandler implements IMetricHandler<MyTestMethod>
  {
    private static IMetricHandler<MyTestMethod> delegate;

    @Override
    public void beforeMethod(@NonNull MyTestMethod pAnnotation, @Nullable Object pObject, @NonNull Method pMethod,
                             Object @NonNull [] pArgs, @NonNull Map<String, Object> pHints)
        throws Exception
    {
      delegate.beforeMethod(pAnnotation, pObject, pMethod, pArgs, pHints);
    }

    @Override
    public void afterMethod(@NonNull MyTestMethod pAnnotation, @Nullable Object pObject, @NonNull Method pMethod,
                            Object @NonNull [] pArgs, @Nullable Object pResult, @Nullable Throwable pException, @NonNull Map<String, Object> pHints)
        throws Exception
    {
      delegate.afterMethod(pAnnotation, pObject, pMethod, pArgs, pResult, pException, pHints);
    }
  }

  /**
   * Demo-Annotation to add to the {@link MyTestClass} class, interpreted by {@link MyTestMethodHandler}
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @MetricType
  public static @interface MyTestMethod
  {
    /**
     * Instance object for this annotation
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    @EqualsAndHashCode
    static class Instance implements MyTestMethod
    {
      @EqualsAndHashCode.Include
      @Override
      public Class<? extends Annotation> annotationType()
      {
        return MyTestMethod.class;
      }
    }
  }

}
