package com.wilco375.roosternotification.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.StrictMode
import android.view.*
import android.widget.Toast
import com.wilco375.roosternotification.R
import com.wilco375.roosternotification.R.layout.activity_main
import com.wilco375.roosternotification.`object`.Schedule
import com.wilco375.roosternotification.general.ScheduleHandler
import com.wilco375.roosternotification.general.Utils
import com.wilco375.roosternotification.online.ZermeloSync
import io.multimoon.colorful.CAppCompatActivity
import io.multimoon.colorful.Colorful
import io.multimoon.colorful.ThemeColor
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : CAppCompatActivity() {
    private var syncing = false
    private var day: Int = 0

    private lateinit var sp: SharedPreferences
    private lateinit var schedule: Schedule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendBroadcast(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        setContentView(activity_main)
        setSupportActionBar(toolbar)

        Colorful().edit().setPrimaryColor(ThemeColor.RED)

        //Allow internet
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        //Get SharedPreferences
        sp = getSharedPreferences("Main", Context.MODE_PRIVATE)

        //Set alarm
        Utils.setAlarm(this)

        checkInit()

        getSchedule()

        //setupNavigation()
    }

    /**
     * Check if first launch or first sync
     */
    private fun checkInit() {
        if (sp.getString("token", "") == "" || sp.getString("website", "") == "") {
            val i = Intent(this@MainActivity, InitActivity::class.java)
            finish()
            startActivity(i)
        } else if (sp.getBoolean("firstSync", true)) {
            syncSchedule()

            sp.edit().putBoolean("firstSync", false).apply()
        }
    }

    /**
     * Get schedule from storage
     */
    fun getSchedule() {
        runOnUiThread({
            schedule = ScheduleHandler.getSchedule(this@MainActivity)

            setupSchedule()
        })
    }

    /**
     * Show schedule in app
     */
    private fun setupSchedule() {
        runOnUiThread({
            val daySchedule = schedule[Date()]

            for (item in daySchedule) {
                println(item)
            }

            //Set text to day
            //dayText.text = Utils.dayIntToStr(day)

            //dayListView.adapter = ScheduleListAdapter(daySchedule, sp, getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?)
        })
    }

    /**
     * Setup navigation to go to previous/next day
     */
    /*private fun setupNavigation() {
        prevDay.setOnClickListener({ _ ->
            if (day - 1 >= Calendar.MONDAY) {
                if (day - 1 == Calendar.SUNDAY + 7)
                    day = Calendar.FRIDAY
                else
                    day -= 1
                setupSchedule()
            } else {
                Toast.makeText(this@MainActivity, "Je kunt niet verder terug", Toast.LENGTH_LONG).show()
            }
        })

        nextDay.setOnClickListener({ _ ->
            if (day + 1 <= Calendar.FRIDAY + 7) {
                if (day + 1 == Calendar.SATURDAY)
                    day = Calendar.MONDAY + 7
                else
                    day += 1
                setupSchedule()
            } else {
                Toast.makeText(this@MainActivity, "Je kunt niet verder", Toast.LENGTH_LONG).show()
            }
        })
    }*/

    /**
     * Sync schedule with Zermelo
     */
    private fun syncSchedule() {
        syncing = true
        ZermeloSync().syncZermelo(application, true, false)
        Toast.makeText(application, "Rooster aan het synchroniseren...", Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            // Launch SettingsActivity
            val i = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(i)

            return true
        } else if (item.itemId == R.id.zermelo_sync) {
            // Sync schedule
            syncSchedule()

            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        Utils.updateWidgets(this)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        setupSchedule()
    }
}
