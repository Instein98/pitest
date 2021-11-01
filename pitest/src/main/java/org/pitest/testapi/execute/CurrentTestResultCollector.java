package org.pitest.testapi.execute;

import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;

public class CurrentTestResultCollector  implements ResultCollector {

    private final ResultCollector child;
    private boolean               hadFailure = false;

    private Description curDescription;

    public Description getCurDescription() {
        return curDescription;
    }

    public CurrentTestResultCollector(final ResultCollector child) {
        this.child = child;
    }

    @Override
    public void notifySkipped(final Description description) {
        this.child.notifySkipped(description);
    }

    @Override
    public void notifyStart(final Description description) {
        this.child.notifyStart(description);
        curDescription = description;
    }

    @Override
    public boolean shouldExit() {
        return this.hadFailure;
    }

    @Override
    public void notifyEnd(final Description description, final Throwable t) {
        this.child.notifyEnd(description, t);
        if (t != null) {
            this.hadFailure = true;
        }

    }

    @Override
    public void notifyEnd(final Description description) {
        this.child.notifyEnd(description);
    }

}