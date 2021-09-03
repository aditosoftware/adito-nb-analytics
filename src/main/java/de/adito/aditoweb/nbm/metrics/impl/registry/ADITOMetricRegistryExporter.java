package de.adito.aditoweb.nbm.metrics.impl.registry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.*;
import org.jetbrains.annotations.NotNull;
import org.openide.modules.OnStart;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Exports all collected metrics to the ADITO endpoint
 *
 * @author w.glanzer, 16.07.2021
 */
// Gets called on designer start automatically by MetricRegistryExporterSTarter
@SuppressWarnings("unused")
class ADITOMetricRegistryExporter implements Runnable
{

  private static final Logger _LOGGER = Logger.getLogger(ADITOMetricRegistryExporter.class.getName());
  private static final long _INTERVAL_MS = Long.parseLong(System.getProperty("adito.metrics.exporter.prometheus.push.interval", "30000"));
  private final IMetricRegistryProvider registryProvider;

  public ADITOMetricRegistryExporter(@NotNull IMetricRegistryProvider pRegistryProvider)
  {
    registryProvider = pRegistryProvider;
  }

  @Override
  public void run()
  {
    try
    {
      PushGateway gateway = new PushGateway(_ADITOEndpoint._URL);
      gateway.setConnectionFactory(new _ADITOEndpoint());
      gateway.pushAdd(new DropwizardExports(registryProvider.getRegistry()), _ADITOEndpoint._JOB_NAME);
    }
    catch (Exception e)
    {
      _LOGGER.log(Level.WARNING, "Failed to transport metrics to gateway", e);
    }
  }

  /**
   * Gets called on designer start automatically
   */
  @OnStart
  public static class MetricRegistryExporterStarter implements Runnable
  {
    @Override
    public void run()
    {
      Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                                                     .setNameFormat("tMetricExporter-%d")
                                                     .setDaemon(true)
                                                     .setPriority(Thread.MIN_PRIORITY)
                                                     .build())
          .scheduleAtFixedRate(new ADITOMetricRegistryExporter(IMetricRegistryProvider.getDefault()), 0, _INTERVAL_MS, TimeUnit.MILLISECONDS);
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
}
