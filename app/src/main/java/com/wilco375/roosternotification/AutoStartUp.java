package com.wilco375.roosternotification;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
public class AutoStartUp extends IntentService {

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("Setting alarmManager");

        AlarmReceiver.createNotification(this);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(getBaseContext(),AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,i,PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 300000, pendingIntent);

    }

    public AutoStartUp(){
        super("AutoStartUp");
    }
}
