package ru.inversion.fru.print.altprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ALTLog
{
    public static final String LOG_ALIAS = "ALTLog";

    private static final Logger g_log = LoggerFactory.getLogger(LOG_ALIAS);

    protected static boolean g_isLogging = false;

    private static boolean isLogging()
{
    return g_isLogging;
}

    public static void configure(boolean enable)
{
    g_isLogging = enable;
}

    public static void trace(Object message)
    {
            if (isLogging()) {
                g_log.trace(String.valueOf(message));
            }
    }

    public static void debug(Object message)
    {
            if (isLogging()) {
                g_log.debug(String.valueOf(message));
            }
    }

    public static void info(Object message)
    {
            if (isLogging()) {
                g_log.info(String.valueOf(message));
            }
    }

    public static void warning(Object message)
    {
            if (isLogging()) {
                g_log.warn(String.valueOf(message));
            }
    }

    public static void error(Object message)
    {
            if (isLogging()) {
                g_log.error(String.valueOf(message));
            }
    }

    public static void tech_info(Object message, Throwable ex)
    {
        if (isLogging()) {
            g_log.info(String.valueOf(message), ex);
        }
    }

    public static void tech_warn(Object message, Throwable ex)
    {
        if (isLogging()) {
            g_log.warn(String.valueOf(message), ex);
        }
    }

    public static void tech_error(Object message, Throwable ex)
    {
        if (isLogging()) {
            g_log.error(String.valueOf(message), ex);
        }
    }
}
