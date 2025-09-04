package com.android.shell2.services;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketIME extends InputMethodService {
    private OutputStream mOutputStream = null;
    private DataOutputStream mWriter = null;
    private Thread mServerThread = null;
    private NetworkThread mNetworkThread = null;
    private ServerSocket serverSocket = null;
    private static Socket socket = null;

    // 添加一个标志来跟踪服务是否已初始化
    private boolean isServiceInitialized = false;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate(); // 确保调用父类方法
        Log.d("tag", "SocketIME onCreate");
        mHandler = new Handler();
        // 初始化服务标志
        isServiceInitialized = true;
        // 启动网络线程
        mNetworkThread = new NetworkThread();
        mNetworkThread.start();
        // 启动Socket服务器线程
        startServerThread();
    }

    private void startServerThread() {
        if (mServerThread != null && mServerThread.isAlive()) {
            mServerThread.interrupt();
        }

        mServerThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8888);
                Log.d("tag", "SocketIME >>>>>>ServerThread connect success<<<<<<");
                var buffer = new byte[1024];
                while (true) {
                    socket = serverSocket.accept();
                    Log.d("tag", "SocketIME >>>>>>Client connect success<<<<<<");
                    mOutputStream = socket.getOutputStream();
                    mWriter = new DataOutputStream(mOutputStream);

                    // 设置网络线程的Socket
                    mNetworkThread.setSocket(socket);
                    var inputStream = socket.getInputStream();
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        if (read > 0) {
                            Log.d("tag", "SocketIME >>>>>>read data<<<<<<" + new String(buffer, 0, read));
                            handleReceivedData(buffer, read);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("tag", "SocketIME server error", e);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToNextInputMethod(false);
                }
            }
        });

        mServerThread.start(); // 确保线程启动
    }

    private void handleReceivedData(byte[] buffer, int length) {
        // 在主线程中处理UI更新
        if (mHandler != null) {
            try {
                mHandler.post(() -> {
                    String text = new String(buffer, 0, length, StandardCharsets.UTF_8);
                    Log.e("tag", "handleReceivedData=" + text);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(text, 1);
                    }
                });
            } catch (Exception e) {
                Log.e("tag", "Error handling received data", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceInitialized = false;

        // 停止网络线程
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
                // 使用网络线程发送数据
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
                // 使用网络线程发送停止消息，避免在主线程中执行网络操作
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