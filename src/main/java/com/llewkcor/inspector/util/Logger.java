package com.llewkcor.inspector.util;

public class Logger {
    private final String name;
    private final boolean debugEnabled;

    public Logger(String name) {
        this.name = name;
        this.debugEnabled = false;
    }

    public Logger(String name, boolean debugEnabled) {
        this.name = name;
        this.debugEnabled = debugEnabled;
    }

    public void info(String message) {
        System.out.println("[" + name + "]" + "[INFO]" + " " + message);
    }

    public void warn(String message) {
        System.out.println("[" + name + "]" + "[WARN]" + " " + message);
    }

    public void severe(String message) {
        System.out.println("[" + name + "]" + "[SEVERE]" + " " + message);
    }

    public void debug(String message) {
        if (!debugEnabled) {
            return;
        }

        System.out.println("[" + name + "]" + "[DEBUG]" + " " + message);
    }
}
