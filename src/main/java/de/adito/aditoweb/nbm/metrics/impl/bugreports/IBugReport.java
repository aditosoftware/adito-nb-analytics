package de.adito.aditoweb.nbm.metrics.impl.bugreports;

import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.lang.management.OperatingSystemMXBean;
import java.util.*;

/**
 * Object that contains everything for a single bug report.
 * If you want to edit something inside, have a look at {@link IMutableBugReport}
 *
 * @author w.glanzer, 13.06.2023
 * @see IMutableBugReport
 */
public interface IBugReport
{

  /**
   * The comment that the user gave during send.
   * May be null, if no comment was entered yet
   *
   * @return the comment or null
   */
  @Nullable
  String getComment();

  /**
   * The mail that the user gave during send.
   * May be null, if no mail was entered yet or it was not provided during workflow.
   *
   * @return the mail or null
   */
  @Nullable
  String getMail();

  /**
   * The ThreadDump that was captured during report creation.
   *
   * @return the dump or null, if it could not be captured.
   */
  @Nullable
  String getThreadDump();

  /**
   * @return information about the system of the user
   */
  @Nullable
  OperatingSystemState getSystemState();

  /**
   * All screenshots of all our windows that are
   * currently shown on screen.
   *
   * @return the {@link Screenshot} objects of all windows
   */
  @Nullable
  List<Screenshot> getScreenshots();

  /**
   * @return all log files to send with this bug report
   */
  @Nullable
  List<LogFile> getLogs();

  /**
   * All necessary project information about all opened projects
   *
   * @return project details to send with this bug report
   */
  @Nullable
  List<ProjectDetails> getProjectDetails();

  /**
   * Contains everything we need to know about the current system state
   */
  @RequiredArgsConstructor
  class OperatingSystemState
  {
    /**
     * The {@link OperatingSystemMXBean#getSystemLoadAverage()}, if we are able to read it
     */
    @Getter
    private final double systemLoadAverage;

    /**
     * The {@link OperatingSystemMXBean#getAvailableProcessors()}, if we are able to read it
     */
    @Getter
    private final int systemCPUCount;

    /**
     * The available processors, read by {@link Runtime#availableProcessors()}
     */
    @Getter
    private final int runtimeCPUCount;

    /**
     * The total amount of free memory, read by {@link Runtime#freeMemory()}, in bytes
     */
    @Getter
    private final double freeMemory;

    /**
     * The total amount of total memory, read by {@link Runtime#totalMemory()}, in bytes
     */
    @Getter
    private final double totalMemory;

    /**
     * The total amount of maximum memory, read by {@link Runtime#maxMemory()}, in bytes
     */
    @Getter
    private final double maxMemory;

    /**
     * All currently defined system properties
     */
    @Getter
    private final Map<Object, Object> systemProperties;
  }

  /**
   * Contains data of a single window and its current state as screenshot
   */
  @RequiredArgsConstructor
  class Screenshot
  {
    /**
     * Name of the window (incl. the extension of the algorithm used), that was captured
     */
    @Getter
    @Nullable
    private final String name;

    /**
     * The data as image bytes
     */
    @Getter
    private final byte @NonNull [] data;
  }

  /**
   * Represents a single log file
   */
  @RequiredArgsConstructor
  class LogFile
  {
    /**
     * Name of the log file
     */
    @Getter
    @NonNull
    private final String name;

    /**
     * The data of the file
     */
    @Getter
    private final byte @NonNull [] data;
  }

  /**
   * Contains everything we need to know about one opened project
   */
  @RequiredArgsConstructor
  class ProjectDetails
  {
    /**
     * Name of the project
     */
    @Getter
    @NonNull
    private final String name;

    /**
     * All details we know about the project
     */
    @Getter
    @Nullable
    private final Map<String, String> details;
  }

}
