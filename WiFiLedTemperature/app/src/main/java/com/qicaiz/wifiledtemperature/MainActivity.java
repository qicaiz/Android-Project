package com.qicaiz.wifiledtemperature;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    /**TCP连接线程*/
    private ConnectThread mConnectThread;
    /**Socket套接字*/
    private Socket mSocket;
    /**服务器IP*/
    private EditText mEtIP;
    /**服务器端口*/
    private EditText mEtPort;
    /**连接按钮*/
    private Button mBtnConnect;
    /**红色LED灯开按钮*/
    private Button mBtnRedOn;
    /**红色LED灯关按钮*/
    private Button mBtnRedOff;
    /**黄色LED灯开按钮*/
    private Button mBtnYellowOn;
    /**黄色LED灯关按钮*/
    private Button mBtnYellowOff;
    /**蓝色LED灯开按钮*/
    private Button mBtnBlueOn;
    /**蓝色LED灯关按钮*/
    private Button mBtnBlueOff;
    /**显示温度按钮*/
    private Button mBtnShowTemperature;
    /**显示温度文本*/
    private TextView mTvTemperature;
    /**输出流*/
    private PrintStream mPrintStream;

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
                    if(mPrintStream!=null){
                        mPrintStream.print("8");
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
                    mPrintStream.print("1");
                    mPrintStream.flush();
                }
                break;
            //关闭红灯
            case R.id.btn_red_off:
                if (mPrintStream != null) {
                    mPrintStream.print("2");
                    mPrintStream.flush();
                }
                break;
            //打开黄灯
            case R.id.btn_yellow_on:
                if (mPrintStream != null) {
                    mPrintStream.print("3");
                    mPrintStream.flush();
                }
                break;
            //关闭黄灯
            case R.id.btn_yellow_off:
                if (mPrintStream != null) {
                    mPrintStream.print("4");
                    mPrintStream.flush();
                }
                break;
            //打开蓝灯
            case R.id.btn_blue_on:
                if (mPrintStream != null) {
                    mPrintStream.print("5");
                    mPrintStream.flush();
                }
                break;
            //关闭蓝灯
            case R.id.btn_blue_off:
                if (mPrintStream != null) {
                    mPrintStream.print("6");
                    mPrintStream.flush();
                }
                break;
            //显示温度，每次断开重连后都要点击该按钮
            case R.id.btn_show_temperature:
                if(mPrintStream!=null){
                    mPrintStream.print("7");
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
                mSocket = new Socket(ip,port);
                mPrintStream = new PrintStream(mSocket.getOutputStream());
                //更新UI
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

    /**
     * 接收温度数据的线程
     */
    private class ShowTemperatureThread extends Thread{
        private ServerSocket serverSocket;
        private DataInputStream in;
        private byte[] receive;
        private Socket client;
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(5000);
                client = serverSocket.accept();
                while (true){
                    //读取温度
                    in = new DataInputStream(client.getInputStream());
                    receive = new byte[4];
                    in.read(receive);
                    //格式化温度
                    final double temp = Double.valueOf(new String(receive))/100;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvTemperature.setText(temp+"℃");
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
