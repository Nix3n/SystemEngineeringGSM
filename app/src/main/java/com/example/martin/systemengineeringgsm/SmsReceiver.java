package com.example.martin.systemengineeringgsm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Martin on 2016-03-22.
 */
public class SmsReceiver extends BroadcastReceiver {

    private SharedPreferences smsPreferences;
    private SharedPreferences.Editor prefEditor;
    private String phoneNbr = "";
    private String message = "";
    private String latitude;
    private String longitude;
    private String strDate;
    private String latitudes;
    private String longitudes;

    private SQLiteDatabase coordinates;

    @Override
    public void onReceive(Context context, Intent intent) {

        smsPreferences = context.getSharedPreferences("smsPreferences", Context.MODE_PRIVATE);
        prefEditor = smsPreferences.edit();

        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        SmsMessage[] msgs = null;
        Object[] pdus = (Object[]) extras.get("pdus");
        msgs = new SmsMessage[pdus.length];

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

            stringBuilder.append(msgs[i].getMessageBody());

            phoneNbr = msgs[i].getOriginatingAddress();

        }

        if(phoneNbr.equals(smsPreferences.getString("phoneNbr", ""))){
            message = stringBuilder.toString();
            if(message.contains("System")) {
                prefEditor.putString("System msg", message).apply();
                prefEditor.putString("Alarm msg", "").apply();
                // A custom Intent that will used as another Broadcast
                Intent main = new Intent("SmsMessage.intent.MAIN").
                        putExtra("get_msg", message);
                //You can place your check conditions here(on the SMS or the sender)
                //and then send another broadcast
                context.sendBroadcast(main);
            }else if(message.contains("Alarm")){
                String title = "Alarm";
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(title)
                                .setContentText(message);

                Intent notificationIntent = new Intent(context, GoogleMaps.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(contentIntent);
                builder.setAutoCancel(true);

                // Add as notification
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(1008, builder.build());

                // A custom Intent that will used as another Broadcast
                Intent main = new Intent("SmsMessage.intent.MAIN").
                        putExtra("get_msg", message);
                //You can place your check conditions here(on the SMS or the sender)
                //and then send another broadcast
                context.sendBroadcast(main);
            }else{
                saveLatAndLong(context);
                // A custom Intent that will used as another Broadcast
                Intent main = new Intent("SmsMessage.intent.GoogleMaps");
                //You can place your check conditions here(on the SMS or the sender)
                //and then send another broadcast
                context.sendBroadcast(main);
            }
        }else{
            stringBuilder.delete(0, stringBuilder.length()-1);
        }
    }

    public void saveLatAndLong(Context context){
        String[] dividedMsg = message.split(":");

        latitude = dividedMsg[0];
        longitude = dividedMsg[2];

        String vertical = dividedMsg[1];
        String horizontal = dividedMsg[3];

        String latDeg = latitude.substring(0, 2);
        String lonDeg = longitude.substring(0, 3);

        String latDivision = latitude.substring(2, latitude.length()-1);
        String lonDivision = longitude.substring(3, longitude.length()-1);

        double latitudeDiv = Double.valueOf(latDivision) / 60;
        double longitudeDiv = Double.valueOf(lonDivision) / 60;

        double finishedLat = 0;
        double finishedLon = 0;

        if(vertical.equals("N") && horizontal.equals("E")){
            finishedLat = (Double.valueOf(latDeg) + latitudeDiv);
            finishedLon = (Double.valueOf(lonDeg) + longitudeDiv);
        }else if(vertical.equals("N") && horizontal.equals("W")){
            finishedLat = (Double.valueOf(latDeg) + latitudeDiv);
            finishedLon = -(Double.valueOf(lonDeg) + longitudeDiv);
        }else if(vertical.equals("S") && horizontal.equals("E")){
            finishedLat = -(Double.valueOf(latDeg) + latitudeDiv);
            finishedLon = (Double.valueOf(lonDeg) + longitudeDiv);
        }else if(vertical.equals("S") && horizontal.equals("W")){
            finishedLat = -(Double.valueOf(latDeg) + latitudeDiv);
            finishedLon = -(Double.valueOf(lonDeg) + longitudeDiv);
        }

        coordinates = context.openOrCreateDatabase("GPSCoordinates", Context.MODE_PRIVATE, null);
        coordinates.execSQL("INSERT INTO TimeAndDate(TimeAndDate) VALUES(datetime(CURRENT_TIMESTAMP, 'localtime'));");
        coordinates.execSQL("INSERT INTO Coordinates VALUES(" + round(finishedLat, 4) + "," + round(finishedLon, 4) + ");");
        coordinates.close();
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}