package com.wilco375.roosternotification;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class LesdagWidgetProvider extends AppWidgetProvider{

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK);

            if(calendar.get(Calendar.HOUR_OF_DAY)>17) day += 1;
            if(day == Calendar.SATURDAY || day == Calendar.SUNDAY) day = Calendar.MONDAY;

            SharedPreferences sp = context.getSharedPreferences("Main", Context.MODE_PRIVATE);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget_lesdag);
            views.setOnClickPendingIntent(R.id.app_widget_lesdag_layout, pendingIntent);
            views.setTextViewText(R.id.app_widget_lesdag_dag, Utils.dayIntToStr(day));

            String widgetText = "";

            Schedule[] schedule = ScheduleHandler.getScheduleByDay(context,day);
            List<Integer> strikethroughStartIndex = new ArrayList<>();
            List<Integer> strikethroughEndIndex = new ArrayList<>();

            Arrays.sort(schedule, new Schedule.ScheduleComparator());
            for(Schedule lesson : schedule){
                int timeslot = lesson.getTimeslot();
                if(timeslot == 0){
                    if(lesson.getCancelled()){
                        String string;
                        if(!lesson.getType().equals("Les")) string = lesson.getSubjectAndGroup(sp) + " (" + lesson.getType() + ") " + lesson.getLocation() + "\n";
                        else string = lesson.getSubjectAndGroup(sp) + " " + lesson.getLocation()+"\n";
                        strikethroughStartIndex.add(widgetText.length());
                        strikethroughEndIndex.add(widgetText.length()+string.length());
                        widgetText += string;
                    }else{
                        if(!lesson.getType().equals("Les")) widgetText += lesson.getSubjectAndGroup(sp) + " (" + lesson.getType() + ") " + lesson.getLocation() + "\n";
                        else widgetText += lesson.getSubjectAndGroup(sp) + " " + lesson.getLocation()+"\n";
                    }
                }else{
                    if(lesson.getCancelled()){
                        String string;
                        if(!lesson.getType().equals("Les")) string = String.valueOf(timeslot) + ". " + lesson.getSubjectAndGroup(sp) + " (" + lesson.getType() + ") " + lesson.getLocation() + "\n";
                        else string = String.valueOf(timeslot) + ". " + lesson.getSubjectAndGroup(sp) + " " + lesson.getLocation()+"\n";
                        strikethroughStartIndex.add(widgetText.length());
                        strikethroughEndIndex.add(widgetText.length()+string.length());
                        widgetText += string;
                    }else{
                        if(!lesson.getType().equals("Les")) widgetText += String.valueOf(timeslot) + ". " + lesson.getSubjectAndGroup(sp) + " (" + lesson.getType() + ") " + lesson.getLocation() + "\n";
                        else widgetText += String.valueOf(timeslot) + ". " + lesson.getSubjectAndGroup(sp) + " " + lesson.getLocation()+"\n";
                    }
                }
            }

            SpannableString widgetTextSpan = new SpannableString(widgetText);

            for(int j=0;j<strikethroughEndIndex.size();j++){
                widgetTextSpan.setSpan(new StrikethroughSpan(),strikethroughStartIndex.get(j),strikethroughEndIndex.get(j),0);
            }

            views.setTextViewText(R.id.app_widget_lesdag_content,widgetTextSpan);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
