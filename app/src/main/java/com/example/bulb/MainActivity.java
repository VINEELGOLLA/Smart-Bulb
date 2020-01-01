package com.example.bulb;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/*Created by
    NAGA VINEEL GOLLA
*/


public class MainActivity extends AppCompatActivity {

    private final int REQUEST_LOCATION_PERMISSION = 1;
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner btScanner;
    private static UUID temp_uuid=UUID.fromString("0ced9345-b31f-457d-a6a2-b3db9b03e39a");
    private static UUID beep_uuid=UUID.fromString("EC958823-F26E-43A9-927C-7E17D8F32A90");
    private static UUID on_uuid=UUID.fromString("FB959362-F26E-43A9-927C-7E17D8FB2D8D");
    //private static UUID uuid1=UUID.fromString("dfdef8c6-0876-74d1-b34c-16325be43b44");
    private static UUID uuid1=UUID.fromString("df3fb837-13ae-746f-0dc5-64e77d412692");

    List<BluetoothGattCharacteristic> list;


    private static final long SCAN_PERIOD = 20000;
    private Handler handler;
    private boolean mScanning;

    private ScanSettings settings;
    private List <ScanFilter> filters;
    private BluetoothGatt mGatt;

    UUID[] uuid = new UUID[1];
    //private static final UUID uuid1 = UUID.fromString("0CED9345-B31F-457D-A6A2-B3DB9B03E39A");

    Button on,off;
    TextView beep,temperature;
    int flag = 0;
    String[] val = new String[3];
    int i =0;

    BluetoothGattCharacteristic bluetoothGattCharacteristic;
    BluetoothGattDescriptor descriptor;






    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        on = findViewById(R.id.on);
        off = findViewById(R.id.off);
        beep = findViewById(R.id.beep);
        temperature = findViewById(R.id.temperature);

        handler = new Handler();

        uuid[0] =uuid1;


        requestLocationPermission();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }


        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        //btScanner = bluetoothAdapter.getBluetoothLeScanner();

    }



    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                btScanner = bluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList <ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == MainActivity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }



    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(mLeScanCallback);

                }
            }, SCAN_PERIOD);
            bluetoothAdapter.startLeScan(uuid,mLeScanCallback);

        } else {
                bluetoothAdapter.stopLeScan(mLeScanCallback);

        }
    }




    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("onLeScan", String.valueOf(device.getName()));
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            BluetoothGattService bluetoothGattService = gatt.getService(uuid1);
            Log.d("lkj",String.valueOf(bluetoothGattService.getCharacteristics().size()));
            list= bluetoothGattService.getCharacteristics();
            Log.d("uuid",list.get(0).getUuid().toString());
            BluetoothGattCharacteristic chract=list.get(0);
            final BluetoothGattCharacteristic chract1=list.get(1);





            on.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Toast.makeText(getApplicationContext(),"on",Toast.LENGTH_LONG).show();

                    BluetoothGattCharacteristic chrac=list.get(1);
                    byte[] b1= new byte[1];
                    b1[0]=(byte)1;
                    chrac.setValue(b1);
                    gatt.writeCharacteristic(chrac);

                }
            });

            off.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Toast.makeText(getApplicationContext(),"off",Toast.LENGTH_LONG).show();


                    BluetoothGattCharacteristic chrac=list.get(1);
                    byte[] b1= new byte[1];
                    b1[0]=(byte)0;
                    chrac.setValue(b1);
                    gatt.writeCharacteristic(chrac);

                }
            });

            beep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(flag ==0) {
                        Log.d("cjcjk", "clicked");
                        BluetoothGattCharacteristic chrac = list.get(2);
                        byte[] b1 = new byte[1];
                        b1[0] = (byte) 1;
                        chrac.setValue(b1);
                        gatt.writeCharacteristic(chrac);
                        flag = 1;
                    }
                    else{
                        flag = 0;
                        BluetoothGattCharacteristic chrac = list.get(2);
                        byte[] b1 = new byte[1];
                        b1[0] = (byte) 0;
                        chrac.setValue(b1);
                        gatt.writeCharacteristic(chrac);

                    }

                }
            });
            gatt.setCharacteristicNotification(chract,true);
            descriptor = chract.getDescriptor(uuid1);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);





        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {

            //Log.i("onCharacteristicRead", String.valueOf(characteristic.getWriteType()));
            Log.d("kl1", new String(characteristic.getValue()));

            //Log.d("kl",String.valueOf(characteristic.getValue().length));
            //gatt.disconnect();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //super.onCharacteristicChanged(gatt, characteristic);
            Log.d("checxk",characteristic.getUuid().toString());

            if(characteristic.getUuid().toString().equals(temp_uuid.toString())) {
                final String temp = new String(characteristic.getValue());
                Log.d("kaa", new String(characteristic.getValue()));


                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        temperature.setText(temp + " " + "F");
                    }
                });

            }
            else if(characteristic.getUuid().toString().equals(on_uuid.toString())){
                final String st = new String(characteristic.getValue());
                if(st == "0") {
                    on.setBackgroundColor(Color.GREEN);

                }
                else{
                    off.setBackgroundColor(Color.RED);
                }



            }
        }


    };


    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }
}
