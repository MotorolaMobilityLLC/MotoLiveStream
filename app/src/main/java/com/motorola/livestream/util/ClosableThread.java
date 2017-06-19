package com.motorola.livestream.util;

public class ClosableThread extends Thread {
    protected boolean bIsRunning = true;

    public void close() {
        bIsRunning = false;
    }

    public boolean isRunning() {
        return bIsRunning;
    }
}
