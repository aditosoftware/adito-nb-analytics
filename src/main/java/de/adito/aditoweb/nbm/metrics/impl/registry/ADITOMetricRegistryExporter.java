package de.adito.aditoweb.nbm.metrics.impl.registry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.metrics.impl.InstallationID;
import de.adito.aditoweb.nbm.metrics.impl.user.IUserAgreement;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;
import io.prometheus.client.exporter.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.jetbrains.annotations.NotNull;
import org.openide.modules.OnStop;
import org.openide.windows.OnShowing;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Exports all collected metrics to the ADITO endpoint
 *
 * @author w.glanzer, 16.07.2021
 */
// Gets called on designer start automatically by MetricRegistryExporterSTarter
@SuppressWarnings("unused")
class ADITOMetricRegistryExporter
{
  private static final boolean _ENABLED = false; // currently disabled, because the data wont get reviewed afterwards
  private static final Logger _LOGGER = Logger.getLogger(ADITOMetricRegistryExporter.class.getName());
  private static final long _INTERVAL_MS = Long.parseLong(System.getProperty("adito.metrics.exporter.prometheus.push.interval", "30000"));
  private static final boolean _LOG_ENABLED = System.getProperty("adito.metrics.exporter.log") != null;
  private static final ScheduledExecutorService _SENDING_SERVICE = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                                                                                                                  .setNameFormat("tMetricExporter-%d")
                                                                                                                  .setDaemon(true)
                                                                                                                  .setPriority(Thread.MIN_PRIORITY)
                                                                                                                  .build());
  private static ADITOMetricRegistryExporter _INSTANCE;
  private final IMetricRegistryProvider registryProvider;
  private ScheduledFuture<?> task;
  private Disposable analyticsAllowedDisposable;

  /**
   * @return the singleton instance
   */
  @NotNull
  public static ADITOMetricRegistryExporter getInstance()
  {
    if (_INSTANCE == null)
      _INSTANCE = new ADITOMetricRegistryExporter(IMetricRegistryProvider.getDefault());
    return _INSTANCE;
  }

  private ADITOMetricRegistryExporter(@NotNull IMetricRegistryProvider pRegistryProvider)
  {
    registryProvider = pRegistryProvider;

    // Log everything because of INFO level
    if (_LOG_ENABLED)
      _LOGGER.setLevel(Level.ALL);
  }

  /**
   * Gets called if the module containing the exporter was started.
   */
  protected synchronized void onModuleStart()
  {
    // init observable to observe analytics state
    // will start automatically, if we should - because the observable triggers it
    if (_ENABLED && analyticsAllowedDisposable == null)
      analyticsAllowedDisposable = IUserAgreement.getInstance().sendingAnalyticsAllowed()
          .subscribeOn(Schedulers.computation())
          .observeOn(Schedulers.computation())
          .distinctUntilChanged()
          .subscribe(pAllowed -> {
            if (!pAllowed)
              _stopTransmitting();
            else
              _startTransmitting();
          });
  }

  /**
   * Gets called if the module containing the exporter was stopped (shutdown).
   * Be careful, a module can be restarted again!
   */
  protected synchronized void onModuleStop()
  {
    // stop if started
    _stopTransmitting();

    // dispose analytics observable, because the module got uninstalled
    if (analyticsAllowedDisposable != null && !analyticsAllowedDisposable.isDisposed())
    {
      analyticsAllowedDisposable.dispose();
      analyticsAllowedDisposable = null;
    }
  }

  /**
   * Starts the transmitting service, if not already started
   */
  private synchronized void _startTransmitting()
  {
    try
    {
      if (task == null)
      {
        // Create gateway
        PushGateway gateway = new PushGateway(new URL(_ADITOEndpoint._URL));
        gateway.setConnectionFactory(new _ADITOEndpoint());
        task = _SENDING_SERVICE.scheduleWithFixedDelay(new _GatewaySender(gateway, registryProvider), 0, _INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Log sending
        if (_LOG_ENABLED)
          _LOGGER.info("Usage statistics and analytics service started. Transmitting data to " + _ADITOEndpoint._URL);
      }
    }
    catch (Exception e)
    {
      if (_LOG_ENABLED)
        _LOGGER.log(Level.WARNING, "Failed to initiate metric gateway", e);
    }
  }

  /**
   * Stops the transmitting service, if started
   */
  private synchronized void _stopTransmitting()
  {
    if (task != null)
    {
      task.cancel(true);
      task = null;
    }
  }

  /**
   * Gets called on designer start automatically
   */
  @OnShowing
  public static class MetricRegistryExporterStarter implements Runnable
  {
    @Override
    public void run()
    {
      ADITOMetricRegistryExporter.getInstance().onModuleStart();
    }
  }

  /**
   * Gets called on designer shutdown automatically
   */
  @OnStop
  public static class MetricRegistryExporterStopper implements Runnable
  {
    @Override
    public void run()
    {
      ADITOMetricRegistryExporter.getInstance().onModuleStop();
    }
  }

  /**
   * Runnable that gets executed asynchronously and triggers the "send"
   */
  private static class _GatewaySender implements Runnable
  {
    private final PushGateway gateway;
    private final IMetricRegistryProvider registryProvider;

    public _GatewaySender(@NotNull PushGateway pGateway, @NotNull IMetricRegistryProvider pRegistryProvider)
    {
      gateway = pGateway;
      registryProvider = pRegistryProvider;
    }

    @Override
    public void run()
    {
      try
      {
        gateway.pushAdd(new ADITODropwizardExports(registryProvider.getRegistry(), new _ADITOSampleBuilder()), _ADITOEndpoint._JOB_NAME);
      }
      catch (Exception e)
      {
        if (_LOG_ENABLED)
          _LOGGER.log(Level.WARNING, "Failed to transport metrics to gateway", e);
      }
    }
  }

  /**
   * Contains all necessary information about the push endpoint at ADITO
   */
  private static class _ADITOEndpoint extends Authenticator implements HttpConnectionFactory
  {
    private static final String _URL = "https://prometheus-gateway.c2.adito.cloud";
    private static final String _JOB_NAME = "designer_prod";

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
      return new PasswordAuthentication("A7PaC9zH6ZMqR32LAGSc", "6adzg5HeYK4h0xFfcJca".toCharArray());
    }

    @Override
    public HttpURLConnection create(String pURL) throws IOException
    {
      HttpURLConnection url = (HttpURLConnection) new URL(pURL).openConnection();
      url.setAuthenticator(this);
      return url;
    }
  }

  /**
   * SampleBuilder that will add the adito prefix to the
   * dropwizard metric name and includes the installation ID as label
   */
  private static class _ADITOSampleBuilder extends DefaultSampleBuilder
  {
    private static final String _PREFIX = "designer.";
    private static final String _VERSIONID_LABEL_NAME = "installID";

    @Override
    public Collector.MetricFamilySamples.Sample createSample(String dropwizardName, String nameSuffix, List<String> additionalLabelNames,
                                                             List<String> additionalLabelValues, double value)
    {
      // include prefix in metric name
      dropwizardName = _PREFIX + dropwizardName;

      // add designer installation id
      additionalLabelNames = additionalLabelNames == null ? new ArrayList<>() : new ArrayList<>(additionalLabelNames);
      additionalLabelValues = additionalLabelValues == null ? new ArrayList<>() : new ArrayList<>(additionalLabelValues);
      if (!additionalLabelNames.contains(_VERSIONID_LABEL_NAME))
      {
        additionalLabelNames.add(_VERSIONID_LABEL_NAME);
        additionalLabelValues.add(InstallationID.get().asText());
      }

      return super.createSample(dropwizardName, nameSuffix, additionalLabelNames, additionalLabelValues, value);
    }
  }
}
