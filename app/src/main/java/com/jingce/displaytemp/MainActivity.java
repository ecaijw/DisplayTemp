package com.jingce.displaytemp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;
import android.graphics.Color;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private class BluetoothHelper {

    }

    private static final String TAG = "BluetoothHelper";

    // 蓝牙设备信息
    private static final String DEVICE_NAME = "jcbg-0201";
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000ffe4-0000-1000-8000-00805f9b34fb";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private static final int REQUEST_BLUETOOTH_CONNECT = 1;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 3;
    private static final int REQUEST_BLUETOOTH = 4;
    private static final int REQUEST_BLUETOOTH_SCAN = 5;
    private static final int REQUEST_BLUETOOTH_ADMIN = 6;

    private static final int REQUEST_ALL_PERMISSIONS = 100;

    private ArrayAdapter<String> tempAdapter;
    private ArrayList<String> tempList;

    private void outputLog(String msg) {
        Log.d(TAG, msg);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // 权限已被授予
//                outputLog("Permission is granted: " + grantResults[0]);
//            } else {
//                // 权限被拒绝
//                Toast.makeText(this, "需要同意权限：" + grantResults[0], Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    outputLog("Permission is granted: " + permissions[i]);
                } else {
                    Toast.makeText(this, "需要同意权限：" + permissions[i], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void requestPermission(String permission, int permissoin_code) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    permissoin_code);
        } else {
            // 权限已被授予
            outputLog("Permission was granted: " + permission);
        }
    }

    public void createBluetooth() {
        String[] permissions = {
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADMIN
        };

        // 请求所有权限
        ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);


        // 获取蓝牙适配器
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // 开始扫描蓝牙设备
        scanForDevices();
    }


    @SuppressLint("MissingPermission")
    public void disconnectBluetooth() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void scanForDevices() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startLeScan((device, rssi, scanRecord) -> {
                String deviceName = device.getName();
                if (deviceName != null && deviceName.equalsIgnoreCase(DEVICE_NAME)) {
                    // 找到设备后，停止扫描并连接
                    bluetoothAdapter.stopLeScan(null);
                    connectToDevice(device);
                }
            });
        } else {
            Log.e(TAG, "Bluetooth is not enabled or adapter is null");
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        Log.i(TAG, "Connecting to device: " + device.getName());
        bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server.");
                    // 尝试发现服务
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
                    if (service != null) {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                        if (characteristic != null) {
                            gatt.setCharacteristicNotification(characteristic, true);

                            // 设置 CCCD 描述符
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            } else {
                                Log.e(TAG, "Descriptor not found.");
                            }
                        } else {
                            Log.e(TAG, "Characteristic not found.");
                        }
                    } else {
                        Log.e(TAG, "Service not found.");
                    }
                } else {
                    Log.e(TAG, "Service discovery failed with status: " + status);
                }
            }


            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                // 当特征值改变时，读取数据
                byte[] data = characteristic.getValue();
                if (data != null) {
                    Log.i(TAG, "Received data: " + bytesToHex(data));
                }
            }
        });
    }

    // 将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        float temperature = bytes[1] + bytes[2] / 100.0f;
        float humidity = bytes[3] + bytes[4] / 100.0f;
        float envTemperature = bytes[5] + bytes[6] / 100.0f;
        int dizuoBattery = bytes[7];
        int penBattery = bytes[8];
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        sb.append(String.format("[%.2f] [%.2f, %.2f] [%d, %d]", temperature, humidity, envTemperature, dizuoBattery, penBattery));

        this.handleBluetoothData(bytes);
        return sb.toString().trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Exit button
        Button btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectBluetooth();
                // Exit the application
                android.os.Process.killProcess(android.os.Process.myPid()); // 彻底杀死应用
            }
        });

        // Connect button
//        Button btnConnect = findViewById(R.id.btn_connect);
//        btnConnect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Display a toast message
//                Toast.makeText(MainActivity.this, "Connect", Toast.LENGTH_SHORT).show();
//            }
//        });

        // 初始化 ListView 和 Adapter
        tempList = new ArrayList<>();
        tempAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tempList);

        ListView listView = findViewById(R.id.list_temp);
        listView.setAdapter(tempAdapter);

        createBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }


    private void handleBluetoothData(byte[] bytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bytes[0] == 0) {
                    // 收到了数据包，处理蓝牙连接成功
                    if (tempList.size() == 0) {
                        insertListText("蓝牙连接成功，请拔出探头测量温度。");
                    }
                }
                if (bytes[0] == 2) {
                    StringBuilder sb = new StringBuilder();
                    float temperature = bytes[1] + bytes[2] / 100.0f;
                    float humidity = bytes[3] + bytes[4] / 100.0f;
                    float envTemperature = bytes[5] + bytes[6] / 100.0f;
                    int dizuoBattery = bytes[7];
                    int penBattery = bytes[8];
                    sb.append(String.format("[%.2f] [%.2f, %.2f] [%d, %d]", temperature, humidity, envTemperature, dizuoBattery, penBattery));

                    updateTemperature(temperature);

                    String newEntry = " 温度: " + String.format("%.2f", temperature);
                    insertListText(newEntry);
                }
            }
        });
    }

    private void updateTemperature(final float temperature) {
        TextView textView = findViewById(R.id.text_temperature);
        // 动态修改文本、字体大小和颜色
        textView.setText(String.format("%.2f ℃", temperature));
        textView.setTextSize(36); // 设置字体大小为 48px
        textView.setTextColor(Color.BLUE); // 设置字体颜色为蓝色
    }

    private void insertListText(String message) {
        // 获取当前时间并格式化
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String line = String.format("[时间: %s]", currentTime) + message;
        tempList.add(0, line); // 插入到列表第一行
        tempAdapter.notifyDataSetChanged(); // 更新 ListView 显示
    }
}
