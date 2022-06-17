package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //step 0 뷰를 초기화 해주기
        initOnOffButton()
        initChangeAlarmTimeButton()

        //step 1 데이터 가져오기
        val model = fetchDataFromSharedPreference()
        renderView(model)

        //step 2 뷰에 데이터 그려주기

    }

    private fun initOnOffButton() {
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {
            // 저장한 데이터 확인
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            // 데이터를 저장
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)

            // 온오프에 따라 작업을 처리
            if (newModel.onOff) {
                // 온 -> 알람을 등록
                // 켜진경우 - > 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)
                    if(before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this,
                    ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )

            } else {
                // 오프 -> 알람을 제거
                // 꺼진경우 -> 알람을 제거
                cancelAlarm()
            }





        }
   }

    private fun initChangeAlarmTimeButton() {
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmTimeButton)
        changeAlarmButton.setOnClickListener {
            // 현재시간 가져오기
            val calendar = Calendar.getInstance()

            // timepickerdialog 사용해 시간을 설정
            TimePickerDialog(this, { picker, hour, minute ->
                // 설정된 시간으로 데이터 저장
                val model = saveAlarmModel(hour, minute, false)

                // 뷰 업데이트
                renderView(model)

                // 알람 시간을 바꿔기 때문에 기존 알람 삭제
                cancelAlarm()
//                val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(
//                    this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE
//                )?.cancel()


            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
                .show()

        }
    }

    private fun saveAlarmModel(hour : Int, minute : Int, onOff : Boolean) : AlarmDisplayModel{
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME,Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(ALARM_KEY,model.makeDataForDB())
            putBoolean(ONOFF_KEY,model.onOff)
            commit()
        }

        return model
    }

    private fun fetchDataFromSharedPreference() : AlarmDisplayModel{
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME,Context.MODE_PRIVATE)

        val timeDBValue = sharedPreferences.getString(ALARM_KEY,"9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )
        // 보정 보정 예외처리
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(
            this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE
        )

        if ((pendingIntent == null) and alarmModel.onOff) {
            // 알람은 꺼져있는데 데이터는 켜저있는 경우
            alarmModel.onOff = false
        } else if((pendingIntent != null) and alarmModel.onOff.not()) {
            // 알람은 켜저있는데 데이터는 꺼저있는경우 알람을 취소함
            pendingIntent.cancel()
        }

        return alarmModel
    }

    private fun renderView(model : AlarmDisplayModel) {
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }
        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }
        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(
            this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE
        )?.cancel()
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }
}



































