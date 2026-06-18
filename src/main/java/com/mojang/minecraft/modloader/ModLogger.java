package com.mojang.minecraft.modloader.logging;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Centralized logger for ModLoader and mods.
 *
 * Writes to both stdout and logs/modloader.log.
 * Levels: DEBUG, INFO, WARN, ERROR, FATAL.
 * Each mod gets its own tagged logger via ModLogger.forMod("modid").
 */
public class ModLogger {

    public enum Level {
        DEBUG(0, "DEBUG"),
        INFO (1, "INFO "),
        WARN (2, "WARN "),
        ERROR(3, "ERROR"),
        FATAL(4, "FATAL");

        public final int priority;
        public final String label;
        Level(int priority, String label) { this.priority = priority; this.label = label; }
    }

    // ─── Singleton file writer ────────────────────────────────────────────────
    private static PrintWriter fileWriter;
    private static Level globalMinLevel = Level.INFO;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    static {
        initFileWriter();
    }

    private static void initFileWriter() {
        try {
            File logDir = new File("logs");
            logDir.mkdirs();
            File logFile = new File(logDir, "modloader.log");
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            // Write session separator
            fileWriter.println("=== ModLoader session started " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ===");
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("[ModLogger] Could not open log file: " + e.getMessage());
        }
    }

    public static void setGlobalLevel(Level level) { globalMinLevel = level; }

    // ─── Factory ──────────────────────────────────────────────────────────────

    private final String tag;

    private ModLogger(String tag) { this.tag = tag; }

    /** Logger tagged as [ModLoader] — used internally by the modloader. */
    public static final ModLogger SYSTEM = new ModLogger("ModLoader");

    /** Per-mod logger: tag will appear as [modId]. */
    public static ModLogger forMod(String modId) { return new ModLogger(modId); }

    // ─── Logging methods ──────────────────────────────────────────────────────

    public void debug(String msg)               { log(Level.DEBUG, msg, null); }
    public void info (String msg)               { log(Level.INFO,  msg, null); }
    public void warn (String msg)               { log(Level.WARN,  msg, null); }
    public void error(String msg)               { log(Level.ERROR, msg, null); }
    public void error(String msg, Throwable t)  { log(Level.ERROR, msg, t);    }
    public void fatal(String msg, Throwable t)  { log(Level.FATAL, msg, t);    }

    private void log(Level level, String msg, Throwable t) {
        if (level.priority < globalMinLevel.priority) return;

        String time = TIME_FMT.format(new Date());
        String line = String.format("[%s] [%s] [%s] %s", time, level.label, tag, msg);

        // Console (WARN+ → stderr, rest → stdout)
        PrintStream out = (level.priority >= Level.WARN.priority) ? System.err : System.out;
        out.println(line);
        if (t != null) t.printStackTrace(out);

        // File
        if (fileWriter != null) {
            fileWriter.println(line);
            if (t != null) {
                t.printStackTrace(fileWriter);
            }
            fileWriter.flush();
        }
    }

    /** Flush and close the log file — call on game shutdown. */
    public static void shutdown() {
        if (fileWriter != null) {
            fileWriter.println("=== Session ended ===");
            fileWriter.flush();
            fileWriter.close();
        }
    }
}
