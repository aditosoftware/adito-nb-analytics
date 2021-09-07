package de.adito.aditoweb.nbm.metrics.impl.options;

import com.google.common.base.Suppliers;
import de.adito.aditoweb.nbm.metrics.impl.user.IUserAgreement;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

/**
 * Adds a tab in the options menu
 *
 * @author w.glanzer, 06.09.2021
 */
@OptionsPanelController.SubRegistration(displayName = "Data Sharing", id = "analytics", position = 600, location = "Adito")
public class AnalyticsOptionsPanelController extends OptionsPanelController
{

  private final Supplier<AnalyticsOptionsPanel> pluginOptionsPanel = Suppliers.memoize(AnalyticsOptionsPanel::new);

  @Override
  public void update()
  {
    pluginOptionsPanel.get().setAllowAnalytics(IUserAgreement.getInstance().sendingAnalyticsAllowed().blockingFirst());
  }

  @Override
  public void applyChanges()
  {
    IUserAgreement.getInstance().getMutable().setSendingAnalyticsAllowed(pluginOptionsPanel.get().isAllowAnalytics());
  }

  @Override
  public void cancel()
  {
    // nothing to do here, it gets reset each time "update()" gets  called
  }

  @Override
  public boolean isValid()
  {
    return true; // always valid, because we do not have to validate any input parameters
  }

  @Override
  public boolean isChanged()
  {
    return !IUserAgreement.getInstance().sendingAnalyticsAllowed().blockingFirst().equals(pluginOptionsPanel.get().isAllowAnalytics());
  }

  @Override
  public JComponent getComponent(Lookup pLookup)
  {
    return pluginOptionsPanel.get();
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener pPropertyChangeListener)
  {
    pluginOptionsPanel.get().addPropertyChangeListener(pPropertyChangeListener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener pPropertyChangeListener)
  {
    pluginOptionsPanel.get().removePropertyChangeListener(pPropertyChangeListener);
  }

}
