package com.example.martin.systemengineeringgsm;

/**
 * Created by Martin on 2016-03-23.
 */


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class GoogleMaps extends AppCompatActivity{

    private GoogleMap mMap;
    private String latitude;
    private String longitude;

    private SharedPreferences smsPreferences;
    private SharedPreferences.Editor prefEditor;

    private String timeStamp;
    private String[] latitudes;
    private String[] longitudes;

    private SQLiteDatabase coordinates;
    private ArrayAdapter<String> adapter;
    private TextView markerInfo;

    public BroadcastReceiver mIntentReceiver;
    public IntentFilter intentFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);

        markerInfo = (TextView) findViewById(R.id.markerInfo);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        coordinates = openOrCreateDatabase("GPSCoordinates", MODE_PRIVATE, null);
        coordinates.execSQL("CREATE TABLE IF NOT EXISTS Coordinates(Latitude VARCHAR, Longitude VARCHAR);");
        coordinates.execSQL("CREATE TABLE IF NOT EXISTS TimeAndDate(TimeAndDate DATETIME);");

        smsPreferences = getSharedPreferences("smsPreferences", Context.MODE_PRIVATE);
        prefEditor = smsPreferences.edit();

        Cursor timeCursor = coordinates.rawQuery("SELECT * FROM TimeAndDate", null);
        String lastTimeStamp = "";
        if(timeCursor.getCount()!=0) {
            if(timeCursor.moveToLast()) {
                lastTimeStamp = timeCursor.getString(0);
            }
        }
        timeCursor.close();

        Cursor coordCursor = coordinates.rawQuery("SELECT * FROM Coordinates", null);
        String lastLatitude = "";
        String lastLongitude = "";
        if(coordCursor.getCount()!=0){
            if(coordCursor.moveToLast()){
                lastLatitude = coordCursor.getString(0);
                lastLongitude = coordCursor.getString(1);
            }
        }
        coordCursor.close();

        if(!lastTimeStamp.equals("")){
            drawMarker(Double.valueOf(lastLatitude), Double.valueOf(lastLongitude));
            markerInfo.setText("Time: " + lastTimeStamp + "\nLatitude: " + lastLatitude + "\nLongitude: " + lastLongitude);//"Time: " + timeArray[i] + "\nLatitude: " + coordinatesArray[i * 2] + " Longitude: " + coordinatesArray[i * 2 + 1];
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        intentFilter = new IntentFilter("SmsMessage.intent.GoogleMaps");
        mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Cursor timeCursor = coordinates.rawQuery("SELECT * FROM TimeAndDate", null);
                String lastTimeStamp = "";
                if(timeCursor.getCount()!=0) {
                    if(timeCursor.moveToLast()) {
                        lastTimeStamp = timeCursor.getString(0);
                    }
                }
                timeCursor.close();

                Cursor coordCursor = coordinates.rawQuery("SELECT * FROM Coordinates", null);
                String lastLatitude = "";
                String lastLongitude = "";
                if(coordCursor.getCount()!=0){
                    if(coordCursor.moveToLast()){
                        lastLatitude = coordCursor.getString(0);
                        lastLongitude = coordCursor.getString(1);
                    }
                }
                coordCursor.close();

                if(!lastTimeStamp.equals("")){
                    drawMarker(Double.valueOf(lastLatitude), Double.valueOf(lastLongitude));
                    markerInfo.setText("Time: " + lastTimeStamp + "\nLatitude: " + lastLatitude + "\nLongitude: " + lastLongitude);//"Time: " + timeArray[i] + "\nLatitude: " + coordinatesArray[i * 2] + " Longitude: " + coordinatesArray[i * 2 + 1];
                }
            }
        };
        this.registerReceiver(mIntentReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        coordinates.close();
        prefEditor.putString("latitudes", "").apply();
        prefEditor.putString("longitudes", "").apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.mIntentReceiver);
        coordinates.close();
        prefEditor.putString("latitudes", "").apply();
        prefEditor.putString("longitudes", "").apply();
    }

    private void drawMarker(double latitude, double longitude){
        mMap.clear();

        //  convert the location object to a LatLng object that can be used by the map API
        LatLng currentPosition = new LatLng(latitude, longitude);//Malmö latitude och longitude

        // zoom to the current location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition,10));

        // add a marker to the map indicating our current position
        mMap.addMarker(new MarkerOptions()
                .position(currentPosition)
                .snippet("Lat:" + latitude + "Lng:" + longitude));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_google_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.show_database) {
            showDatabase();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showDatabase(){
        Cursor c = coordinates.rawQuery("SELECT * FROM Coordinates", null);
        Cursor time = coordinates.rawQuery("SELECT * FROM TimeAndDate", null);

        if(c.getCount()==0)
        {
            showMessage("Empty", "No GPSCoordinates found.", "");
            return;
        }

        StringBuffer coordinates=new StringBuffer();
        StringBuffer timeBuffer = new StringBuffer();

        while(c.moveToNext())
        {
            coordinates.append(c.getString(0) + ":");
            coordinates.append(c.getString(1) + ":");
        }

        while(time.moveToNext()){
            timeBuffer.append(time.getString(0) + "X");
        }

        c.close();
        time.close();

        showMessage("GPSCoordinates", coordinates.toString(), timeBuffer.toString());
    }

    public void showMessage(String title, String coordinates, String time)
    {
        if(title.equals("Empty")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(title);
            builder.setMessage(coordinates);
            AlertDialog alert = builder.create();
            alert.show();
        }else {
            final String[] coordinatesArray = coordinates.split(":");
            final String[] timeArray = time.split("X");

            final String[] finalArray = new String[timeArray.length];
            for (int i = 0; i < timeArray.length; i++) {
                finalArray[i] = "Time: " + timeArray[i] + "\nLatitude: " + coordinatesArray[i * 2] + "\nLongitude: " + coordinatesArray[i * 2 + 1];
            }

            adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.database_view, finalArray);
            adapter.notifyDataSetChanged();

            String[] databaseItems = new String[adapter.getCount()];
            for(int i = 0; i < adapter.getCount(); i++){
                databaseItems[i] = adapter.getItem(i);
            }

            View loadView = getLayoutInflater().inflate(R.layout.load_view, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(GoogleMaps.this);
            builder.setCancelable(true);
            builder.setView(loadView);
            builder.setTitle(title);
            builder.setItems(databaseItems,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                            drawMarker(Double.valueOf(coordinatesArray[which * 2]), Double.valueOf(coordinatesArray[which * 2 + 1]));
                            markerInfo.setText(finalArray[which]);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}
