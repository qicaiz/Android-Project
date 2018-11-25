package com.daxiniot.wifitemphumidity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static int ACTIVITY_REQUEST_CODE = 0x01;
    /**
     * 图表控件
     */
    private LineChart mLineChart;
    /**
     * 图表数据
     */
    private LineData mLineData;

    /**
     * 记录推退出时间
     */
    private long mExitTime = 0;

    /**
     * 温度上限
     */
    private String mTempHigh = "32";
    /**
     * 温度下限
     */
    private String mTempLow = "18";
    /**
     * 湿度上限
     */
    private String mHumidityHigh = "92";
    /**
     * 湿度下限
     */
    private String mHumidityLow = "70";
    /**
     * 温度显示控件
     */
    private TextView mTemperatureTv;
    /**
     * 湿度显示控件
     */
    private TextView mHumidityTv;
    /**
     * 温度告警控件
     */
    private TextView mTemperatureAlert;
    /**
     * 湿度告警控件
     */
    private TextView mHumidityAlert;
    /**
     * 温度告警范围
     */
    private TextView mTemperatureRangeTv;
    /**
     * 湿度告警范围
     */
    private TextView getmHumidityRangeTv;

    private LineDataSet mTemperatureDataSet;
    private LineDataSet mHumidityDataSet;

    private int xIndex = 8;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    String data = (String) msg.obj;
                    //将字符转换成16进制的字符串共5个字节 xx xx xx xx xx
                    String hexData = str2HexStr(data);
                    //湿度整数部分,第一个字节
                    String humidityTemp = hexData.substring(0, 2);
                    //温度整数部分，第三个字节
                    String temperatureTemp = hexData.substring(4, 6);
                    //将16进制转换成10进制
                    int humidity = Character.digit(humidityTemp.charAt(0), 16) * 16
                            + Character.digit(humidityTemp.charAt(1), 16);
                    int temperature = Character.digit(temperatureTemp.charAt(0), 16) * 16
                            + Character.digit(temperatureTemp.charAt(1), 16);
                    mTemperatureTv.setText(temperature + "℃");
                    mHumidityTv.setText(humidity + "%");
                    //判断温度是否超出告警阈值
                    if (temperature > Integer.valueOf(mTempHigh)) {
                        mTemperatureAlert.setText("温度过高");
                        mTemperatureAlert.setTextColor(Color.RED);
                    } else if (temperature < Integer.valueOf(mTempLow)) {
                        mTemperatureAlert.setText("温度偏低");
                        mTemperatureAlert.setTextColor(Color.RED);
                    } else {
                        mTemperatureAlert.setText("温度正常");
                        mTemperatureAlert.setTextColor(Color.GREEN);
                    }
                    //判断湿度是否超出告警阈值
                    if (humidity > Integer.valueOf(mHumidityHigh)) {
                        mHumidityAlert.setText("湿度过高");
                        mHumidityAlert.setTextColor(Color.RED);
                    } else if (humidity < Integer.valueOf(mHumidityLow)) {
                        mHumidityAlert.setText("湿度偏低");
                        mHumidityAlert.setTextColor(Color.RED);
                    } else {
                        mHumidityAlert.setText("湿度正常");
                        mHumidityAlert.setTextColor(Color.GREEN);
                    }
                    //刷新图表
                    int entryCount = mTemperatureDataSet.getEntryCount();
                    if (entryCount < 8) {
                        ++entryCount;
                        int tempEntryCount = entryCount;
                        mTemperatureDataSet.addEntry(new Entry(tempEntryCount, temperature));
                        mHumidityDataSet.addEntry(new Entry(tempEntryCount, humidity));
                    } else {
                        ++xIndex;
                        int tempIndex = xIndex;
                        mTemperatureDataSet.addEntry(new Entry(tempIndex, temperature));
                        mTemperatureDataSet.removeFirst();
                        mHumidityDataSet.addEntry(new Entry(tempIndex, humidity));
                        mHumidityDataSet.removeFirst();
                    }
                    mLineData.notifyDataChanged();
                    mLineChart.notifyDataSetChanged();
                    mLineChart.invalidate();
                    break;
                default:
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //实例化控件
        mTemperatureTv = (TextView) findViewById(R.id.tv_temperature);
        mHumidityTv = (TextView) findViewById(R.id.tv_humidity);
        mTemperatureAlert = (TextView) findViewById(R.id.tv_temperature_alert);
        mHumidityAlert = (TextView) findViewById(R.id.tv_humidity_alert);
        mTemperatureRangeTv = (TextView) findViewById(R.id.tv_temperature_range);
        getmHumidityRangeTv = (TextView) findViewById(R.id.tv_humidity_range);
        mTemperatureRangeTv.setText("(" + mTempLow + " - " + mTempHigh + "℃)");
        getmHumidityRangeTv.setText("(" + mHumidityLow + " - " + mHumidityHigh + "%)");
        mLineChart = (LineChart) findViewById(R.id.chart);
        //初始化图表属性
        initChart();
        //开启温度显示线程
        new ShowTemperatureThread().start();

    }

    /**
     * 初始化图表属性
     */
    private void initChart() {
        //图表不可拖拽
        mLineChart.setDragEnabled(false);
        //设置X轴属性
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        //设置Y轴属性
        YAxis yAxisLeft = mLineChart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        YAxis yAxisRight = mLineChart.getAxisRight();
        yAxisRight.setTextColor(Color.WHITE);
        //设置图表数据
        ArrayList<Entry> temperatureList = new ArrayList<>();
        temperatureList.add(new Entry(0, 0));
        // y轴的数据
        ArrayList<Entry> humidityList = new ArrayList<>();
        humidityList.add(new Entry(0, 0));
        mTemperatureDataSet = new LineDataSet(temperatureList, "温度");
        //dataSet.enableDashedLine(10f, 10f, 0f);//将折线设置为曲线(即设置为虚线)
        //用y轴的集合来设置参数
        mTemperatureDataSet.setLineWidth(1.75f); // 线宽
        mTemperatureDataSet.setCircleRadius(2f);// 显示的圆形大小
        mTemperatureDataSet.setColor(Color.rgb(89, 194, 230));// 折线显示颜色
        mTemperatureDataSet.setCircleColor(Color.rgb(89, 194, 230));// 圆形折点的颜色
        mTemperatureDataSet.setHighLightColor(Color.GREEN); // 高亮的线的颜色
        mTemperatureDataSet.setHighlightEnabled(true);
        mTemperatureDataSet.setValueTextColor(Color.rgb(89, 194, 230)); //数值显示的颜色
        mTemperatureDataSet.setValueTextSize(8f);     //数值显示的大小

        mHumidityDataSet = new LineDataSet(humidityList, "湿度");
        //用y轴的集合来设置参数
        mHumidityDataSet.setLineWidth(1.75f);
        mHumidityDataSet.setCircleRadius(2f);
        mHumidityDataSet.setColor(Color.rgb(252, 76, 122));
        mHumidityDataSet.setCircleColor(Color.rgb(252, 76, 122));
        mHumidityDataSet.setHighLightColor(Color.GREEN);
        mHumidityDataSet.setHighlightEnabled(true);
        mHumidityDataSet.setValueTextColor(Color.rgb(252, 76, 122));
        mHumidityDataSet.setValueTextSize(8f);
        //构建一个类型为LineDataSet的ArrayList 用来存放所有 y的LineDataSet   他是构建最终加入LineChart数据集所需要的参数
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        //将数据加入dataSets
        dataSets.add(mTemperatureDataSet);
        dataSets.add(mHumidityDataSet);
        //构建一个LineData  将dataSets放入
        mLineData = new LineData(dataSets);
        mLineChart.setData(mLineData);
        mLineChart.invalidate();
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
                    receive = new byte[5];
                    in.read(receive);
                    String temp = new String(receive);
                    Message message = new Message();
                    message.what = 0x01;
                    message.obj = temp;
                    mHandler.sendMessage(message);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //退出应用
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 菜单点击事件回调方法
     *
     * @param item 被点击的菜单项
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.alert_settings:
                Intent intent = new Intent(MainActivity.this, AlertSettingActivity.class);
                intent.putExtra(Constants.TEMP_HIGH_SETTING, mTempHigh);
                intent.putExtra(Constants.TEMP_LOW_SETTING, mTempLow);
                intent.putExtra(Constants.HUMIDITY_HIGH_SETTING, mHumidityHigh);
                intent.putExtra(Constants.HUMIDITY_LOW_SETTING, mHumidityLow);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                break;


        }
        return true;
    }

    /**
     * 将字符串转换成16进制字符串
     *
     * @param origin
     * @return
     */
    public static String str2HexStr(String origin) {
        byte[] bytes = origin.getBytes();
        return bytesToHexString(bytes);
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


}