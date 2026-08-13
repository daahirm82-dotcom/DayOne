package com.dayone.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class GymEntry(
    val id: Long,
    val exercise: String,
    val sets: String,
    val reps: String,
    val weight: String,
    val date: String
)

fun loadGymEntries(context: Context): List<GymEntry> {
    val prefs = getPrefs(context)
    val json = prefs.getString("gym_entries", "[]") ?: "[]"
    val arr = JSONArray(json)
    val list = mutableListOf<GymEntry>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
            GymEntry(
                id = o.getLong("id"),
                exercise = o.getString("exercise"),
                sets = o.getString("sets"),
                reps = o.getString("reps"),
                weight = o.getString("weight"),
                date = o.getString("date")
            )
        )
    }
    return list.reversed()
}

fun saveGymEntries(context: Context, entries: List<GymEntry>) {
    val arr = JSONArray()
    entries.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("exercise", it.exercise)
        o.put("sets", it.sets)
        o.put("reps", it.reps)
        o.put("weight", it.weight)
        o.put("date", it.date)
        arr.put(o)
    }
    getPrefs(context).edit().putString("gym_entries", arr.toString()).apply()
}

@Composable
fun GymScreen(context: Context) {
    var entries by remember { mutableStateOf(loadGymEntries(context)) }
    var exercise by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Gym Tracker", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = exercise,
            onValueChange = { exercise = it },
            label = { Text("Jimicsiga (Exercise)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("Sets") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Miisaanka (kg)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (exercise.isBlank() || sets.isBlank() || reps.isBlank() || weight.isBlank()) {
                    errorMsg = "Fadlan buuxi dhammaan meelaha"
                    return@Button
                }
                errorMsg = null
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val newEntry = GymEntry(
                    id = System.currentTimeMillis(),
                    exercise = exercise,
                    sets = sets,
                    reps = reps,
                    weight = weight,
                    date = sdf.format(Date())
                )
                val updated = (entries + newEntry).sortedByDescending { e -> e.id }
                entries = updated
                saveGymEntries(context, updated)
                exercise = ""
                sets = ""
                reps = ""
                weight = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ku Dar (Add)")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Taariikhda", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(entries) { entry ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.exercise, fontWeight = FontWeight.SemiBold)
                            Text("${entry.sets} sets x ${entry.reps} reps — ${entry.weight}kg")
                            Text(entry.date, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            val updated = entries.filter { it.id != entry.id }
                            entries = updated
                            saveGymEntries(context, updated)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}
