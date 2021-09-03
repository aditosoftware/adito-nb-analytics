package de.adito.aditoweb.nbm.metrics.impl.registry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.PushGateway;
import org.jetbrains.annotations.NotNull;
import org.openide.modules.OnStart;

import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * @author w.glanzer, 16.07.2021
 */
// Gets called on designer start automatically by MetricRegistryExporterSTarter
@SuppressWarnings("unused")
class MetricRegistryExporter implements Runnable
{

  private static final Logger _LOGGER = Logger.getLogger(MetricRegistryExporter.class.getName());
  private static final long _INTERVAL_MS = Long.parseLong(System.getProperty("adito.metrics.exporter.prometheus.push.interval", "30000"));
  private final IMetricRegistryProvider registryProvider;

  public MetricRegistryExporter(@NotNull IMetricRegistryProvider pRegistryProvider)
  {
    registryProvider = pRegistryProvider;
  }

  @Override
  public void run()
  {
    try
    {
      //todo dummy impl
      //todo refactor
      PushGateway gateway = new PushGateway(new URL("https://prometheus-gateway.c2.adito.cloud"));
      gateway.setConnectionFactory(pURL -> {
        HttpURLConnection url = (HttpURLConnection) new URL(pURL).openConnection();
        url.setAuthenticator(new Authenticator()
        {
          @Override
          protected PasswordAuthentication getPasswordAuthentication()
          {
            return new PasswordAuthentication("A7PaC9zH6ZMqR32LAGSc", "6adzg5HeYK4h0xFfcJca".toCharArray());
          }
        });
        return url;
      });
      gateway.pushAdd(new DropwizardExports(registryProvider.getRegistry()), "some_job");
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
          .scheduleAtFixedRate(new MetricRegistryExporter(IMetricRegistryProvider.getDefault()), 0, _INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
  }
}
