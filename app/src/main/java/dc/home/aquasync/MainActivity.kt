package dc.aquasync

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.PropertyName
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import kotlinx.coroutines.delay
import java.util.Locale

// Updated Data model matching our new database node structure
data class PumpState(
    @get:PropertyName("is_on") @set:PropertyName("is_on") var is_on: Boolean = false,
    @get:PropertyName("started_at") @set:PropertyName("started_at") var started_at: Long = 0L,
    @get:PropertyName("last_action_by") @set:PropertyName("last_action_by") var last_action_by: String = ""
)

class MainActivity : ComponentActivity() {

    // Target your specific Singapore database URL explicitly
    private val databaseURL = ""
    private val databaseReference = Firebase.database(databaseURL).reference.child("water_pump_status")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize internal device storage to remember whose phone this is
        val sharedPreferences = getSharedPreferences("AquaSyncPrefs", Context.MODE_PRIVATE)

        setContent {
            var isFirebaseAuthenticated by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
            var userName by remember { mutableStateOf(sharedPreferences.getString("user_name", "") ?: "") }
            var pumpState by remember { mutableStateOf(PumpState()) }
            var currentSystemTime by remember { mutableStateOf(System.currentTimeMillis()) }
            var isEditingName by remember { mutableStateOf(userName.isBlank()) }

            // 1. Authenticate securely in the background before running app listeners
            LaunchedEffect(Unit) {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    auth.signInAnonymously()
                        .addOnSuccessListener {
                            isFirebaseAuthenticated = true
                        }
                        .addOnFailureListener {
                            // Handle network failure or retry if necessary
                        }
                } else {
                    isFirebaseAuthenticated = true
                }
            }

            // 2. ACTIVE LISTENER: Only attaches AFTER successful authentication and when dashboard is active
            LaunchedEffect(isEditingName, isFirebaseAuthenticated) {
                if (!isEditingName && isFirebaseAuthenticated) {
                    databaseReference.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val isOn = snapshot.child("is_on").getValue(Boolean::class.java) ?: false
                            val startedAt = snapshot.child("started_at").getValue(Long::class.java) ?: 0L
                            val lastActionBy = snapshot.child("last_action_by").getValue(String::class.java) ?: ""
                            pumpState = PumpState(is_on = isOn, started_at = startedAt, last_action_by = lastActionBy)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database connection cancel errors here
                        }
                    })
                }
            }

            // 3. ACTIVE LOCAL TIMER: Steps up local timestamps every second only while running
            LaunchedEffect(pumpState.is_on) {
                if (pumpState.is_on) {
                    while (true) {
                        currentSystemTime = System.currentTimeMillis()
                        delay(1000)
                    }
                }
            }

            // Main UI Render Controller
            if (!isFirebaseAuthenticated) {
                // Display a loading state while checking security access tokens
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (isEditingName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NameOnboardingScreen(
                        initialName = userName,
                        onCancel = {
                            isEditingName = false
                        },
                        onNameSaved = { chosenName ->
                            sharedPreferences.edit().putString("user_name", chosenName).apply()
                            userName = chosenName
                            isEditingName = false
                        }
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PumpControlScreen(
                        pumpState = pumpState,
                        currentTime = currentSystemTime,
                        currentUser = userName,
                        onTogglePump = {
                            if (pumpState.is_on) {
                                databaseReference.setValue(PumpState(is_on = false, started_at = 0L, last_action_by = userName))
                            } else {
                                databaseReference.setValue(PumpState(is_on = true, started_at = System.currentTimeMillis(), last_action_by = userName))
                            }
                        },
                        onResetProfile = {
                            isEditingName = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NameOnboardingScreen(
    initialName: String,
    onCancel: () -> Unit,
    onNameSaved: (String) -> Unit
) {
    var textInput by remember { mutableStateOf(initialName) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Profile Settings", fontSize = 28.sp, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Change or set your device profile name:", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Enter your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onNameSaved(textInput.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Profile")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (initialName.isNotBlank()) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PumpControlScreen(
    pumpState: PumpState,
    currentTime: Long,
    currentUser: String,
    onTogglePump: () -> Unit,
    onResetProfile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (pumpState.is_on) "PUMP IS RUNNING" else "PUMP IS OFF",
            fontSize = 26.sp,
            style = MaterialTheme.typography.headlineMedium,
            color = if (pumpState.is_on) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val historyText = if (pumpState.is_on) {
            "Started by: ${pumpState.last_action_by}"
        } else if (pumpState.last_action_by.isNotEmpty()) {
            "Last stopped by: ${pumpState.last_action_by}"
        } else {
            "No recent activity logged"
        }

        Text(
            text = historyText,
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        val runningTimeString = if (pumpState.is_on && pumpState.started_at > 0L) {
            val totalSeconds = (currentTime - pumpState.started_at) / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        } else {
            "00:00"
        }

        Text(
            text = runningTimeString,
            fontSize = 64.sp,
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onTogglePump,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (pumpState.is_on) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
        ) {
            Text(
                text = if (pumpState.is_on) "STOP MOTOR" else "START MOTOR",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Logged in as $currentUser", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        TextButton(onClick = onResetProfile) {
            Text(text = "Change Name / Reset Profile", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
        }
    }
}