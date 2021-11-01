package org.pitest.mutationtest.execute;

import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.testapi.Description;
import org.pitest.util.ExitCode;
import org.pitest.util.Id;
import org.pitest.util.SafeDataOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class CurrentTestReporter implements Reporter {

    private final SafeDataOutputStream w;

    CurrentTestReporter(final OutputStream w) {
        this.w = new SafeDataOutputStream(w);
    }

    @Override
    public synchronized void describe(final MutationIdentifier i)
            throws IOException {
        this.w.writeByte(Id.DESCRIBE);
        this.w.write(i);
        this.w.flush();
    }

    @Override
    public synchronized void report(final MutationIdentifier i,
                                    final MutationStatusTestPair mutationDetected) throws IOException {
        this.w.writeByte(Id.REPORT);
        this.w.write(i);
        this.w.write(mutationDetected);
        this.w.flush();
    }

    @Override
    public synchronized void done(final ExitCode exitCode) {
        this.w.writeByte(Id.DONE);
        this.w.writeInt(exitCode.getCode());
        this.w.flush();
    }

    public synchronized void done(final ExitCode exitCode, Description curDescription) {
        this.w.writeByte(Id.DONE);
        if (exitCode == ExitCode.TIMEOUT && curDescription != null){
            this.w.writeString("" + exitCode.getCode() + "@" + curDescription.getFirstTestClass() + "#" + curDescription.getName());
        } else {
            this.w.writeString("" + exitCode.getCode() + "@#");
        }
        this.w.flush();
    }

}
