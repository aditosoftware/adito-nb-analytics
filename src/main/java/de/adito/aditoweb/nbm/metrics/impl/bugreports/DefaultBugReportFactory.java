package de.adito.aditoweb.nbm.metrics.impl.bugreports;

import de.adito.aditoweb.nbm.metrics.impl.bugreports.IBugReport.Screenshot;
import de.adito.aditoweb.nbm.metrics.impl.detectors.ThreadUtility;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metainfo.deploy.IDeployMetaInfoProvider;
import lombok.*;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.project.*;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.modules.Places;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Default-Implementation of {@link IBugReportFactory} that captures all
 * necessary data directly if a report was created
 *
 * @author w.glanzer, 13.06.2023
 */
@Log
class DefaultBugReportFactory implements IBugReportFactory
{

  protected static final IBugReportFactory INSTANCE = new DefaultBugReportFactory();

  @NonNull
  @Override
  public IMutableBugReport create()
  {
    return new DefaultReport.DefaultReportBuilder()
        .threadDump(ThreadUtility.getThreadDump(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)))
        .systemState(new IBugReport.OperatingSystemState(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
                                                         ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(),
                                                         Runtime.getRuntime().availableProcessors(),
                                                         Runtime.getRuntime().freeMemory(),
                                                         Runtime.getRuntime().totalMemory(),
                                                         Runtime.getRuntime().maxMemory(),
                                                         System.getProperties()))
        .screenshots(captureScreenshots())
        .logs(collectLogs())
        .projectDetails(getProjectDetails())
        .build();
  }

  /**
   * Captures screenshots of all currently visible frames
   *
   * @return a list of screenshots as bytes
   */
  @Nullable
  private List<Screenshot> captureScreenshots()
  {
    try
    {
      return Arrays.stream(Window.getWindows())
          .filter(Window::isVisible)
          .map(pWindow -> {
            Graphics2D g2d = null;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
              BufferedImage img = new BufferedImage(pWindow.getWidth(), pWindow.getHeight(), BufferedImage.TYPE_INT_RGB);
              g2d = img.createGraphics();

              // Paint everything to the image
              pWindow.paint(g2d);

              // write the gathered data on the output stream
              ImageIO.write(img, "jpg", baos);
              return new Screenshot(pWindow.getName() + ".jpg", baos.toByteArray());
            }
            catch (Exception e)
            {
              log.log(Level.WARNING, "Failed to render screenshot to file", e);
              return null;
            }
            finally
            {
              if (g2d != null)
                g2d.dispose();
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
    catch (Exception e)
    {
      log.log(Level.WARNING, "Failed to capture screenshots", e);
      return null;
    }
  }

  /**
   * Collects all log files we have available
   *
   * @return all log files as list
   */
  @NonNull
  private List<IBugReport.LogFile> collectLogs()
  {
    List<IBugReport.LogFile> files = new ArrayList<>();
    File logDir = new File(Places.getUserDirectory(), "var/log");
    if (logDir.isDirectory() && logDir.exists())
    {
      for (File file : Objects.requireNonNull(logDir.listFiles()))
      {
        try
        {
          if (!file.getName().toLowerCase().endsWith(".lck") && file.length() > 0) //ignore lock-files and empty-files
            files.add(new IBugReport.LogFile(file.getName(), Files.readAllBytes(file.toPath())));
        }
        catch (IOException e)
        {
          log.log(Level.WARNING, "Failed to read file " + file.getPath(), e);
        }
      }
    }

    return files;
  }

  /**
   * Collects all details we know about all currently opened projects
   *
   * @return the details of all projects as list
   */
  @NonNull
  private List<IBugReport.ProjectDetails> getProjectDetails()
  {
    return Arrays.stream(OpenProjects.getDefault().getOpenProjects())
        .map(pProject -> new IBugReport.ProjectDetails(ProjectUtils.getInformation(pProject).getName(), getDetails(pProject)))
        .collect(Collectors.toList());
  }

  /**
   * Collects all details we know about the given project
   *
   * @param pProject Project to get the details for
   * @return the details as key-value-pairs
   */
  @NonNull
  private Map<String, String> getDetails(@NonNull Project pProject)
  {
    Map<String, String> result = new HashMap<>();
    for (IDeployMetaInfoProvider provider : pProject.getLookup().lookupAll(IDeployMetaInfoProvider.class))
      result.putAll(provider.getMetaInfo());
    return result;
  }

  /**
   * Report containing all necessary data
   */
  @Builder
  private static class DefaultReport implements IMutableBugReport
  {
    /**
     * Contains the user comment, if any available
     */
    @Getter
    @Setter
    @Nullable
    private String comment;

    /**
     * Contains the users mail adress, if any available
     */
    @Getter
    @Setter
    @Nullable
    private String mail;

    /**
     * The current thread dump
     */
    @Getter
    @Nullable
    private final String threadDump;

    /**
     * Information about the current OS
     */
    @Getter
    @Nullable
    private final OperatingSystemState systemState;

    /**
     * Screenshots of all currently visible windows
     */
    @Getter
    @Nullable
    private final List<Screenshot> screenshots;

    /**
     * All necessary log files
     */
    @Getter
    @Nullable
    private final List<LogFile> logs;

    /**
     * Information about all currently open projects
     */
    @Getter
    @Nullable
    private final List<ProjectDetails> projectDetails;
  }

}
