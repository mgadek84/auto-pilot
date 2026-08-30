package com.gitlab.rmarzec.core;

import org.testng.Reporter;

/**
 * Thin wrapper around console output. The tasks require printing results to the console, and
 * routing everything through one place keeps the output consistently formatted and also
 * attaches it to the TestNG report.
 */
public final class ConsoleLog {

    private ConsoleLog() {
    }

    /** Prints a section header, used to separate the steps of a task. */
    public static void step(String message) {
        print("== " + message);
    }

    public static void info(String message) {
        print("   " + message);
    }

    public static void info(String label, String value) {
        print("   " + label + ": " + value);
    }

    private static void print(String message) {
        System.out.println(message);
        Reporter.log(message);
    }
}
