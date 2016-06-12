package com.example.martin.systemengineeringgsm;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private BroadcastReceiver mIntentReceiver;

    private Button BLE, setNbr;
    private LinearLayout layout;

    private TextView testText, connectionStatus;

    private EditText phoneNbr;

    private Handler inputHandler = new Handler();
    private Thread firstStartUp;

    private boolean running = false;
    private boolean delay = true;

    private IntentFilter intentFilter;

    private SharedPreferences smsPreferences;

    private SharedPreferences.Editor prefEditor;

    private View startUpSettingsView;
    private PopupWindow popupWindow;

    private BluetoothLeService mBluetoothLeService;
    private String deviceAddress;

    private GUI gui;

    private int width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        width = metrics.widthPixels;
        height = metrics.heightPixels;

        gui = new GUI(width, height, getApplicationContext());

        layout = (LinearLayout) findViewById(R.id.mainLayout);

        connectionStatus = (TextView) findViewById(R.id.isConnected);
        BLE = gui.largeBtn("Connect to BLE");
        BLE.setBackgroundResource(android.R.drawable.btn_default);
        layout.addView(BLE);
        Button GPSButton = gui.largeBtn("Google Maps");
        GPSButton.setBackgroundResource(android.R.drawable.btn_default);
        layout.addView(GPSButton);

        final Intent googleMaps = new Intent(this, GoogleMaps.class);
        final Intent scanBLE = new Intent(this, DeviceScanActivity.class);

        smsPreferences = getApplicationContext().getSharedPreferences("smsPreferences", MODE_PRIVATE);
        prefEditor = smsPreferences.edit();

        if(!smsPreferences.getString("deviceAddress", "").equals("")){
            deviceAddress = smsPreferences.getString("deviceAddress", "");
            BLE.setText("Connect to BLE");
            BLE.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectToBLE();
                }
            });
        }else{
            BLE.setText("Scan for BLE devices");
            BLE.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(scanBLE, 3);
                }
            });
        }


        GPSButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(googleMaps);
                }
            });

        LayoutInflater inflater = getLayoutInflater();
        startUpSettingsView = inflater.inflate(R.layout.start_up_settings, null);

        if(smsPreferences.getString("phoneNbr", "").equals("")){
            setNbr = gui.largeBtn("Set Sim Card Number");
            setNbr.setBackgroundResource(android.R.drawable.btn_default);
            setNbr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startUpSettings();
                }
            });
            layout.addView(setNbr);
        }
    }

    public void startUpSettings(){
        int popupWidth = width - width/3;
        int popupHeight = height/2;

        popupWindow = new PopupWindow(
                startUpSettingsView,
                popupWidth,
                popupHeight);

        //screen_background_dark_transparent

        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.toast_frame));
        popupWindow.setFocusable(true);
        //popupWindow.update();

        phoneNbr = (EditText) startUpSettingsView.findViewById(R.id.phoneNbr); //0730755968
        phoneNbr.setImeOptions(EditorInfo.IME_ACTION_DONE);
        phoneNbr.setSingleLine(true);
        String nbr = smsPreferences.getString("phoneNbr", "");
        if(!nbr.equals("")){
            phoneNbr.setHint(nbr);
            phoneNbr.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        final Button save = (Button) startUpSettingsView.findViewById(R.id.save);
        save.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (phoneNbr.getText().length() < 8) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Phone number not entered!",
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    prefEditor.putString("phoneNbr", phoneNbr.getText().toString()).commit();
                    if(setNbr != null) {
                        layout.removeView(setNbr);
                    }
                    popupWindow.dismiss();
                }
            }
        });

        ImageButton info = (ImageButton) startUpSettingsView.findViewById(R.id.infoBtn);
        info.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.ic_dialog_info));
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(startUpSettingsView.getContext());
                builder.setCancelable(true);
                builder.setTitle("Sim Card Number");
                builder.setMessage("Enter the Sim Card number of the alarm device.");
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
        popupWindow.showAtLocation(findViewById(R.id.mainLayout), Gravity.CENTER, 0, 0);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("FAIL", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if(mBluetoothLeService.connect(deviceAddress)) {
                BLE.setText("Disconnect from BLE");
                BLE.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        disconnectFromBLE();
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void connectToBLE(){
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public void disconnectFromBLE(){
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
        unbindService(mServiceConnection);

        BLE.setText("Connect to BLE");
        BLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToBLE();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            if(resultCode == RESULT_OK) {
                String device = data.getDataString();
                Log.i("Log", "Device recieved: " + device);
                //connectTo(device);
            }
        }else if(requestCode == 3){
            if(resultCode == RESULT_OK){
                String deviceInfo = data.getDataString();
                String[] nameAndAddress = deviceInfo.split(",");
                prefEditor.putString("deviceAddress", nameAndAddress[1]).commit();
                deviceAddress = nameAndAddress[1];
                //deviceAddress = "88:33:14:d9:87:8e";
                Log.i("Log", "Device recieved: " + deviceInfo);
                connectToBLE();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        smsPreferences = getApplicationContext().getSharedPreferences("smsPreferences", MODE_PRIVATE);
        prefEditor = smsPreferences.edit();

        if(smsPreferences.getString("System msg", ""). equals("")) {
            String systemStatus = "System is off";
            connectionStatus.setText(systemStatus);
            connectionStatus.setTextSize(height/50);
            layout.setBackgroundResource(R.drawable.system_off_background);
        }else if(smsPreferences.getString("Alarm msg", "").equals("")) {
            String systemStatus = smsPreferences.getString("System msg", "");
            connectionStatus.setText(systemStatus);
            connectionStatus.setTextSize(height/50);
            if(systemStatus.contains("on")) {
                layout.setBackgroundResource(R.drawable.system_on_background);
            }else{
                layout.setBackgroundResource(R.drawable.system_off_background);
            }
        }else{
            String alarmStatus = smsPreferences.getString("Alarm msg", "");
            String[] alarmMsg = alarmStatus.split(":");
            connectionStatus.setText(alarmMsg[1]);
            connectionStatus.setTextSize(height/40);
            layout.setBackgroundResource(R.drawable.alarm_background);
        }

        intentFilter = new IntentFilter("SmsMessage.intent.MAIN");
        mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String stringToReceive = intent.getStringExtra("get_msg");

                if(stringToReceive.contains("System")){
                    prefEditor.putString("System msg", stringToReceive).apply();
                    prefEditor.putString("Alarm msg", "").apply();
                    connectionStatus.setText(stringToReceive);
                    connectionStatus.setTextSize(height/50);
                    if(stringToReceive.contains("on")) {
                        layout.setBackgroundResource(R.drawable.system_on_background);
                    }else{
                        layout.setBackgroundResource(R.drawable.system_off_background);
                    }
                }else if(stringToReceive.contains("Alarm")){
                    prefEditor.putString("Alarm msg", stringToReceive).apply();
                    String[] alarmMsg = stringToReceive.split(":");
                    connectionStatus.setText(alarmMsg[1]);
                    connectionStatus.setTextSize(height/40);
                    layout.setBackgroundResource(R.drawable.alarm_background);
                }
            }
        };
        this.registerReceiver(mIntentReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.ble_scan) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(intent, 3);
            return true;
        }else if(id == R.id.phoneNbr){
            startUpSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //addNotification();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBluetoothLeService != null) {
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
        //addNotification();
        if(mIntentReceiver != null) {
            this.unregisterReceiver(this.mIntentReceiver);
        }
    }
}
