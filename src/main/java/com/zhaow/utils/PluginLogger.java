package com.zhaow.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Plugin logger utility that writes logs to both IntelliJ standard logger and custom log file
 * Logs are written to .idea/restful-toolkit-fix/logs directory
 */
public class PluginLogger {
    private static final String LOG_DIR_NAME = "restful-toolkit-fix";
    private static final String LOG_SUBDIR = "logs";
    private static final Logger INTELLIJ_LOGGER = Logger.getLogger(PluginLogger.class.getName());
    
    private final String className;
    private final Project project;
    private PrintWriter logWriter;
    private boolean initialized = false;
    
    public PluginLogger(@NotNull Class<?> clazz, @Nullable Project project) {
        this.className = clazz.getSimpleName();
        this.project = project;
        initializeLogFile();
    }
    
    private void initializeLogFile() {
        if (initialized) {
            return;
        }
        
        try {
            String logFilePath = getLogFilePath();
            if (logFilePath != null) {
                File logFile = new File(logFilePath);
                File parentDir = logFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        INTELLIJ_LOGGER.warning("Failed to create log directory: " + parentDir.getAbsolutePath());
                    }
                }
                
                this.logWriter = new PrintWriter(new FileWriter(logFile, true), true);
                initialized = true;
                // Write initial log without calling info() to avoid recursion
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                String fullMessage = String.format("[%s] [%s] [%s] %s", timestamp, "INFO", className, "PluginLogger initialized");
                logWriter.println(fullMessage);
            }
        } catch (Exception e) {
            // Fallback to IntelliJ logger only
            INTELLIJ_LOGGER.warning("Failed to initialize custom log file: " + e.getMessage());
            initialized = true; // Mark as initialized to avoid repeated attempts
        }
    }
    
    private String getLogFilePath() {
        if (project == null) {
            return null;
        }
        
        try {
            // Use project base path instead of VirtualFile to avoid threading issues
            String basePath = project.getBasePath();
            if (basePath == null) {
                return null;
            }
            
            // Build path using File API instead of VirtualFile to avoid createChildDirectory issues
            File ideaDir = new File(basePath, ".idea");
            if (!ideaDir.exists()) {
                boolean created = ideaDir.mkdirs();
                if (!created) {
                    INTELLIJ_LOGGER.warning("Failed to create .idea directory");
                    return null;
                }
            }
            
            File logRootDir = new File(ideaDir, LOG_DIR_NAME);
            if (!logRootDir.exists()) {
                boolean created = logRootDir.mkdirs();
                if (!created) {
                    INTELLIJ_LOGGER.warning("Failed to create log root directory");
                    return null;
                }
            }
            
            File logDir = new File(logRootDir, LOG_SUBDIR);
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (!created) {
                    INTELLIJ_LOGGER.warning("Failed to create log subdirectory");
                    return null;
                }
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String logFileName = "plugin-" + timestamp + ".log";
            File logFile = new File(logDir, logFileName);
            
            return logFile.getAbsolutePath();
        } catch (Exception e) {
            INTELLIJ_LOGGER.warning("Failed to get log file path: " + e.getMessage());
            return null;
        }
    }
    
    public void info(String message) {
        log("INFO", message);
    }
    
    public void warn(String message) {
        log("WARN", message);
    }
    
    public void error(String message) {
        log("ERROR", message);
    }

    public void error(String message, Exception ex) {
        log("ERROR", message + ": " + ex.getMessage());
        ex.printStackTrace();
    }
    
    public void debug(String message) {
        log("DEBUG", message);
    }
    
    private void log(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String fullMessage = String.format("[%s] [%s] [%s] %s", timestamp, level, className, message);
        
        // Log to IntelliJ standard logger
        switch (level) {
            case "ERROR":
                INTELLIJ_LOGGER.severe(fullMessage);
                break;
            case "WARN":
                INTELLIJ_LOGGER.warning(fullMessage);
                break;
            case "INFO":
                INTELLIJ_LOGGER.info(fullMessage);
                break;
            case "DEBUG":
                INTELLIJ_LOGGER.fine(fullMessage);
                break;
        }
        
        // Log to custom file if available
        if (logWriter != null) {
            logWriter.println(fullMessage);
        }
    }
    
    public void close() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }
}