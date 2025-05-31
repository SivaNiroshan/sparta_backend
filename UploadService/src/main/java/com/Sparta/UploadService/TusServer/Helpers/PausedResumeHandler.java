package com.Sparta.UploadService.TusServer.Helpers;

import org.springframework.stereotype.Component;

@Component
public class PausedResumeHandler {
    private volatile boolean paused = false;
    private long pauseStartTime;
    private Thread processingThread;

    public synchronized void pause() {
        this.paused = true;
        this.pauseStartTime = System.currentTimeMillis();
    }

    public synchronized void resume() {
        this.paused = false;
        notifyAll(); // Wakes up the thread waiting in `processPatch`
        if (processingThread != null) {
            processingThread.interrupt(); // In case it's sleeping or waiting
        }
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized long getPausedDurationMillis() {
        return System.currentTimeMillis() - pauseStartTime;
    }

    public void setProcessingThread(Thread thread) {
        this.processingThread = thread;
    }
}

