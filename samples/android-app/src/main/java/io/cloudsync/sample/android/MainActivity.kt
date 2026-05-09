package io.cloudsync.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cloudsync.engine.CloudSync
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CloudSyncSample()
                }
            }
        }
    }
}

@Composable
fun CloudSyncSample() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Not initialized") }
    var engine = remember { CloudSync.configure("{\"configName\":\"android-sample\",\"serverUrl\":\"https://api.example.com\"}") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "CloudSync Engine Sample",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Status: $status",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Button(onClick = {
            scope.launch {
                status = "Initializing..."
                engine.initialize("{\"configName\":\"android-sample\",\"serverUrl\":\"https://api.example.com\"}")
                status = "Starting..."
                engine.start()
                status = "Started! State: ${engine.syncState.value}"
            }
        }) {
            Text("Start Engine")
        }

        Button(onClick = {
            scope.launch {
                status = "Syncing..."
                engine.syncNow()
                status = "Sync triggered! State: ${engine.syncState.value}"
            }
        }) {
            Text("Sync Now")
        }

        Button(onClick = {
            scope.launch {
                engine.stop()
                status = "Stopped"
            }
        }) {
            Text("Stop Engine")
        }
    }
}
