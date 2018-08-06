package daxiniot.bttemphumidity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final static int ACTIVITY_REQUEST_CODE = 0x01;
    /**
     * 图表控件
     */
    private LineChart mLineChart;
    /**
     * 图表数据集
     */
    private LineDataSet mLineDataSet;
    /**
     * 图表数据
     */
    private LineData mLineData;
    /**
     * 手机蓝牙适配器
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * 蓝牙通信socket
     */
    private BluetoothSocket mSocket;
    /**
     * 蓝牙设备集合
     */
    private List<MyDevice> mDevices;
    /**
     * 设备列表控件适配器
     */
    private ArrayAdapter<MyDevice> mAdapter;
    /**
     * 蓝牙设备列表对话框
     */
    private AlertDialog mDeviceListDialog;
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

    int xIndex = 8;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothUtil.READ_DATA:
                    String data = (String) msg.obj;
                    //5b 00 1e 06 7f
                    Log.i("temperature", "handleMessage: " + data);
                    Log.i("temperature", "handleMessage: " + str2HexStr(data));
                    String hexData = str2HexStr(data);
                    String humidityTemp = hexData.substring(0, 2);
                    String temperatureTemp = hexData.substring(4, 6);
                    Log.i("temperature", "humidityTemp: " + humidityTemp + "  temperatureTemp:" + temperatureTemp);
                    int humidity = Character.digit(humidityTemp.charAt(0), 16) * 16
                            + Character.digit(humidityTemp.charAt(1), 16);
                    Log.i("temperature", "humidityTemp: " + humidity);

                    int temperature = Character.digit(temperatureTemp.charAt(0), 16) * 16
                            + Character.digit(temperatureTemp.charAt(1), 16);
                    Log.i("temperature", "temperature: " + temperature);
                    mTemperatureTv.setText(temperature + "℃");
                    mHumidityTv.setText(humidity + "%");
                    //判断温度是否超出告警阈值
                    Log.i("temperature", "temperature alert: " + Integer.valueOf(mTempHigh));
                    Log.i("temperature", "humidity alert: " + Integer.valueOf(mHumidityHigh));
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

    public static String str2HexStr(String origin) {
        byte[] bytes = origin.getBytes();
        String hex = bytesToHexString(bytes);
        return hex;
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

    /**
     * 广播监听器：负责接收搜索到蓝牙的广播
     */
    private BroadcastReceiver mDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //更新UI
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                boolean bonded = false;
                Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
                //检查设备是否已配对
                for (BluetoothDevice tempDevice : bondedDevices) {
                    if (tempDevice.getAddress().equals(address)) {
                        bonded = true;
                    }
                }
                //刷新设备显示列表
                MyDevice myDevice = new MyDevice();
                myDevice.setName(name);
                myDevice.setAddress(address);
                myDevice.setBonded(bonded);
                mDevices.add(myDevice);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTemperatureTv = (TextView) findViewById(R.id.tv_temperature);
        mHumidityTv = (TextView) findViewById(R.id.tv_humidity);
        mTemperatureAlert = (TextView) findViewById(R.id.tv_temperature_alert);
        mHumidityAlert = (TextView) findViewById(R.id.tv_humidity_alert);
        mTemperatureRangeTv = (TextView) findViewById(R.id.tv_temperature_range);
        getmHumidityRangeTv = (TextView) findViewById(R.id.tv_humidity_range);
        mTemperatureRangeTv.setText("("+mTempLow+" - "+mTempHigh+"℃)");
        getmHumidityRangeTv.setText("("+mHumidityLow+" - "+mHumidityHigh+"%)");
        mLineChart = (LineChart) findViewById(R.id.chart);
        //初始化图表属性
        initChart();
        //初始化蓝牙设备列表对话框
        initDeviceDialog();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //判断手机是否有蓝牙
        if (!BluetoothUtil.isBluetoothSupported()) {
            Toast.makeText(MainActivity.this, "手机上没有蓝牙，应用退出", Toast.LENGTH_SHORT).show();
            finish();
        }
        //注册设备发现广播接收器
        BluetoothUtil.registerDeviceFoundReceiver(mDeviceFoundReceiver, MainActivity.this);

    }

    /**
     * 初始化蓝牙设备列表对话框
     */
    private void initDeviceDialog() {
        View DialogView = getLayoutInflater().inflate(R.layout.dialog_scan_device, null);
        mDeviceListDialog = new AlertDialog.Builder(MainActivity.this).setView(DialogView).create();
        Button cancelScanBtn = (Button) DialogView.findViewById(R.id.btn_cancel_scan);
        cancelScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeviceListDialog.dismiss();
            }
        });
        ListView deviceListView = (ListView) DialogView.findViewById(R.id.lvw_devices);
        //初始化蓝牙列表数据
        mDevices = new ArrayList<>();
        mAdapter = new ArrayAdapter<MyDevice>(MainActivity.this,
                android.R.layout.simple_list_item_1, mDevices);
        deviceListView.setAdapter(mAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mDeviceListDialog.dismiss();
                final String address = mDevices.get(i).getAddress();
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("正在连接...");
                progressDialog.show();
                BluetoothUtil.connectDevice(address, new ConnectCallback() {
                    @Override
                    public void onSuccess(BluetoothSocket socket) {
                        progressDialog.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                            }
                        });
                        mSocket = socket;
                        //手机每隔一秒向单片机发送数据请求
                        BluetoothUtil.writeData(socket, "1");
                        BluetoothUtil.readData(socket, mHandler, BluetoothUtil.READ_DATA);
                    }

                    @Override
                    public void onFailure() {
                        progressDialog.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "连接失败，请重连", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
            }
        });
    }

    LineDataSet mTemperatureDataSet;
    LineDataSet mHumidityDataSet;

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

        int count = 8;

        // 温度数据
        float[] datas1 = {0, 1, 3, 7, 5, 3, 8, 5};
        ArrayList<Entry> temperatureList = new ArrayList<Entry>();
        temperatureList.add(new Entry(0, 0));
//        for (int i = 0; i < count; i++) {
//            temperatureList.add(new Entry(i, datas1[i]));
//        }
        // y轴的数据
        float[] datas2 = {0, 10, 30, 70, 50, 30, 80, 50};
        ArrayList<Entry> humidityList = new ArrayList<Entry>();
        humidityList.add(new Entry(0, 0));
//        for (int i = 0; i < count; i++) {
//            humidityList.add(new Entry(i, datas2[i]));
//        }

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
            case R.id.scan:
                //判断蓝牙是否已经打开
                if (!mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "请先打开蓝牙", Toast.LENGTH_SHORT).show();
                    return true;
                }
                //检查定位权限是否已经开启
                if (!BluetoothUtil.isLocationPermissionEnabled(MainActivity.this)) {
                    BluetoothUtil.requestLocationPermission(MainActivity.this);
                    return true;
                }
                //开始扫描设备
                startDiscoveryDevice();
                return true;
            case R.id.alert_settings:
                Intent intent = new Intent(MainActivity.this, AlertSettingActivity.class);
                intent.putExtra("tempHighSetting", mTempHigh);
                intent.putExtra("tempLowSetting", mTempLow);
                intent.putExtra("humidityHighSetting", mHumidityHigh);
                intent.putExtra("humidityLowSetting", mHumidityLow);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                break;
            case R.id.disconnect:
                if (mSocket != null) {
                    try {
                        mSocket.close();
                        mTemperatureTv.setText("__℃");
                        mHumidityTv.setText("__℃");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            mTempHigh = data.getStringExtra("tempHighSetting");
            mTempLow = data.getStringExtra("tempLowSetting");
            mHumidityHigh = data.getStringExtra("humidityHighSetting");
            mHumidityLow = data.getStringExtra("humidityLowSetting");
            mTemperatureRangeTv.setText("("+mTempLow+" - "+mTempHigh+"℃)");
            getmHumidityRangeTv.setText("("+mHumidityLow+" - "+mHumidityHigh+"%)");
        }
    }

    /**
     * 开始扫描蓝牙设备
     */
    private void startDiscoveryDevice() {
        mDevices.clear();
        mAdapter.notifyDataSetChanged();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        mDeviceListDialog.show();
    }

    /**
     * 再按一次退出程序：如果用户在2秒之内连续点击返回键则退出应用
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                //关闭蓝牙socket
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //退出应用
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //退出应用时反注册设备监听器
        unregisterReceiver(mDeviceFoundReceiver);
    }

}
