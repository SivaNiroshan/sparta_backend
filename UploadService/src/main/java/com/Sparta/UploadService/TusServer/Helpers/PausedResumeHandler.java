package com.Sparta.UploadService.TusServer.Helpers;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PausedResumeHandler {
    private final Object pauseLock = new Object(); // final monitor

    private volatile boolean paused = false;
    private volatile boolean flushRequested = false;
    private long pauseStartTime;
    private Thread processingThread;

    public void pause() {
        synchronized (pauseLock) {
            this.paused = true;
            this.flushRequested = true;
            this.pauseStartTime = System.currentTimeMillis();
        }
    }

    public void resume() {
        synchronized (pauseLock) {
            this.paused = false;
            this.flushRequested = false;
            pauseLock.notifyAll(); // safer and clearer
        }

        if (processingThread != null) {
            processingThread.interrupt(); // optional
        }
    }

    public boolean isPaused() {
        synchronized (pauseLock) {
            return paused;
        }
    }

    public boolean isFlushRequested() {
        synchronized (pauseLock) {
            return flushRequested;
        }
    }

    public void resetFlushRequested(boolean reset) {
        synchronized (pauseLock) {
            this.flushRequested = reset;
        }
    }

    public long getPausedDurationMillis() {
        synchronized (pauseLock) {
            return System.currentTimeMillis() - pauseStartTime;
        }
    }



    public void setProcessingThread(Thread thread) {
        this.processingThread = thread;
    }
}
