package com.socialapp.ui.training

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialapp.utils.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Cấu trúc dữ liệu bài tập
data class Exercise(
    val name: String,
    val sets: String,
    val reps: String,
    val note: String = ""
)

// Cấu trúc dữ liệu lịch tập
data class WorkoutPlan(
    val title: String,
    val desc: String,
    val exercises: List<Exercise>,
    val type: String // "Muscle" hoặc "Daily"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(t("muscle_plans"), t("daily_routines"), t("ai_personal_plan"))

    // Trạng thái lưu lịch tập đang được chọn để xem chi tiết
    var selectedPlan by remember { mutableStateOf<WorkoutPlan?>(null) }

    // Xử lý nút quay lại của hệ thống Android
    BackHandler(enabled = selectedPlan != null) {
        selectedPlan = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedPlan == null) t("training_center") else selectedPlan!!.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (selectedPlan != null) {
                        IconButton(onClick = { selectedPlan = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedPlan == null) {
                // Hiển thị danh sách các Tabs
                Column(modifier = Modifier.fillMaxSize()) {
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
                        0 -> MusclePlansSection { selectedPlan = it }
                        1 -> DailyRoutinesSection { selectedPlan = it }
                        2 -> AIPersonalPlanSection()
                    }
                }
            } else {
                // Hiển thị giao diện chi tiết khi đã chọn 1 Plan
                WorkoutDetailSection(selectedPlan!!) {
                    selectedPlan = null
                }
            }
        }
    }
}

@Composable
fun MusclePlansSection(onPlanClick: (WorkoutPlan) -> Unit) {
    val plans = listOf(
        WorkoutPlan(
            title = "Hypertrophy Max (Monthly)",
            desc = "Chương trình tăng cơ cường độ cao trong 4 tuần.",
            type = "Muscle",
            exercises = listOf(
                Exercise("Bench Press", "4", "8-12", "Tập trung hạ tạ chậm"),
                Exercise("Incline Dumbbell Press", "3", "10-12"),
                Exercise("Cable Fly", "3", "15", "Gồng chặt cơ ngực ở điểm cao nhất"),
                Exercise("Tricep Pushdown", "4", "12-15")
            )
        ),
        WorkoutPlan(
            title = "Strength Foundation (Yearly)",
            desc = "Hướng dẫn dài hạn để tăng sức mạnh 1RM.",
            type = "Muscle",
            exercises = listOf(
                Exercise("Back Squat", "5", "5", "Sử dụng mức tạ nặng"),
                Exercise("Deadlift", "3", "5"),
                Exercise("Overhead Press", "5", "5"),
                Exercise("Pull Ups", "4", "Đến khi mỏi")
            )
        ),
        WorkoutPlan(
            title = "Quick Pump (Daily)",
            desc = "Buổi tập tay nhanh gọn để bơm máu vào cơ bắp.",
            type = "Muscle",
            exercises = listOf(
                Exercise("Bicep Curls", "4", "12"),
                Exercise("Hammer Curls", "3", "12"),
                Exercise("Skull Crushers", "4", "10"),
                Exercise("Dips", "3", "Đến khi mỏi")
            )
        )
    )

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(plans) { plan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onPlanClick(plan) },
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
                        Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(plan.desc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyRoutinesSection(onPlanClick: (WorkoutPlan) -> Unit) {
    val routines = listOf(
        WorkoutPlan(
            title = "Thứ 2: Ngực & Tay sau",
            desc = "4 bài tập, 3 hiệp mỗi bài. Thời gian: 45 phút.",
            type = "Daily",
            exercises = listOf(
                Exercise("Flat Bench Press", "3", "10"),
                Exercise("Incline DB Press", "3", "12"),
                Exercise("Pushups", "3", "20"),
                Exercise("Rope Pushdowns", "3", "15")
            )
        ),
        WorkoutPlan(
            title = "Thứ 3: Lưng & Tay trước",
            desc = "Tập trung vào các động tác kéo. Thời gian: 50 phút.",
            type = "Daily",
            exercises = listOf(
                Exercise("Lat Pulldowns", "3", "12"),
                Exercise("Seated Rows", "3", "12"),
                Exercise("Single Arm Rows", "3", "10"),
                Exercise("EZ Bar Curls", "3", "12")
            )
        ),
        WorkoutPlan(
            title = "Thứ 4: Nghỉ ngơi tích cực",
            desc = "Cardio nhẹ nhàng và giãn cơ. Thời gian: 30 phút.",
            type = "Daily",
            exercises = listOf(
                Exercise("Walking", "1", "30 phút"),
                Exercise("Yoga Stretching", "1", "15 phút"),
                Exercise("Plank", "3", "1 phút")
            )
        )
    )

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(routines) { plan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onPlanClick(plan) },
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
                        Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(plan.desc, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDetailSection(plan: WorkoutPlan, onDone: () -> Unit) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val sectionHeader = if (plan.type == "Muscle") t("muscle_plans") else t("daily_routines")
            Text(
                text = sectionHeader,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
        }

        items(plan.exercises) { exercise ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                exercise.sets,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (exercise.note.isNotBlank()) {
                            Text(
                                text = exercise.note,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Reps",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = exercise.reps,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    Toast.makeText(context, "Chúc mừng! Bạn đã hoàn thành buổi tập.", Toast.LENGTH_SHORT).show()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text(t("done"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
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
                t("AI person plan"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                t("AI plan") ?: "HLV AI sẽ thiết kế lịch tập dựa trên chỉ số PR của bạn.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(2000)
                        generatedPlan = "1. Khởi động (10 phút)\n2. Barbell Squats 4x8\n3. Leg Press 3x10\n4. Lying Leg Curls 3x12\n5. Calf Raises 4x15\n\nMẹo AI: Hãy hít thở đều và gồng core khi tập Squat!"
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(t("Generate AI plan"))
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(t("AI suggested plan") ?: "Lịch tập đề xuất từ AI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = generatedPlan!!,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { generatedPlan = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(t("Regenerate") ?: "Tạo lại")
                    }
                }
            }
        }
    }
}