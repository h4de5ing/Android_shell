package com.android.shell2.services;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketIME extends InputMethodService {
    private Thread mServerThread = null;
    private NetworkThread mNetworkThread = null;
    private ServerSocket serverSocket = null;

    private boolean isServiceInitialized = false;
    private Handler mHandler;
    private final boolean isServerRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("tag", "SocketIME onCreate");
        mHandler = new Handler();
        isServiceInitialized = true;
        mNetworkThread = new NetworkThread();
        mNetworkThread.start();
        startServerThread();
    }

    private void startServerThread() {
        if (mServerThread != null && mServerThread.isAlive()) {
            mServerThread.interrupt();
        }

        mServerThread = new Thread(() -> {
            while (isServerRunning) {
                try (ServerSocket serverSocket = new ServerSocket(8888)) {
                    Log.d("tag", "SocketIME >>>>>>ServerThread started on port 8888<<<<<<");
                    while (isServerRunning) {
                        Socket clientSocket = serverSocket.accept();
                        Log.d("tag", "SocketIME >>>>>>Client connected<<<<<<");
                        new Thread(() -> handleClient(clientSocket)).start();
                    }
                } catch (Exception e) {
                    Log.e("tag", "Server socket error, will retry in 3s", e);
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }
        });
        mServerThread.start();
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket; var inputStream = clientSocket.getInputStream()) {
            mNetworkThread.setSocket(clientSocket);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                if (read > 0) {
                    String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    Log.d("tag", "SocketIME received: " + text);
                    mHandler.post(() -> {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) ic.commitText(text, 1);
                    });
                }
            }
        } catch (Exception e) {
            Log.e("tag", "Client handler error", e);
        } finally {
            Log.d("tag", "Client disconnected");
            mNetworkThread.setSocket(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceInitialized = false;
        if (mNetworkThread != null) {
            mNetworkThread.shutdown();
            mNetworkThread = null;
        }

        if (mServerThread != null && mServerThread.isAlive()) {
            mServerThread.interrupt();
            mServerThread = null;
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception e) {
            Log.e("tag", "Error closing server socket", e);
        }
    }

    private static final int MESSAGE_TYPE_START = 0;
    private static final int MESSAGE_TYPE_UPDATE_CURSOR = 1;
    private static final int MESSAGE_TYPE_STOP = 2;

    @Override
    public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        if (isServiceInitialized && mNetworkThread != null) {
            try {
                mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, MESSAGE_TYPE_START));
                mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, info.inputType));

                CharSequence initialSelectedText = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    initialSelectedText = info.getInitialSelectedText(0);
                }

                if (initialSelectedText != null) {
                    var buffer = initialSelectedText.toString().getBytes(StandardCharsets.UTF_8);
                    mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_BYTES, buffer));
                }

                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE | InputConnection.CURSOR_UPDATE_MONITOR);
                }
            } catch (Exception e) {
                Log.e("tag", "Error in onStartInput", e);
            }
        }
    }

    @Override
    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo);
        if (isServiceInitialized && mNetworkThread != null) {
            try {
                mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, MESSAGE_TYPE_UPDATE_CURSOR));
                var selectionStart = cursorAnchorInfo.getSelectionStart();
                var rect = cursorAnchorInfo.getCharacterBounds(selectionStart);
                if (rect != null) {
                    mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, (int) rect.top));
                    mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, (int) rect.bottom));
                    mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, (int) rect.left));
                    mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, (int) rect.right));
                }
            } catch (Exception e) {
                Log.e("tag", "Error in onUpdateCursorAnchorInfo", e);
            }
        }
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        if (isServiceInitialized && mNetworkThread != null) {
            try {
                mNetworkThread.addTask(new NetworkThread.NetworkTask(NetworkThread.NetworkTask.TYPE_WRITE_INT, MESSAGE_TYPE_STOP));
            } catch (Exception e) {
                Log.e("tag", "Error in onFinishInput", e);
            }
        }
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
    }
}