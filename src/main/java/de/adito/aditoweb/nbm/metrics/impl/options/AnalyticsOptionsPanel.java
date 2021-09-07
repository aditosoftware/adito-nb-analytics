package de.adito.aditoweb.nbm.metrics.impl.options;

import de.adito.aditoweb.nbm.metrics.impl.user.IUserAgreement;
import de.adito.swing.TableLayoutUtil;
import info.clearthought.layout.TableLayout;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * @author w.glanzer, 06.09.2021
 */
class AnalyticsOptionsPanel extends JPanel
{

  private final JCheckBox allowAnalytics;

  public AnalyticsOptionsPanel()
  {
    allowAnalytics = new JCheckBox();
    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    final double gap = 15;
    double[] cols = {gap, pref, gap, fill, gap};
    double[] rows = {gap,
                     pref,
                     pref,
                     gap};
    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);
    tlu.add(1, 1, new JLabel(NbBundle.getMessage(AnalyticsOptionsPanel.class, "LBL_SendUsageStats")));
    tlu.add(3, 1, allowAnalytics);

    JLabel hintLabel = new JLabel("<html><body>" + NbBundle.getMessage(IUserAgreement.class, "TXT_AnalyticsAgreement").replace("\n", "<br/>") + "</body></html>");
    hintLabel.setEnabled(false);
    tlu.add(1, 2, 3, 2, hintLabel);
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener pPropertyChangeListener)
  {
    super.addPropertyChangeListener(pPropertyChangeListener);
    allowAnalytics.addPropertyChangeListener(pPropertyChangeListener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener pPropertyChangeListener)
  {
    super.removePropertyChangeListener(pPropertyChangeListener);
    allowAnalytics.removePropertyChangeListener(pPropertyChangeListener);
  }

  public void setAllowAnalytics(boolean pAllowAnalytics)
  {
    allowAnalytics.setSelected(pAllowAnalytics);
  }

  public boolean isAllowAnalytics()
  {
    return allowAnalytics.isSelected();
  }

}
