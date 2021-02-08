package com.wilco375.roosternotification.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.wilco375.roosternotification.R
import com.wilco375.roosternotification.`object`.Schedule
import com.wilco375.roosternotification.activity.MainActivity
import com.wilco375.roosternotification.general.Utils
import io.multimoon.colorful.Colorful
import io.multimoon.colorful.Defaults
import io.multimoon.colorful.ThemeColor
import io.multimoon.colorful.initColorful
import java.text.SimpleDateFormat
import java.util.*


class LesdagWidgetProvider : AppWidgetProvider() {
    private val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        Utils.updateColorful(context)

        for (appWidgetId in appWidgetIds) {
            val mainActivityIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, mainActivityIntent, 0)
            val views = RemoteViews(context.packageName, R.layout.app_widget_lesdag)

            views.setOnClickPendingIntent(R.id.app_widget_lesdag_layout, pendingIntent)
            views.setTextViewText(R.id.app_widget_lesdag_dag, Utils.currentDay())

            val serviceIntent = Intent(context, ScheduleListSmallService::class.java)
            views.setRemoteAdapter(R.id.app_widget_lesdag_content, serviceIntent)

            views.setPendingIntentTemplate(R.id.app_widget_lesdag_content, pendingIntent)

            val schedule = Schedule.getInstance(context)[Utils.currentScheduleDate()]

            // Get 15 minutes before current time
            val currentTime = Calendar.getInstance().also { it.add(Calendar.MINUTE, 15) }.time
            var upcomingItem = schedule.getItems().firstOrNull()
            for (lesson in schedule) {
                if (currentTime >= lesson.start && currentTime <= lesson.end) {
                    upcomingItem = lesson
                }
            }

            val sp = Utils.getSharedPreferences(context)
            val teacher = sp.getBoolean("teacher", false)
            val teacherFull = sp.getBoolean("teacherFull", false)
            if (!teacher && !teacherFull) {
                views.setViewVisibility(R.id.app_widget_lesdag_teacher, View.GONE)
            } else {
                views.setViewVisibility(R.id.app_widget_lesdag_teacher, View.VISIBLE)
            }

            val primaryColor = Colorful().getPrimaryColor().getColorPack().normal().asInt()

            views.setImageViewBitmap(R.id.app_widget_lesdag_location_bg,
                    Utils.getRoundedSquareBitmap(50.0, 6.0, primaryColor)
            )

            views.setInt(R.id.app_widget_lesdag_dag, "setTextColor", primaryColor)

            if (upcomingItem != null) {
                var summary = upcomingItem.getSubjectAndGroup(sp)
                if (upcomingItem.type != "Les") summary += " (${upcomingItem.type})"
                views.setTextViewText(R.id.app_widget_lesdag_subject, summary)

                if (teacherFull) {
                    views.setTextViewText(R.id.app_widget_lesdag_teacher, upcomingItem.teacherFull)
                } else if (teacher) {
                    views.setTextViewText(R.id.app_widget_lesdag_teacher, upcomingItem.teacher)
                }
                views.setTextViewText(R.id.app_widget_lesdag_time,
                        "${hourFormat.format(upcomingItem.start)} - ${hourFormat.format(upcomingItem.end)}")
                views.setTextViewText(R.id.app_widget_lesdag_location, upcomingItem.location)
            } else {
                views.setTextViewText(R.id.app_widget_lesdag_subject, "")
                views.setTextViewText(R.id.app_widget_lesdag_teacher, "")
                views.setTextViewText(R.id.app_widget_lesdag_time, "")
                views.setTextViewText(R.id.app_widget_lesdag_location, "")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
