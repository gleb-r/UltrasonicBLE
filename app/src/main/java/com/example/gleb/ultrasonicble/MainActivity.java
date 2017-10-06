package com.example.gleb.ultrasonicble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.robinhood.spark.SparkAdapter;
import com.robinhood.spark.SparkView;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String DEVICE_NAME = "Curie";
    public static final String DEVICE_ADDRESS = "98:4F:EE:10:6C:4A";
    public static final int NUM_GRAPH_POINTS = 200;

    public static final UUID heightCharacteristicUUID = UUID.fromString("00002ab3-0000-1000-8000-00805f9b34fb");
    public static final UUID batteryCharacteristicUUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private TextView mTextViewConnectionStatus;
    private TextView mTextViewHeight;


    private BluetoothLeService mBluetoothLeService;
    private boolean mServiceConnected = false;
    private boolean mConnected = false;
    private final String TAG = "UltasonicBle.Main";
    private List<BluetoothGattService> mGattServices;
    private List<BluetoothGattCharacteristic> mGattCharacteristics;
    private List<UltraHeight> lastData;
    private UltraHeightSingleton mData;
    private BluetoothGattCharacteristic batteryCharacteristic;

    private MySparkAdapter mSparkAdapter;
    private TextView tvZeroLevel;
    private int mZeroLevel;
    private OdometerService odometer;
    private boolean isLocationServiceBound = false;
    private float lastSpeed = 0;
    private TextView tvSpeed;


    private ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "could't initialize BleService!");
                finish();
            }
            mServiceConnected = true;
            Log.i(TAG, "Ble service initialazed");
            mBluetoothLeService.connect(DEVICE_ADDRESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceConnected = false;
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;

        }
    };

    private ServiceConnection mLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            isLocationServiceBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isLocationServiceBound = false;

        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.w(TAG, "Null action");
                return;
            }
            switch (action) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    updateConnectionState(mConnected);
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    updateConnectionState(mConnected);
                    break;
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    setBleNotificationsOn();
                    break;
                case BluetoothLeService.ACTION_HEIGHT_DATA_AVAILABLE:
                    updateHeightValue(intent);
                    break;
                case BluetoothLeService.ACTION_BATTERY_DATA_AVAILABLE:
                    updateBatteryValue(intent);
                    break;
                default:
                    Log.w(TAG, "found unknown action: " + action);
                    break;
            }
        }
    };


    private void setBleNotificationsOn() {
        mGattServices = mBluetoothLeService.getSupportedGattServices();
        Log.i(TAG, String.valueOf(mGattServices.size()) + "services");
        for (BluetoothGattService service : mGattServices) {
            Log.i(TAG, "Service " + service.getUuid().toString());
            mGattCharacteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : mGattCharacteristics) {
                Log.i(TAG, "Got a characteristic: " + characteristic.getUuid().toString());
                if (characteristic.getUuid().equals(heightCharacteristicUUID)) {
                    Log.i(TAG, "HeightCharacteristic Found!");
                    mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    break;
                } else if (characteristic.getUuid().equals(batteryCharacteristicUUID)) {
                    Log.w(TAG, "BatteryCharacteristic Found!");
                    batteryCharacteristic = characteristic;
                    // БУБЕН! Если включать уведомление без задержки, то не срабатывает
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothLeService.setCharacteristicNotification(batteryCharacteristic,true);
                        }
                    },1000);

                }
            }
        }
    }

    private void updateHeightValue(Intent intent) {
        float height = intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0);
        if (lastData.size() > NUM_GRAPH_POINTS) {
            lastData.remove(0);
        }
        lastData.add(new UltraHeight(height, lastSpeed));
        mData.addItem(height, lastSpeed);
        mTextViewHeight.setText(String.valueOf((int) height - mZeroLevel));
        mSparkAdapter.notifyDataSetChanged();
    }

    private void updateBatteryValue(Intent intent) {
        int batteryLevel = intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0);
        TextView tvBatteryLevel = findViewById(R.id.tv_battery_level);
        tvBatteryLevel.setText(String.valueOf(batteryLevel) + "%");
        //Log.i(TAG, "Got a new battery level = " + batteryLevel);
    }


    private void updateConnectionState(final boolean isConnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    mTextViewConnectionStatus.setText(R.string.connected);
                    invalidateOptionsMenu();
                    watchSpeed();
                } else {
                    mTextViewConnectionStatus.setText(R.string.disconnected);
                    invalidateOptionsMenu();
                    if (isLocationServiceBound) {
                        unbindService(mLocationServiceConnection);
                        isLocationServiceBound = false;
                        tvSpeed.setText("---");
                    }
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mBleServiceConnection, BIND_AUTO_CREATE);
        Intent locationServiceIntent = new Intent(this, OdometerService.class);
        bindService(locationServiceIntent, mLocationServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewConnectionStatus = findViewById(R.id.tv_connectionStatus);
        mTextViewHeight = findViewById(R.id.tv_height);
        mData = UltraHeightSingleton.get(this);
        lastData = mData.getLastData(NUM_GRAPH_POINTS);
        Log.i(TAG, "Size of lastData = " + lastData.size());
        SparkView sparkView = findViewById(R.id.sv_height_graph);
        mSparkAdapter = new MySparkAdapter(lastData);
        sparkView.setAdapter(mSparkAdapter);
        tvZeroLevel = findViewById(R.id.tv_zero_lvl);
        Button btnZeroLevel = findViewById(R.id.btn_zero_level);
        btnZeroLevel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.setCharacteristicNotification(batteryCharacteristic, true);
                mZeroLevel = 0;
                for (int i = lastData.size() - 10; i < lastData.size(); i++) {
                    mZeroLevel += lastData.get(i).getHeight();
                }
                mZeroLevel /= 10;
                tvZeroLevel.setText(mZeroLevel + " mm");
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.connect);
        if (mConnected) {
            menuItem.setTitle(R.string.disconnect);
            menuItem.setIcon(R.drawable.ic_connected);
        } else {
            menuItem.setTitle(R.string.connect);
            menuItem.setIcon(R.drawable.ic_disconnected);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.connect):
                if (mConnected) {
                    mBluetoothLeService.close();
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService = null;
                    unregisterReceiver(mBroadcastReceiver);
                    unbindService(mBleServiceConnection);

                    Log.i(TAG, "disconnected");
                    mConnected = false;
                    updateConnectionState(mConnected);
                } else {
                    Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
                    bindService(bleServiceIntent, mBleServiceConnection, BIND_AUTO_CREATE);
                }
                return true;
            case (R.id.show_history):
                Intent intent = new Intent(this, DataViewActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, makerIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(DEVICE_ADDRESS);
            Log.i(TAG, "Connect request result=" + result);
        } else {
            Log.i(TAG, "bleService = null");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isLocationServiceBound) {
            unbindService(mLocationServiceConnection);
            isLocationServiceBound = false;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mBleServiceConnection);
        mBluetoothLeService = null;
    }


    private IntentFilter makerIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_HEIGHT_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BATTERY_DATA_AVAILABLE);
        return intentFilter;
    }

    private void watchSpeed() {
        tvSpeed = findViewById(R.id.tv_speed);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (odometer != null) {
                    lastSpeed = odometer.getSpeed();
                    String speedStr = String.format(Locale.US, "%.0f km/h", (lastSpeed * 3.6));
                    tvSpeed.setText(speedStr);
                }
                handler.postDelayed(this, 1000);
            }
        });
    }


    private class MySparkAdapter extends SparkAdapter {

        List<UltraHeight> data;

        @Override
        public RectF getDataBounds() {
            return super.getDataBounds();
        }

        public MySparkAdapter(List<UltraHeight> data) {
            this.data = data;
        }

        @Override
        public boolean hasBaseLine() {
            return true;
        }

        @Override
        public float getBaseLine() {
            return mZeroLevel;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int index) {
            return data.get(index).getHeight();
        }

        @Override
        public float getY(int index) {
            return data.get(index).getHeight();
        }


    }

}
