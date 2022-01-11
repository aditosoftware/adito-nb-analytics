package de.adito.aditoweb.nbm.metrics.impl.detectors;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.metrics.IDeadlockDetector;
import io.reactivex.rxjava3.disposables.Disposable;
import org.openide.util.lookup.ServiceProvider;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author m.kaspera, 14.12.2021
 */
@ServiceProvider(service = IDeadlockDetector.class)
public class DeadLockDetector extends ARunnableDetector implements IDeadlockDetector
{

  static final Set<String> SYSTEM_THREADS = Set.of("File Watcher", "W32 File Monitor", "Signal Dispatcher", "Finalizer", "Sweeper thread",
                                                   "Common-Cleaner", "Service Thread", "Active Reference Queue Daemon", "CLI Requests Server",
                                                   "Java2D Disposer", "AWT-Shutdown", "FelixDispatchQueue", "FelixFrameworkWiring", "TimerQueue",
                                                   "Swing-Shell", "FelixStartLevel", "ReferenceQueueThread", "Batik CleanerThread");
  static final List<String> MONITORED_CLASS_INFIXES = List.of(".nbm.", ".adito.", ".rxjava.");
  static final Logger LOGGER = Logger.getLogger(DeadLockDetector.class.getName());

  @Override
  String getThreadNameFormat()
  {
    return "Analytics-DeadLockDetector-%d";
  }

  @Override
  long getTimeInterval()
  {
    return 15;
  }

  @Override
  Runnable getRunner()
  {
    return new DeadlockDetectorRunner();
  }

  @Override
  Logger getLogger()
  {
    return LOGGER;
  }

}
