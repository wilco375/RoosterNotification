package com.wilco375.roosternotification.`object`

import android.content.Context
import com.wilco375.roosternotification.general.Config
import java.io.Serializable
import java.util.*

class Schedule private constructor(context: Context): Serializable {
    private val scheduleDays = ArrayList<ScheduleDay>()
    private var db = context.openOrCreateDatabase("Schedule", Context.MODE_PRIVATE, null)!!

    init {
        db.enableWriteAheadLogging()

        createTables()
    }

    companion object {
        private var instance: Schedule? = null

        fun getInstance(context: Context): Schedule {
            if (instance == null) {
                instance = Schedule(context)
            }
            return instance!!
        }
    }

    private fun createTables() {
        val lessonTable = "CREATE TABLE IF NOT EXISTS Lesson (" +
                "   subject TEXT," +
                "   lessonGroup TEXT," +
                "   location TEXT," +
                "   type TEXT," +
                "   cancelled INTEGER," +
                "   start INTEGER," +
                "   end INTEGER," +
                "   timeslot INTEGER," +
                "   day INTEGER," +
                "   PRIMARY KEY (subject, lessonGroup, start)" +
                ")"
        db.execSQL(lessonTable)

        val notificationTable = "CREATE TABLE IF NOT EXISTS Notification (" +
                "   subject TEXT," +
                "   lessonGroup TEXT," +
                "   start INTEGER," +
                "   type TEXT," +
                "   PRIMARY KEY (subject, lessonGroup, start, type)" +
                ")"
        db.execSQL(notificationTable)
    }

    /**
     * Gets the schedule for a certain day
     */
    fun getScheduleByDay(day: Date) : ScheduleDay {
        val startOfDay = day.startOfDay().time
        return ScheduleDay(
                db.rawQuery("SELECT * FROM Lesson WHERE start >= $startOfDay AND end < ${startOfDay + 24*3600*1000}", null),
                day
        )
    }

    /**
     * Adds a new schedule item to the correct day
     * @param item item to add
     */
    fun addScheduleItem(item: ScheduleItem) {
        val date = item.start.toCalendar()
        val day = scheduleDays.firstOrNull { it -> it.day.time == date.timeInMillis || it.day.toCalendar().isOnSameDayAs(date) }
        if(day != null) {
            // Day is already in the list
            day.addItem(item)
            // Set day to same date as other items in that day so it will be faster to match next time
            item.day = day.day
        } else {
            // Day is not in list yet
            val newDay = ScheduleDay(item.start)
            newDay.addItem(item)
            scheduleDays.add(newDay)
        }
    }

    fun save() {
        for (scheduleDay in scheduleDays) {
            val startOfDay = scheduleDay.day.startOfDay().time
            db.execSQL("DELETE FROM Lesson WHERE (start >= $startOfDay AND end < ${startOfDay + 24*3600*1000}) OR (end < ${startOfDay - Config.SYNC_WINDOW*24*3600*1000L})")
            for (item in scheduleDay) {
                item.apply {
                    db.execSQL("INSERT INTO Lesson VALUES (${subject.escape()}, ${group.escape()}, ${location.escape()}, ${type.escape()}, ${if(cancelled) 1 else 0}, ${start.time}, ${end.time}, $timeslot, ${day.time})")
                }
            }
        }
        scheduleDays.clear()
    }

    /**
     * Check if the user has been sent a notification yet for a certain item
     */
    fun isCancelledNotified(item: ScheduleItem): Boolean {
        // Check if it has been notified yet
        val cursor = db.rawQuery("SELECT * FROM Notification WHERE subject = ${item.subject.escape()} AND lessonGroup = ${item.group.escape()} AND start = ${item.start.time} AND type = ${"cancelled".escape()}", null)
        val result = cursor.count > 0
        cursor.close()

        if(!result) {
            // Update database
            db.execSQL("INSERT INTO Notification VALUES (${item.subject.escape()}, ${item.group.escape()}, ${item.start.time}, ${"cancelled".escape()})")
        }

        return result
    }

    operator fun plusAssign(item: ScheduleItem) = addScheduleItem(item)

    operator fun get(day: Date) = getScheduleByDay(day)

    operator fun iterator() : Iterator<ScheduleItem>
            = getAllScheduleItems().iterator()

    fun getAllScheduleItems() : List<ScheduleItem>
        = scheduleDays.map { it -> it.getItems() }.flatten()
}
