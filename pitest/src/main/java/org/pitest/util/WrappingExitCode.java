package org.pitest.util;

import org.pitest.testapi.Description;

public class WrappingExitCode {

    private ExitCode exitCode;

    private Description currentTestDescription;

    public WrappingExitCode(ExitCode exitCode, Description currentTest) {
        this.exitCode = exitCode;
        this.currentTestDescription = currentTest;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public Description getCurrentTestDescription() {
        return currentTestDescription;
    }
}

