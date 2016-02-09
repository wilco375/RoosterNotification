package com.wilco375.roosternotification;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class ZermeloSync {
    SharedPreferences sp;

    public void syncZermelo(final Context context, final Activity activity, final boolean updateMainActivity, final boolean copyClipboard){

		if (!Utils.isWifiConnected(context) && !updateMainActivity) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                //Get start and end of week in unix time
                long startOfWeek = Utils.getUnixWeek()[0];
                long endOfWeek = Utils.getUnixWeek()[1];

                //Get token
                sp = context.getSharedPreferences("Main", Context.MODE_PRIVATE);

                //Get schedule string
                String scheduleString = getScheduleString(startOfWeek, endOfWeek, sp.getString("token", ""));
                if(scheduleString == null) return;

                //If necessary copy string to clipboard
                if(copyClipboard) Utils.copyText(activity, context, "Rooster JSON", scheduleString, true);

                try{
                    //Format to JSONArray
                    JSONArray schedule = new JSONObject(scheduleString).getJSONObject("response").getJSONArray("data");
                    List<Schedule> scheduleArray = new ArrayList<>();

                    //Loop trough all lessons and create an object array with all lessons
                    for(int i = 0; i < schedule.length(); i++) {
                       scheduleArray.add(getScheduleByJSON(schedule.getJSONObject(i)));
                    }

                    //Loop through all lessons and check cancelled
                    for (Schedule lesson : scheduleArray) {
                        if(lesson.getCancelled()) cancelNotification(lesson, context);
                    }

                    //Save schedule
                    ScheduleHandler.setSchedule(context, Utils.scheduleListToArray(scheduleArray));

                    //Update widgets
                    Utils.updateWidgets(context);

                    //Restart app if necessary
                    if(updateMainActivity){
                        MainActivity mainActivity = (MainActivity) activity;
                        mainActivity.getSchedule();
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

	private void cancelNotification(Schedule schedule, Context context){
        if(sp.getBoolean("notifyCancel",true)) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.notification_logo)
                    .setContentTitle("Er valt een uur uit op " + Utils.dayIntToStr(schedule.getDay()).toLowerCase());

            if(schedule.getTimeslot() != 0) builder.setContentText(schedule.getTimeslot() + ". " + schedule.getSubject());
            else builder.setContentText(schedule.getSubject());

            Notification notification = builder.build();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);

            Calendar calendar = Calendar.getInstance();
            int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
            int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            if(currentDay == Calendar.SATURDAY || (currentDay == Calendar.FRIDAY && calendar.get(Calendar.HOUR_OF_DAY)>=17)){
                currentDay = Calendar.SUNDAY;
                currentWeek += 1;
            }

            String currentNotString = intStr(calendar.get(Calendar.YEAR)) + intStr(currentWeek) + intStr(schedule.getDay()) + intStr(schedule.getTimeslot()) + schedule.getSubject();
            if(schedule.getDay()>=currentDay && !sp.getString("prevNots","").contains(currentNotString)) {
                int notId = sp.getInt("notId", 2);
                SharedPreferences.Editor spe = sp.edit();
                spe.putInt("notId", notId + 1);
                spe.putString("prevNots", sp.getString("prevNots","") + currentNotString);
                spe.apply();
                notificationManagerCompat.notify(notId, notification);
            }
        }
	}

    private static String intStr(int integer){
        return String.valueOf(integer);
    }

    private String getScheduleString(long startOfWeek,long endOfWeek, String token){
        try{
            HttpClient client = HttpClientBuilder.create().build();
            String url = "https://jfc.zportal.nl/api/v2/appointments?user=~me&start=" + String.valueOf(startOfWeek) + "&end=" + String.valueOf(endOfWeek) + "&valid=true&fields=subjects,cancelled,locations,startTimeSlot,start,end,groups,type&access_token=" + token;
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);

            if (response.getStatusLine().getStatusCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                return br.readLine();
            }else return null;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private Schedule getScheduleByJSON(JSONObject jsonObject){
        try {
            Schedule scheduleItem = new Schedule();
            long unixStart = jsonObject.getLong("start");
            long unixEnd = jsonObject.getLong("end");
            Calendar calendarStart = Utils.unixToCalendar(unixStart);
            Calendar calendarEnd = Utils.unixToCalendar(unixEnd);

            //Day
            scheduleItem.setDay(calendarStart.get(Calendar.DAY_OF_WEEK));

            //Start (Readable string)
            scheduleItem.setStart(Utils.calendarTimeToString(calendarStart));

            //End (Readable string)
            scheduleItem.setEnd(Utils.calendarTimeToString(calendarEnd));

            //Start Hour (Decimal double representing hours and minutes)
            scheduleItem.setStartHour(Utils.calendarTimeToDouble(calendarStart));

            //End Hour (Decimal double representing hours and minutes)
            scheduleItem.setEndHour(Utils.calendarTimeToDouble(calendarEnd));

            //Subject
            JSONArray subjectsArray = jsonObject.getJSONArray("subjects");
            String subjectsString = "";
            for (int j = 0; j < subjectsArray.length(); j++) {
                if (!subjectsString.equals("")) subjectsString += "/";
                subjectsString += subjectsArray.get(j).toString().toUpperCase();
            }
            scheduleItem.setSubject(subjectsString);

            //Group
            JSONArray groupsArray = jsonObject.getJSONArray("groups");
            String groupsString = "";
            for (int j = 0; j < groupsArray.length(); j++) {
                if (!groupsString.equals("")) groupsString += "/";
                groupsString += groupsArray.get(j).toString().toUpperCase();
            }
            scheduleItem.setGroup(groupsString);

            //Location
            JSONArray locationsArray = jsonObject.getJSONArray("locations");
            String locationsString = "";
            for (int j = 0; j < locationsArray.length(); j++) {
                if (!locationsString.equals("")) locationsString += "/";
                locationsString += locationsArray.get(j).toString();
            }
            scheduleItem.setLocation(locationsString);

            //Cancelled
            if(!jsonObject.isNull("cancelled"))
                scheduleItem.setCancelled(jsonObject.getBoolean("cancelled"));
            else scheduleItem.setCancelled(false);


            //Timeslot
            if(!jsonObject.isNull("startTimeSlot"))
                scheduleItem.setTimeslot(jsonObject.getInt("startTimeSlot"));
            else scheduleItem.setTimeslot(0);

            //Type
            scheduleItem.setType(jsonObject.getString("type"));

            return scheduleItem;
        }catch (JSONException e){
            e.printStackTrace();
            return new Schedule();
        }
    }

    public static boolean authenticate(String code, Context context, SharedPreferences sp) {
        if (code.equals("")) {
            Toast.makeText(context, R.string.invalid_code, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!Utils.isConnected(context)) {
            Toast.makeText(context, R.string.no_connection, Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            HttpClient client = HttpClientBuilder.create().build();

            HttpPost post = new HttpPost("https://jfc.zportal.nl/api/v2/oauth/token?");

            List<NameValuePair> nameValuePair = new ArrayList<>(2);
            nameValuePair.add(new BasicNameValuePair("grant_type", "authorization_code"));
            nameValuePair.add(new BasicNameValuePair("code", code));

            post.setEntity(new UrlEncodedFormEntity(nameValuePair));
            HttpResponse response = client.execute(post);
            if(response.getStatusLine().getStatusCode() != 200) return false;

            JSONObject tokenJson = new JSONObject(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))).readLine());
            String token = tokenJson.getString("access_token");

            if (token == null) {
                Toast.makeText(context, R.string.invalid_code, Toast.LENGTH_LONG).show();
                return false;
            }
            if (token.equals("")) {
                Toast.makeText(context, R.string.invalid_code, Toast.LENGTH_LONG).show();
                return false;
            }
            Toast.makeText(context, R.string.auth_success, Toast.LENGTH_LONG).show();

            SharedPreferences.Editor spe = sp.edit();
            spe.putString("token", token);
            spe.putBoolean("zermeloSync", true);
            spe.apply();
            return true;
        }catch(JSONException e){
            e.printStackTrace();
            return false;
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
    }
}
