package org.pitest.util;

import org.pitest.functional.SideEffect1;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WrappingCommnicationThread {

    private static final Logger LOG = Log.getLogger();

    private final SideEffect1<SafeDataOutputStream> sendInitialData;
    private final ReceiveStrategy                   receive;
    private final ServerSocket socket;
    private FutureTask<WrappingExitCode> future;

    public WrappingCommnicationThread(final ServerSocket socket,
                               final SideEffect1<SafeDataOutputStream> sendInitialData,
                               final ReceiveStrategy receive) {
        this.socket = socket;
        this.sendInitialData = sendInitialData;
        this.receive = receive;
    }

    public void start() throws IOException, InterruptedException {
        this.future = createFuture();
    }

    private FutureTask<WrappingExitCode> createFuture() {
        final FutureTask<WrappingExitCode> newFuture = new FutureTask<>(
                new WrappingExitCodeCallable(this.socket, this.sendInitialData,
                        this.receive));
        final Thread thread = new Thread(newFuture);
        thread.setDaemon(true);
        thread.setName("pit communication");
        thread.start();
        return newFuture;
    }

    public WrappingExitCode waitToFinish() {
        try {
            return this.future.get();
        } catch (final ExecutionException e) {
            LOG.log(Level.WARNING, "Error while watching child process", e);
            return new WrappingExitCode(ExitCode.UNKNOWN_ERROR, null);
        } catch (final InterruptedException e) {
            LOG.log(Level.WARNING, "interrupted while waiting for child process", e);
            return new WrappingExitCode(ExitCode.UNKNOWN_ERROR, null);
        }
    }
}
