package com.souschef.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Paper = Color(0xFFFAF5EB)
private val Ink = Color(0xFF211C18)
private val InkMuted = Color(0xFF6B5F55)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var probe by remember { mutableStateOf("Loading Python…") }

            LaunchedEffect(Unit) {
                probe = withContext(Dispatchers.IO) {
                    runCatching {
                        Python.getInstance()
                            .getModule("souschef.extract")
                            .callAttr("probe")
                            .toString()
                    }.getOrElse { "probe failed: $it" }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Paper)
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sous Chef", color = Ink, fontSize = 32.sp)
                Spacer(Modifier.height(12.dp))
                Text(probe, color = InkMuted, textAlign = TextAlign.Center)
            }
        }
    }
}
