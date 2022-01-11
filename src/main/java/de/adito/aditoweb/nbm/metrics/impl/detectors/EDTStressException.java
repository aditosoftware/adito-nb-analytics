package de.adito.aditoweb.nbm.metrics.impl.detectors;

/**
 * @author m.kaspera, 22.12.2021
 */
public class EDTStressException extends Exception
{

  public EDTStressException(String message)
  {
    super(message);
  }

  public EDTStressException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public EDTStressException(Throwable cause)
  {
    super(cause);
  }
}
