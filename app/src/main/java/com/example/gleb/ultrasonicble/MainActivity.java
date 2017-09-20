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
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.robinhood.spark.SparkAdapter;
import com.robinhood.spark.SparkView;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String DEVICE_NAME = "Curie";
    public static final String DEVICE_ADDRESS = "98:4F:EE:10:6C:4A";
    public static final int NUM_GRAPH_POINTS = 200;

    private static final UUID heightCharacteristicUUID = UUID.fromString("00002ab3-0000-1000-8000-00805f9b34fb");

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

    private MySparkAdapter mSparkAdapter;


    private ServiceConnection mServiceConnection = new ServiceConnection() {
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

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothLeService.ACTION_GATT_CONNECTED)) {
                mConnected = true;
                updateConnectionState(mConnected);

            } else if (action.equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)) {
                mConnected = false;
                updateConnectionState(mConnected);




            } else if (action.equals(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mGattServices = mBluetoothLeService.getSupportedGattServices();
                Log.i(TAG, String.valueOf(mGattServices.size()) + "services");
                for (BluetoothGattService service : mGattServices) {
                    Log.i(TAG, "Service " + service.getUuid().toString());
                    mGattCharacteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : mGattCharacteristics) {
                        if (characteristic.getUuid().equals(heightCharacteristicUUID)) {
                            Log.i(TAG, "HeightCharacteristic Found!");
                            mBluetoothLeService.setCharacteristicNotification(characteristic,true);
                            break;
                        }
                    }
                }

            } else if (action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {
                Log.i(TAG, "action data available");
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null) {
                    float height;
                    try {
                        height = Float.valueOf(data);
                        if (lastData.size()>NUM_GRAPH_POINTS) {
                            Log.i(TAG, "Before remove lastData size= "+lastData.size());
                            lastData.remove(0);

                        } else {
                            Log.i(TAG, "lastData size <=  100 ("+lastData.size()+ ")");
                        }
                        Log.i(TAG, "Before add lastData size= "+lastData.size());
                        lastData.add(new UltraHeight(height));
                        mData.addItem(height);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Wrong float to int format " +e);
                    }

                    mTextViewHeight.setText(data);
                    mSparkAdapter.notifyDataSetChanged();
                }
            }

        }
    };




//
//    private void updateConnectionState(final int resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mTextViewConnectionStatus.setText(resourceId);
//            }
//        });
//
//    }


    private void updateConnectionState(final boolean isConnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    mTextViewConnectionStatus.setText(R.string.connected);
                    invalidateOptionsMenu();
                } else {
                    mTextViewConnectionStatus.setText(R.string.disconnected);
                    invalidateOptionsMenu();
                }
            }
        });

    }



    @Override
    protected void onStart() {
        super.onStart();
        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewConnectionStatus = (TextView) findViewById(R.id.tv_connectionStatus);
        mTextViewHeight = (TextView) findViewById(R.id.tv_height);
        mData = UltraHeightSingleton.get(this);
        lastData = mData.getLastData(NUM_GRAPH_POINTS);
        Log.i(TAG, "Size of lastData = "+ lastData.size());
        SparkView sparkView = (SparkView)findViewById(R.id.sv_height_graph);
        mSparkAdapter = new MySparkAdapter(lastData);
        sparkView.setAdapter(mSparkAdapter);





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
                    unbindService(mServiceConnection);

                    Log.i(TAG, "disconnected");
                    mConnected = false;
                    updateConnectionState(mConnected);
                }
                else {
                    Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
                    bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

                }


                return true;

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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    private IntentFilter makerIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private class MySparkAdapter extends SparkAdapter {

        List<UltraHeight> data;
//        int boundsHeight = 15050;
//        int boundsWidth = 200;

        @Override
        public RectF getDataBounds() {
//            int left = getCount()> boundsWidth ?getCount()- boundsWidth :0;
//            int right = getCount()> boundsWidth ?getCount(): boundsWidth;
//
//            RectF bounds = super.getDataBounds();
//            bounds.left = left;
//            bounds.right = right;
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
            return 0;
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
