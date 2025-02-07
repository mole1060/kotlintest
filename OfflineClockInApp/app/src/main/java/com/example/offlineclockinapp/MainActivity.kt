package com.example.offlineclockinapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.offlineclockinapp.ui.theme.OfflineClockInAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfflineClockInAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClockInApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ClockInApp(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var records by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("请输入姓名") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val currentTime = dateFormat.format(Date())
                records += "${name.text} 上班时间: $currentTime\n"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("上班")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val currentTime = dateFormat.format(Date())
                records += "${name.text} 下班时间: $currentTime\n"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("下班")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = records,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ClockInAppPreview() {
    OfflineClockInAppTheme {
        ClockInApp()
    }
}

