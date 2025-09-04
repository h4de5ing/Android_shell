package com.android.shell2.services;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkThread extends Thread {
    private static final String TAG = "NetworkThread";
    private volatile boolean isRunning = true;
    private final BlockingQueue<NetworkTask> taskQueue = new LinkedBlockingQueue<>();
    private DataOutputStream mWriter = null;
    private Socket socket = null;

    public void setSocket(Socket socket) {
        this.socket = socket;
        try {
            if (socket != null && socket.isConnected()) {
                OutputStream outputStream = socket.getOutputStream();
                mWriter = new DataOutputStream(outputStream);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating output stream", e);
        }
    }

    public void addTask(NetworkTask task) {
        if (isRunning) {
            taskQueue.offer(task);
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                NetworkTask task = taskQueue.take();
                if (mWriter != null && socket != null && socket.isConnected()) {
                    executeTask(task);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Network thread interrupted");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error in network thread", e);
            }
        }

        // 清理资源
        try {
            if (mWriter != null) {
                mWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing resources", e);
        }
    }

    private void executeTask(NetworkTask task) {
        try {
            switch (task.type) {
                case NetworkTask.TYPE_WRITE_INT:
                    mWriter.writeInt(task.intValue);
                    break;
                case NetworkTask.TYPE_WRITE_BYTES:
                    mWriter.writeInt(task.bytes.length);
                    mWriter.write(task.bytes, 0, task.bytes.length);
                    break;
            }
            mWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error executing network task", e);
        }
    }

    public void shutdown() {
        isRunning = false;
        interrupt();
    }

    public static class NetworkTask {
        public static final int TYPE_WRITE_INT = 1;
        public static final int TYPE_WRITE_BYTES = 2;

        public final int type;
        public final int intValue;
        public final byte[] bytes;

        public NetworkTask(int type, int intValue) {
            this.type = type;
            this.intValue = intValue;
            this.bytes = null;
        }

        public NetworkTask(int type, byte[] bytes) {
            this.type = type;
            this.intValue = 0;
            this.bytes = bytes;
        }
    }
}