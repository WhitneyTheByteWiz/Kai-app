package com.kai.assistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import com.kai.assistant.voice.TTSHelper

class MainActivity : ComponentActivity() {
    private val ttsHelper by lazy { TTSHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsHelper.initialize { /* TTS ready */ }
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KaiScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaiScreen() {
    val activity = LocalContext.current as android.app.Activity
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var assistantState by remember { mutableStateOf(AssistantState.IDLE) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Top bar with state indicator
        TopAppBar(
            title = { Text("Kai", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            actions = {
                AssistantStateIndicator(state = assistantState)
                IconButton(onClick = { /* Settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Conversation area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userMsg = Message(inputText, false, System.currentTimeMillis())
                        messages = messages + userMsg
                        inputText = ""
                        // TODO: Send to AI
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    // TODO: Start voice input using activity
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = if (assistantState == AssistantState.LISTENING) Color.Green else Color.Unspecified
                )
            }
        }
    }
}

@Composable
fun AssistantStateIndicator(state: AssistantState) {
    val (color, label) = when (state) {
        AssistantState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to "Ready"
        AssistantState.LISTENING -> Color.Green to "Listening…"
        AssistantState.THINKING -> MaterialTheme.colorScheme.primary to "Thinking…"
        AssistantState.EXECUTING -> MaterialTheme.colorScheme.secondary to "Executing…"
        AssistantState.SPEAKING -> Color.Cyan to "Speaking…"
        AssistantState.ERROR -> Color.Red to "Error"
        AssistantState.CONFIRMING -> Color(0xFFFF9800) to "Confirm?"
    }
    Row(
        modifier = Modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(8.dp)
        ) {
            drawCircle(color = color, radius = 4.dp.toPx())
        }
        Text(label, fontSize = 12.sp, color = color)
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .widthIn(min = 0.dp, max = 300.dp)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp
            )
        }
    }
}

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)

enum class AssistantState {
    IDLE, LISTENING, THINKING, EXECUTING, SPEAKING, ERROR, CONFIRMING
}