package com.socialapp.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Muscle Plans", "Daily Routines", "AI Personal Plan")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Center", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> MusclePlansSection()
                1 -> DailyRoutinesSection()
                2 -> AIPersonalPlanSection()
            }
        }
    }
}

@Composable
fun MusclePlansSection() {
    val plans = listOf(
        "Hypertrophy Max (Monthly)" to "Intense muscle building program over 4 weeks.",
        "Strength Foundation (Yearly)" to "A long-term guide to increasing 1RM.",
        "Quick Pump (Daily)" to "Get the blood flowing with this quick arm workout."
    )
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(plans) { (title, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyRoutinesSection() {
    val routines = listOf(
        "Monday: Chest & Triceps" to "4 exercises, 3 sets each. 45 mins.",
        "Tuesday: Back & Biceps" to "Focus on pull movements. 50 mins.",
        "Wednesday: Active Rest" to "Light cardio and stretching. 30 mins."
    )
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(routines) { (title, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AIPersonalPlanSection() {
    var isLoading by remember { mutableStateOf(false) }
    var generatedPlan by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (generatedPlan == null) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tạo Lịch Tập AI Cá Nhân Hóa",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "AI sẽ dựa vào các chỉ số Gymer của bạn ở Profile để tạo ra lộ trình tập luyện hiệu quả nhất.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(2000) // Simulate AI generation
                        generatedPlan = "1. Warming up (10 mins)\n2. Barbell Squats 4x8\n3. Leg Press 3x10\n4. Lying Leg Curls 3x12\n5. Calf Raises 4x15\n\nAI Tip: Remember to brace your core during squats!"
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Tạo Lịch Tập Ngay")
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Lịch tập đề xuất từ AI:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(generatedPlan!!, fontSize = 16.sp, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { generatedPlan = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Tạo Lại")
                    }
                }
            }
        }
    }
}
