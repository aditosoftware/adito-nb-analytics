package de.adito.aditoweb.nbm.metrics.impl;

import org.jetbrains.annotations.*;
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
  private final String aditoVersion;

  /**
   * @return the instance of the current installation id
   */
  @NotNull
  public static InstallationID get()
  {
    if (_INSTANCE == null)
    {
      Preferences prefs = NbPreferences.root(); // do not store it in module prefs, because we want to persist it between addon installation / removal.
      String installationIDText = prefs.get(_ID_KEY, "");

      // generate new id if necessary
      if (installationIDText == null || installationIDText.trim().isEmpty())
        prefs.put(_ID_KEY, (installationIDText = UUID.randomUUID().toString()));

      _INSTANCE = new InstallationID(installationIDText, System.getProperty(_VERSION_KEY));
    }

    return _INSTANCE;
  }

  private InstallationID(@NotNull String pValue, @Nullable String pAditoVersion)
  {
    value = pValue;
    aditoVersion = pAditoVersion == null || pAditoVersion.trim().isEmpty() ? "UNKNOWN" : pAditoVersion.replace('-', '_');
  }

  /**
   * @return the textual interpretation of the installation id
   */
  @NotNull
  public String asText()
  {
    return value + "-" + aditoVersion;
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