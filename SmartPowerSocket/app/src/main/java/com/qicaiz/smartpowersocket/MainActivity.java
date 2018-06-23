package com.qicaiz.smartpowersocket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * TCP连接线程
     */
    private ConnectThread mConnectThread;
    /**
     * Socket套接字
     */
    private Socket mSocket;
    /**
     * 输出流
     */
    private PrintStream mPrintStream;
    /**
     * 服务器IP
     */
    private EditText mEtIP;
    /**
     * 服务器端口
     */
    private EditText mEtPort;
    /**
     * 连接按钮
     */
    private Button mBtnConnect;
    /**
     * 打开按钮
     */
    private Button mBtnOn;
    /**
     * 关闭按钮
     */
    private Button mBtnOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mEtIP = (EditText) findViewById(R.id.et_ip);
        mEtPort = (EditText) findViewById(R.id.et_port);
        mBtnOn = (Button) findViewById(R.id.btn_on);
        mBtnOff = (Button) findViewById(R.id.btn_off);
        mBtnConnect.setOnClickListener(this);
        mBtnOn.setOnClickListener(this);
        mBtnOff.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                //连接
                if (mSocket == null || !mSocket.isConnected()) {
                    String ip = mEtIP.getText().toString();
                    int port = Integer.valueOf(mEtPort.getText().toString());
                    mConnectThread = new ConnectThread(ip, port);
                    mConnectThread.start();
                }
                //断开
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        mSocket.close();
                        mBtnConnect.setText("连接");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                break;
            case R.id.btn_on:
                if (mPrintStream != null) {
                    mPrintStream.print("1");
                    mPrintStream.flush();
                }
                break;
            case R.id.btn_off:
                if (mPrintStream != null) {
                    mPrintStream.print("2");
                    mPrintStream.flush();
                }
                break;

        }
    }

    private class ConnectThread extends Thread {
        private String ip;
        private int port;

        public ConnectThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                mSocket = new Socket(ip, port);
                mPrintStream = new PrintStream(mSocket.getOutputStream());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConnect.setText("断开");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
