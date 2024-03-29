package de.adito.aditoweb.nbm.metrics.impl;

import lombok.NonNull;
import org.openide.modules.OnStart;
import org.openide.util.NbPreferences;

import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Unique identifier of a single designer installation
 *
 * @author w.glanzer, 03.09.2021
 */
public class InstallationID
{
  private static final String _VERSION_KEY = "adito.designer.version";
  private static final String _ID_KEY = "adito.designer.installation.id";
  private static final String _SYSTEMPROP_KEY = _ID_KEY;
  private static InstallationID _INSTANCE;
  private final String value;
  private String aditoVersion;

  /**
   * @return the instance of the current installation id
   */
  @NonNull
  public static InstallationID get()
  {
    if (_INSTANCE == null)
    {
      Preferences prefs = NbPreferences.root(); // do not store it in module prefs, because we want to persist it between addon installation / removal.
      String installationIDText = prefs.get(_ID_KEY, "");

      // generate new id if necessary
      if (installationIDText == null || installationIDText.trim().isEmpty())
        prefs.put(_ID_KEY, (installationIDText = UUID.randomUUID().toString()));

      _INSTANCE = new InstallationID(installationIDText);
    }

    return _INSTANCE;
  }

  private InstallationID(@NonNull String pValue)
  {
    value = pValue;
  }

  /**
   * @return the installation id without version
   */
  @NonNull
  public String getID()
  {
    return value;
  }

  /**
   * @return true, if the adito version is not known
   */
  public boolean isUnknownVersion()
  {
    String v = getFullVersion();
    return v.endsWith("DEV") || v.equals("UNKNOWN");
  }

  /**
   * @return the adito version, without the RC/TEST specifications
   */
  @NonNull
  public String getVersion()
  {
    return getFullVersion().split("_")[0];
  }

  /**
   * @return the adito version, containing all specifications
   */
  @NonNull
  public String getFullVersion()
  {
    if (aditoVersion == null)
    {
      aditoVersion = System.getProperty(_VERSION_KEY);
      if (aditoVersion != null)
        aditoVersion = aditoVersion.replace('-', '_');
    }

    return aditoVersion == null || aditoVersion.trim().isEmpty() ? "UNKNOWN" : aditoVersion;
  }

  /**
   * Gets called on designer start automatically
   */
  @OnStart
  @SuppressWarnings("unused") // called by service
  public static class InstallationIDSetter implements Runnable
  {
    @Override
    public void run()
    {
      System.setProperty(_SYSTEMPROP_KEY, InstallationID.get().value);
    }
  }

}
