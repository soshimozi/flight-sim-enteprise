package net.fseconomy.util;

import org.slf4j.LoggerFactory;

public class GlobalLogger
{
    static final String APPLICATION_LOG = "FSEconomy";
    static final String DEBUG_LOG = "FSE-Debug";
    static final String JSP_LOG = "FSE-Jsp";
    static final String FLIGHT_LOG = "FSE-Flight";
    static final String GROUPAUDIT_LOG = "Group-Audit";
    static final String EXPLOITAUDIT_LOG = "Exploit-Audit";

    public static void logApplicationLog( String message, Class source ) {
        LoggerFactory.getLogger(source).info("[" + APPLICATION_LOG + "]" + message);
    }

    public static void logDebugLog( String message, Class source ) {
        LoggerFactory.getLogger(source).info("[" + DEBUG_LOG + "]" + message);
    }

    public static void logJspLog( String message ) {
        LoggerFactory.getLogger(GlobalLogger.class).info("[" + JSP_LOG + "]" + message);
    }

    public static void logFlightLog( String message, Class source ) {
        LoggerFactory.getLogger(source).info("[" + FLIGHT_LOG + "]" + message);
    }

    public static void logGroupAuditLog( String message, Class source ) {
        LoggerFactory.getLogger(source).info("[" + GROUPAUDIT_LOG + "]" + message);
    }

    public static void logExploitAuditLog( String message, Class source ) {
        LoggerFactory.getLogger(source).info("[" + EXPLOITAUDIT_LOG + "]" + message);
    }
}
