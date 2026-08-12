package com.dayone.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class PrayerTimes(
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent { DayOneApp(this) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "salah_channel",
                "Salah Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun DayOneApp(context: Context) {
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var times by remember { mutableStateOf<PrayerTimes?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DayOne — Salah", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Magaalada (City)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Dalka (Country)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (city.isBlank() || country.isBlank()) {
                            errorMsg = "Fadlan geli magaalada iyo dalka"
                            return@Button
                        }
                        errorMsg = null
                        loading = true
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    fetchPrayerTimes(city, country)
                                }
                                times = result
                                schedulePrayerAlarms(context, result)
                            } catch (e: Exception) {
                                errorMsg = "Khalad: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (loading) "Sugaya..." else "Hel Waqtiyada Salaadda")
                }

                Spacer(modifier = Modifier.height(24.dp))

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                times?.let { t ->
                    PrayerRow("Fajr", t.fajr)
                    PrayerRow("Dhuhr", t.dhuhr)
                    PrayerRow("Asr", t.asr)
                    PrayerRow("Maghrib", t.maghrib)
                    PrayerRow("Isha", t.isha)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Digniino ayaa la dejiyay maalinta xigta oo saacadaas ah.",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerRow(name: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontWeight = FontWeight.SemiBold)
        Text(time)
    }
}

fun fetchPrayerTimes(city: String, country: String): PrayerTimes {
    val encodedCity = URLEncoder.encode(city, "UTF-8")
    val encodedCountry = URLEncoder.encode(country, "UTF-8")
    val urlStr = "https://api.aladhan.com/v1/timingsByCity?city=$encodedCity&country=$encodedCountry&method=2"
    val url = URL(urlStr)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000

    val responseCode = conn.responseCode
    if (responseCode != 200) {
        throw Exception("Server error: $responseCode")
    }

    val response = conn.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(response)
    val timings = json.getJSONObject("data").getJSONObject("timings")

    return PrayerTimes(
        fajr = timings.getString("Fajr"),
        dhuhr = timings.getString("Dhuhr"),
        asr = timings.getString("Asr"),
        maghrib = timings.getString("Maghrib"),
        isha = timings.getString("Isha")
    )
}

fun schedulePrayerAlarms(context: Context, times: PrayerTimes) {
    val prayers = listOf(
        "Fajr" to times.fajr,
        "Dhuhr" to times.dhuhr,
        "Asr" to times.asr,
        "Maghrib" to times.maghrib,
        "Isha" to times.isha
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            return
        }
    }

    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    for ((index, pair) in prayers.withIndex()) {
        val (name, timeStr) = pair
        try {
            val cleanTime = timeStr.substringBefore(" ")
            val date = sdf.parse(cleanTime) ?: continue
            val cal = Calendar.getInstance()
            val now = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, date.hours)
            cal.set(Calendar.MINUTE, date.minutes)
            cal.set(Calendar.SECOND, 0)

            if (cal.before(now)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("prayer_name", name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            // skip this prayer if parsing fails
        }
    }
}
