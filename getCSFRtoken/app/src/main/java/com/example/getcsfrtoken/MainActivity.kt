package com.example.getcsfrtoken

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // 确保正确导入
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import com.example.getcsfrtoken.ui.theme.GetCSFRtokenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GetCSFRtokenTheme {
                MyApp()
                //LoginScreen()
            }
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()

    // 设置导航主机
    NavHost(navController = navController, startDestination = "home") {
        // 定义首页
        composable("home") { HomeScreen(navController) }

        // 定义详情页
        composable("details") { DetailsScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold { paddingValues -> // 通过 paddingValues 获取 contentPadding
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(text = "登入取得更新資料")
            Button(onClick = { navController.navigate("details") }) {
                Text("前往打卡頁面")
            }
            LoginScreen()
        }
    }
}

@Composable
fun DetailsScreen(navController: NavController) {
    Scaffold { paddingValues -> // 通过 paddingValues 获取 contentPadding
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(text = "打卡")
            Button(onClick = { navController.popBackStack() }) {
                Text("返回首頁")
            }
            AttendanceScreen()
        }
    }
}

@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginResponse by remember { mutableStateOf("請輸入帳號和密碼後點擊登入") }
    var isLoggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("帳號") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密碼") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    loginResponse = "登入中..."
                    val response = login(username, password)
                    loginResponse = response ?: "登入失敗"
                    isLoggingIn = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoggingIn // 在登入過程中禁用按鈕
        ) {
            Text("登入")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = loginResponse)
    }
}

// 使用 OkHttp 庫進行網路請求
private suspend fun login(username: String, password: String): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val loginUrl = "https://fisherman2.toff.best/api/login"  // 替換為真實的API地址
/*
        // Step 1: 取得 Cookies
        val cookieRequest = Request.Builder()
            .url("https://fisherman.toff.best/sanctum/csrf-cookie")  // 替換為取得 cookies 的 API 地址
            .build()

        val cookieResponse = try {
            client.newCall(cookieRequest).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext "無法獲取 Cookies：${e.message}"
        }

        val cookies = cookieResponse.headers("Set-Cookie")
        if (cookies.isEmpty()) {
            return@withContext "無法獲取 Cookies"
        }*/

        // Step 2: 使用 Cookies 進行登入
        val formBody = FormBody.Builder()
            .add("email", username)
            .add("password", password)
            .build()

        val loginRequest = Request.Builder()
            .url(loginUrl)
            .post(formBody)
           // .header("Cookie", cookies.joinToString("; "))
            .build()

        val loginResponse = try {
            client.newCall(loginRequest).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext "登入請求失敗：${e.message}"
        }

        // Step 3: 顯示登入結果
        if (loginResponse.isSuccessful) {
            loginResponse.body?.string()?.let {
                return@withContext try {
                    val jsonObject = JSONObject(it)
                    "登入成功！返回資料：\n$jsonObject"
                } catch (e: Exception) {
                    "無法解析 JSON: ${e.message}"
                }
            }
        } else {
            return@withContext "登入失敗：${loginResponse.message}"
        }
    }
}


data class Person(val name: String, var isWorking: Boolean)
data class Record(val name: String, val newState: String, val timestamp: String)

@Composable
fun AttendanceScreen() {
    var people by remember {
        mutableStateOf(
            listOf(
                Person("Alice", true),
                Person("Bob", false),
                Person("Charlie", true)
            )
        )
    }

    var records by remember { mutableStateOf(listOf<Record>()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("上班區", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            items(people.filter { it.isWorking }) { person ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = person.name)
                    Button(onClick = {
                        val updatedState = !person.isWorking
                        people = people.map {
                            if (it.name == person.name) it.copy(isWorking = updatedState) else it
                        }
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        records += Record(person.name, if (updatedState) "上班" else "下班", timestamp)
                    }) {/*
                        // 切换单个人的状态
                        val updatedState = !person.isWorking
                        people = people.map {
                            if (it.name == person.name) it.copy(isWorking = updatedState) else it
                        }
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        records = records + Record(person.name, if (updatedState) "上班" else "下班", timestamp)
                    }) {*/
                        Text("下班打卡")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("下班區", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            items(people.filter { !it.isWorking }) { person ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = person.name)
                    Button(onClick = {
                        val updatedState = !person.isWorking
                        people = people.map {
                            if (it.name == person.name) it.copy(isWorking = updatedState) else it
                        }
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        records += Record(person.name, if (updatedState) "上班" else "下班", timestamp)
                    }) {/*
                        // 切换单个人的状态
                        val updatedState = !person.isWorking
                        people = people.map {
                            if (it.name == person.name) it.copy(isWorking = updatedState) else it
                        }
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        records = records + Record(person.name, if (updatedState) "上班" else "下班", timestamp)
                    }) {*/
                        Text("上班打卡")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("切換記錄", style = MaterialTheme.typography.headlineMedium)

        // 切换记录 LazyColumn，带有可滚动功能
        Box(
            modifier = Modifier
                .weight(2f)
                .padding(8.dp)
        ) {
            val scrollState = rememberScrollState()
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                items(records) { record ->
                    Text(text = "${record.timestamp} - ${record.name} 打卡 ${record.newState}")
                }
            }
        }
    }
}


/* 滚动提示图标
@Composable
fun ScrollHintIcon() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_search),  // 使用系统图标，或自定义
            contentDescription = "滚动提示",
            modifier = Modifier
                .size(32.dp)
                .clickable { /* Do something on click */ },  // 可以添加点击事件
            tint = Color.Gray
        )
    }
}

 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GetCSFRtokenTheme {
        LoginScreen()
    }
}