package com.gitlab.rmarzec.task;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;

import com.gitlab.rmarzec.core.ConsoleLog;
import com.gitlab.rmarzec.core.DriverFactory;
import com.gitlab.rmarzec.core.TestConfig;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Task 1 - smoke check of the toolchain: the project has to build and run on Java 1.8 with
 * Maven, and the WebDriver setup has to produce a working browser session.
 *
 * <p>This test intentionally does not extend {@code BaseTest}: the first check needs no
 * browser at all, and the second one owns its driver so that the toolchain check can be run
 * on a machine without a browser (for example {@code mvn test -Dtest=Task1Test#toolchainRunsOnJava8}).
 */
public class Task1Test {

    private static final int REQUIRED_MAJOR_JAVA_VERSION = 8;

    @Test(description = "Task 1 - the project compiles and runs on Java 1.8 under Maven")
    public void toolchainRunsOnJava8() {
        String javaVersion = System.getProperty("java.version");
        String javaSpecificationVersion = System.getProperty("java.specification.version");

        ConsoleLog.step("Task 1 - toolchain");
        ConsoleLog.info("Java version", javaVersion);
        ConsoleLog.info("Java vendor", System.getProperty("java.vendor"));
        ConsoleLog.info("Java specification", javaSpecificationVersion);
        ConsoleLog.info("Compiled for", "Java 1.8 (maven.compiler.source/target = 1.8)");

        assertNotNull(javaVersion, "java.version must be reported by the JVM");
        assertTrue(majorVersionOf(javaSpecificationVersion) >= REQUIRED_MAJOR_JAVA_VERSION,
                "The tests require at least Java 1.8, but ran on " + javaSpecificationVersion);

        // Class file 52 is Java 8: proves the sources really are compiled for Java 1.8.
        assertEquals(classFileMajorVersionOfThisClass(), 52,
                "Test classes must be compiled with a Java 1.8 target");
    }

    @Test(description = "Task 1 - Selenium is wired up and a browser session can be started")
    public void webDriverStartsAndReportsBrowserVersion() {
        WebDriver driver = DriverFactory.createDriver();
        try {
            ConsoleLog.step("Task 1 - WebDriver");
            ConsoleLog.info("Requested browser", TestConfig.browser().name().toLowerCase());
            ConsoleLog.info("Browser", String.valueOf(
                    ((RemoteWebDriver) driver).getCapabilities().getBrowserName()
                            + " " + ((RemoteWebDriver) driver).getCapabilities().getBrowserVersion()));

            driver.get("about:blank");
            assertNotNull(driver.getCurrentUrl(), "The browser session must expose a current URL");
            assertNotNull(((RemoteWebDriver) driver).getSessionId(), "A WebDriver session must be created");
        } finally {
            driver.quit();
        }
    }

    private static int majorVersionOf(String specificationVersion) {
        if (specificationVersion == null) {
            return 0;
        }
        String version = specificationVersion.startsWith("1.")
                ? specificationVersion.substring(2)
                : specificationVersion;
        int dot = version.indexOf('.');
        if (dot > 0) {
            version = version.substring(0, dot);
        }
        try {
            return Integer.parseInt(version.trim());
        } catch (NumberFormatException unexpectedFormat) {
            return 0;
        }
    }

    private int classFileMajorVersionOfThisClass() {
        InputStream classFile = Task1Test.class.getResourceAsStream("Task1Test.class");
        assertNotNull(classFile, "The compiled test class must be readable from the classpath");
        try {
            DataInputStream stream = new DataInputStream(classFile);
            try {
                stream.readInt();  // 0xCAFEBABE magic number
                stream.readUnsignedShort();  // minor version
                return stream.readUnsignedShort();
            } finally {
                stream.close();
            }
        } catch (IOException classFileUnreadable) {
            throw new IllegalStateException("Could not read the compiled class file", classFileUnreadable);
        }
    }
}
