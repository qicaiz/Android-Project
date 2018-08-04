package daxiniot.bttemphumidity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AlertSettingActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 取消按钮
     */
    private Button mCancelBtn;
    /**
     * 保存按钮
     */
    private Button mSaveBtn;
    /**
     * 温度上限
     */
    private EditText mTempHigh;
    /**
     * 温度下限
     */
    private EditText mTempLow;
    /**
     * 湿度上限
     */
    private EditText mHumidityHigh;
    /**
     * 湿度下限
     */
    private EditText mHumidityLow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_setting);
        mCancelBtn = (Button) findViewById(R.id.btn_cancel);
        mSaveBtn = (Button) findViewById(R.id.btn_save);
        mTempHigh = (EditText) findViewById(R.id.et_temp_high);
        mTempLow = (EditText) findViewById(R.id.et_temp_low);
        mHumidityHigh = (EditText) findViewById(R.id.et_humidity_high);
        mHumidityLow = (EditText) findViewById(R.id.et_humidity_low);

        mCancelBtn.setOnClickListener(this);
        mSaveBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case R.id.btn_save:
                Intent settingIntent = new Intent();
                settingIntent.putExtra("tempHighSetting",mTempHigh.getText().toString());
                settingIntent.putExtra("tempLowSetting",mTempLow.getText().toString());
                settingIntent.putExtra("humidityHighSetting",mHumidityHigh.getText().toString());
                settingIntent.putExtra("humidityLowSetting",mHumidityLow.getText().toString());
                setResult(RESULT_OK, settingIntent);
                finish();
                break;
        }
    }
}
