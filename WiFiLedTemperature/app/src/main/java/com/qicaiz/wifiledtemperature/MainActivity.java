package com.qicaiz.wifiledtemperature;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
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
     * 红色LED灯开按钮
     */
    private Button mBtnRedOn;
    /**
     * 红色LED灯关按钮
     */
    private Button mBtnRedOff;
    /**
     * 黄色LED灯开按钮
     */
    private Button mBtnYellowOn;
    /**
     * 黄色LED灯关按钮
     */
    private Button mBtnYellowOff;
    /**
     * 蓝色LED灯开按钮
     */
    private Button mBtnBlueOn;
    /**
     * 蓝色LED灯关按钮
     */
    private Button mBtnBlueOff;
    /**
     * 显示温度按钮
     */
    private Button mBtnShowTemperature;
    /**
     * 显示温度文本
     */
    private TextView mTvTemperature;
    /**
     * 输出流
     */
    private PrintStream mPrintStream;

    /**
     * 心跳线程
     */
    private HeartBeatThread mHeartBeatThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //控件初始化
        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mEtIP = (EditText) findViewById(R.id.et_ip);
        mEtPort = (EditText) findViewById(R.id.et_port);
        mBtnRedOn = (Button) findViewById(R.id.btn_red_on);
        mBtnRedOff = (Button) findViewById(R.id.btn_red_off);
        mBtnYellowOn = (Button) findViewById(R.id.btn_yellow_on);
        mBtnYellowOff = (Button) findViewById(R.id.btn_yellow_off);
        mBtnBlueOn = (Button) findViewById(R.id.btn_blue_on);
        mBtnBlueOff = (Button) findViewById(R.id.btn_blue_off);
        mBtnShowTemperature = (Button) findViewById(R.id.btn_show_temperature);
        mTvTemperature = (TextView) findViewById(R.id.txt_temperature);
        //绑定点击回调事件
        mBtnConnect.setOnClickListener(this);
        mBtnRedOn.setOnClickListener(this);
        mBtnRedOff.setOnClickListener(this);
        mBtnYellowOn.setOnClickListener(this);
        mBtnYellowOff.setOnClickListener(this);
        mBtnBlueOn.setOnClickListener(this);
        mBtnBlueOff.setOnClickListener(this);
        mBtnShowTemperature.setOnClickListener(this);
        //开启温度显示线程
        new ShowTemperatureThread().start();
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
                //断开连接并停止温度数据请求
                if (mSocket != null && mSocket.isConnected()) {
                    if (mPrintStream != null) {
                        mPrintStream.print("s");
                        mPrintStream.flush();
                    }
                    try {
                        mSocket.close();
                        mBtnConnect.setText("连接");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            //打开红灯
            case R.id.btn_red_on:
                if (mPrintStream != null) {
                    mPrintStream.print("3");
                    mPrintStream.flush();
                }
                break;
            //关闭红灯
            case R.id.btn_red_off:
                if (mPrintStream != null) {
                    mPrintStream.print("4");
                    mPrintStream.flush();
                }
                break;
            //打开黄灯
            case R.id.btn_yellow_on:
                if (mPrintStream != null) {
                    mPrintStream.print("5");
                    mPrintStream.flush();
                }
                break;
            //关闭黄灯
            case R.id.btn_yellow_off:
                if (mPrintStream != null) {
                    mPrintStream.print("6");
                    mPrintStream.flush();
                }
                break;
            //打开蓝灯
            case R.id.btn_blue_on:
                if (mPrintStream != null) {
                    mPrintStream.print("7");
                    mPrintStream.flush();
                }
                break;
            //关闭蓝灯
            case R.id.btn_blue_off:
                if (mPrintStream != null) {
                    mPrintStream.print("8");
                    mPrintStream.flush();
                }
                break;
            //显示温度，每次断开重连后都要点击该按钮
            case R.id.btn_show_temperature:
                if (mPrintStream != null) {
                    mPrintStream.print("t");
                    mPrintStream.flush();
                }
                break;
        }
    }

    /***
     * 连接线程：负责与ESP8266 WiFi进行连接
     */
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
                //建立网络socket，esp8266默认IP： 192.168.4.1 端口：333
                mSocket = new Socket(ip, port);
                mPrintStream = new PrintStream(mSocket.getOutputStream());
                //更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConnect.setText("断开");
                    }
                });
                //启动心跳线程，维持Socket连接
                if (mHeartBeatThread == null) {
                    mHeartBeatThread = new HeartBeatThread();
                    mHeartBeatThread.start();
                }
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

    /**
     * 接收温度数据的线程
     */
    private ServerSocket serverSocket;
    private Socket client;
    private class ShowTemperatureThread extends Thread {
        private DataInputStream in;
        private byte[] receive;

        @Override
        public void run() {
            try {
                //在手机端建立一个ServerSocket，负责获取温度数据，端口为5000
                serverSocket = new ServerSocket(5000);
                client = serverSocket.accept();
                while (true) {
                    //读取温度
                    in = new DataInputStream(client.getInputStream());
                    receive = new byte[4];
                    in.read(receive);
                    //格式化温度并捕获异常
                    try{
                        final double temp = Double.valueOf(new String(receive)) / 100;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTvTemperature.setText(temp + "℃");
                            }
                        });
                    } catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 心跳线程，每隔10s向esp8266发送一个字符，保持连接
     */
    private class HeartBeatThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(10 * 1000);
                    if (mPrintStream != null) {
                        mPrintStream.print("x");
                        mPrintStream.flush();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //按返回键时 停止数据请求，关闭所有socket连接
        if (mSocket != null && mSocket.isConnected()) {
            if (mPrintStream != null) {
                mPrintStream.print("s");
                mPrintStream.flush();
            }
            try {
                mSocket.close();
                mBtnConnect.setText("连接");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(serverSocket!=null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(client!=null){
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //退出应用
        finish();
    }
}
