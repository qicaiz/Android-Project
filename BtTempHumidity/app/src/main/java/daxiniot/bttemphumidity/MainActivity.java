package daxiniot.bttemphumidity;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLineChart = (LineChart) findViewById(R.id.chart);
        //初始化图表属性
        initChart();
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
        //隐藏右侧的Y轴
        yAxisRight.setEnabled(false);
        //设置图表数据
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 0));
        entries.add(new Entry(1, 1));
        entries.add(new Entry(2, 3));
        entries.add(new Entry(3, 7));
        entries.add(new Entry(4, 5));
        entries.add(new Entry(5, 3));
        entries.add(new Entry(6, 8));
        mLineDataSet = new LineDataSet(entries, "temperature");
        mLineData = new LineData(mLineDataSet);
        mLineChart.setData(mLineData);
        mLineChart.invalidate();
    }

}
