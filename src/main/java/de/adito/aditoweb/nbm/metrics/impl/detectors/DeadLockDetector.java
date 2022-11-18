package de.adito.aditoweb.nbm.metrics.impl.detectors;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.metrics.IDeadlockDetector;
import org.openide.util.lookup.ServiceProvider;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author m.kaspera, 14.12.2021
 */
@ServiceProvider(service = IDeadlockDetector.class)
public class DeadLockDetector extends ARunnableDetector implements IDeadlockDetector
{

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
