package de.adito.aditoweb.nbm.metrics.impl.actions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.metrics.impl.bugreports.*;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import de.adito.aditoweb.nbm.metrics.impl.user.IUserAgreement;
import de.adito.notification.INotificationFacade;
import de.adito.swing.TableLayoutUtil;
import info.clearthought.layout.TableLayout;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.*;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.*;
import org.openide.awt.*;
import org.openide.modules.Places;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.lang.Exception;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import static org.openide.NotifyDescriptor.*;

/**
 * Action to transmit a bug report
 *
 * @author w.glanzer, 13.06.2023
 */
@ActionID(category = "adito/ribbon", id = "de.adito.aditoweb.nbm.metrics.impl.actions.SendBugReportAction")
@ActionRegistration(displayName = "#ACTION_SendBugReport", iconBase = "de/adito/aditoweb/nbm/metrics/impl/actions/bugreport.png")
@ActionReferences({
    @ActionReference(path = "Toolbars/analytics", position = 0),
    @ActionReference(path = "Menu/Help", position = 1310)
})
public class SendBugReportAction extends AbstractAction
{
  private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                                 .setNameFormat("tBugReport-%d")
                                                                                 .build());
  private static final String DLG_SEND_DATA_OPTION = NbBundle.getMessage(SendBugReportAction.class, "NAME_SendBugReportDialog_SendData");
  private final TemporaryFileProvider temporaryFileProvider = new TemporaryFileProvider();

  @SuppressWarnings({"FieldCanBeLocal", "unused"}) // Strong Ref
  private final Disposable disposable;

  public SendBugReportAction()
  {
    disposable = IUserAgreement.getInstance().sendingAnalyticsAllowed()
        .subscribe(pAllowed -> SwingUtilities.invokeLater(() -> setEnabled(pAllowed))); // enable, if sendings is allowed
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    CompletableFuture.supplyAsync(this::createReport, EXECUTOR)
        .thenApplyAsync(this::enrichWithUserInput, EXECUTOR)
        .thenApplyAsync(this::sendReport, EXECUTOR)
        .thenAcceptAsync(this::notifyReportSent, EXECUTOR)
        .thenRunAsync(this::cleanup, EXECUTOR);
  }

  /**
   * Captures a new bug report and shows a progress handle during capture
   *
   * @return the newly created report
   */
  @NonNull
  private IMutableBugReport createReport()
  {
    try (ProgressHandle handle = ProgressHandle.createSystemHandle(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReport_CaptureReport"), null))
    {
      handle.start();
      handle.switchToIndeterminate();
      return IBugReportFactory.getInstance().create();
    }
  }

  /**
   * Enriches the given report with anything manually given by the user
   *
   * @param pReport report to be enriched
   * @return the enriched report
   */
  @Nullable
  private IBugReport enrichWithUserInput(@Nullable IMutableBugReport pReport)
  {
    // if the report could not be calculated, then do not ask the user anything
    if (pReport == null)
      return null;

    BugReportDialogContentPanel reportPanel = new BugReportDialogContentPanel(temporaryFileProvider, pReport);
    NotifyDescriptor descr = new NotifyDescriptor(reportPanel, NbBundle.getMessage(SendBugReportAction.class, "TITLE_SendBugReportDialog"), DEFAULT_OPTION, QUESTION_MESSAGE,
                                                  new Object[]{DLG_SEND_DATA_OPTION, CANCEL_OPTION}, DLG_SEND_DATA_OPTION);
    Object result = DialogDisplayer.getDefault().notify(descr);
    if (Objects.equals(DLG_SEND_DATA_OPTION, result))
    {
      reportPanel.enrich(pReport);
      return pReport;
    }

    return null;
  }

  /**
   * Sends the given report
   *
   * @param pReport report to send
   */
  @Nullable
  private String sendReport(@Nullable IBugReport pReport)
  {
    // if the report could not be calculated or enriched, then do not transmit it
    if (pReport == null)
      return null;

    try (ProgressHandle handle = ProgressHandle.createSystemHandle(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReport_SendReport"), null))
    {
      handle.start();
      handle.switchToIndeterminate();
      return IEventLogger.getInstance().captureBugReport(pReport, null);
    }
  }

  /**
   * Cleans everything up, that was modified / created during workflow
   */
  private void cleanup()
  {
    temporaryFileProvider.deleteAll();
  }

  /**
   * Notifies the user, that the report with the given ID has been sent
   *
   * @param pReportID ID of the report that was sent
   */
  private void notifyReportSent(@Nullable String pReportID)
  {
    if (pReportID == null)
      return;

    INotificationFacade.INSTANCE.notify(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReport_ReportSent_Title"),
                                        NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReport_ReportSent_Message"),
                                        false, e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pReportID), null));
  }

  /**
   * Panel that contains all necessary components for the dialog
   */
  private static class BugReportDialogContentPanel extends JPanel
  {
    private static final Preferences PREFS = NbPreferences.forModule(BugReportDialogContentPanel.class);
    private static final String PREF_CONTACT = "contact_ok";
    private static final String PREF_EMAIL = "mail";

    private final TemporaryFileProvider fileProvider;
    private final IBugReport report;

    private JTextArea commentArea;
    private JCheckBox contactBox;
    private JTextField emailField;

    public BugReportDialogContentPanel(@NonNull TemporaryFileProvider pFileProvider, @NonNull IBugReport pReport)
    {
      fileProvider = pFileProvider;
      report = pReport;
      setPreferredSize(new Dimension(550, 600));
      setBorder(new CompoundBorder(getBorder(), new EmptyBorder(5, 5, 0, 5)));
      initComponents();

      // automatically focus the user input field
      SwingUtilities.invokeLater(commentArea::requestFocusInWindow);
    }

    /**
     * Appends all information, that was given by the user, to the given bug report
     *
     * @param pReport Report to enrich
     */
    public void enrich(@NonNull IMutableBugReport pReport)
    {
      pReport.setComment(commentArea.getText());
      if (contactBox.isSelected())
        pReport.setMail(emailField.getText());
    }

    /**
     * Initializes all swing components and adds it to this dialog content panel
     */
    private void initComponents()
    {
      setLayout(new TableLayout(new double[]{TableLayout.FILL}, new double[]{
          TableLayout.PREFERRED,
          10,
          TableLayout.PREFERRED,
          10,
          TableLayout.FILL,
          10,
          TableLayout.PREFERRED
      }));
      TableLayoutUtil tlu = new TableLayoutUtil(this);

      // Dialog title
      JPanel titlePanel = new JPanel(new BorderLayout());
      titlePanel.add(new JLabel(ImageUtilities.loadImageIcon("de/adito/aditoweb/nbm/metrics/impl/actions/bugreport_logo.png", false)), BorderLayout.WEST);
      JLabel sorryLabel = new JLabel(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReportDialog_Title"));
      sorryLabel.setBorder(new EmptyBorder(15, 20, 20, 10));
      titlePanel.add(sorryLabel, BorderLayout.CENTER);
      titlePanel.setBorder(new MatteBorder(0, 0, 1, 0, new JSeparator().getBackground()));
      tlu.add(0, 0, titlePanel);

      // Upper Description
      JPanel description = new JPanel();
      description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
      description.add(new JLabel(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReportDialog_Description")));
      description.add(Box.createVerticalStrut(10));
      JLabel openReportLbl = new JLabel(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReportDialog_OpenReport"));
      openReportLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      openReportLbl.addMouseListener(createOpenReportListener());
      openReportLbl.setMaximumSize(openReportLbl.getPreferredSize());
      description.add(openReportLbl);
      description.add(Box.createVerticalStrut(10));
      description.add(new JLabel(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReportDialog_DetailDescription")));
      tlu.add(0, 2, description);

      // Lower comment section for user input
      commentArea = new JTextArea();
      tlu.add(0, 4, new JScrollPane(commentArea));

      // Contact information
      JPanel contactPanel = new JPanel(new BorderLayout(10, 10));
      contactBox = new JCheckBox(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReportDialog_Contact"),
                                 PREFS.getBoolean(PREF_CONTACT, false));
      contactBox.addActionListener(e -> PREFS.putBoolean(PREF_CONTACT, contactBox.isSelected()));
      contactPanel.add(contactBox, BorderLayout.NORTH);
      JPanel emailPanel = new JPanel(new BorderLayout(10, 10));
      emailField = new JTextField(contactBox.isSelected() ? PREFS.get(PREF_EMAIL, "") : "");
      emailField.setPreferredSize(new Dimension(230, emailField.getPreferredSize().height));
      emailField.getDocument().addDocumentListener(new DocumentChangeListener(pText -> PREFS.put(PREF_EMAIL, pText)));
      emailPanel.add(new JLabel(NbBundle.getMessage(SendBugReportAction.class, "TEXT_SendBugReportDialog_Contact_Mail")), BorderLayout.WEST);
      emailPanel.add(emailField, BorderLayout.CENTER);
      emailPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
      emailField.setEnabled(contactBox.isSelected());
      contactPanel.add(emailPanel, BorderLayout.WEST);
      tlu.add(0, 6, contactPanel);
      contactBox.addActionListener(e -> emailField.setEnabled(contactBox.isSelected()));
    }

    /**
     * @return the listener that should be triggered, if "Preview Bug Report" gets clicked
     */
    @NonNull
    private MouseListener createOpenReportListener()
    {
      return new MouseAdapter()
      {
        @Override
        public void mouseReleased(MouseEvent e)
        {
          try
          {
            File targetFile = fileProvider.createFile();
            IEventLogger.getInstance().captureBugReport(report, targetFile);
            Desktop.getDesktop().edit(targetFile);
          }
          catch (Exception ex)
          {
            // Notify the user about the exception
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
          }
        }
      };
    }
  }

  /**
   * Contains all temporary files that were created during the send report workflow
   */
  @Log
  private static class TemporaryFileProvider
  {
    private final Set<File> createdFiles = new HashSet<>();

    /**
     * Creates a new unique temporary file
     *
     * @return the newly created file
     */
    @NonNull
    public File createFile()
    {
      File file = Places.getCacheSubfile("analytics/" + UUID.randomUUID() + ".report.txt");
      createdFiles.add(file);
      return file;
    }

    /**
     * Deletes every file that was created during {@link TemporaryFileProvider#createFile()}
     */
    public void deleteAll()
    {
      createdFiles.forEach(pFile -> {
        try
        {
          //noinspection ResultOfMethodCallIgnored
          pFile.delete();
        }
        catch (Exception e)
        {
          // only log the exception, because it is not worth notyfing the user about it
          log.log(Level.WARNING, "", e);
        }
      });
      createdFiles.clear();
    }
  }

  /**
   * DocumentListener that can be written in one line
   */
  @Log
  @RequiredArgsConstructor
  private static class DocumentChangeListener implements DocumentListener
  {
    /**
     * Consumer that gets executed when something changes
     */
    @NonNull
    private final Consumer<String> onChange;

    @Override
    public void insertUpdate(DocumentEvent e)
    {
      changedUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
      changedUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
      try
      {
        onChange.accept(e.getDocument().getText(0, e.getDocument().getLength()));
      }
      catch (BadLocationException ex)
      {
        log.log(Level.WARNING, "", ex);
      }
    }
  }

}
