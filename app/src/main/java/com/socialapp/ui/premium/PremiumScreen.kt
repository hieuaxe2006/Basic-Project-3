package com.socialapp.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialapp.ui.home.FeedViewModel
import com.socialapp.utils.t

@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    vm: PremiumViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel()
) {
    val state = vm.state
    val isAlreadyPremium = feedViewModel.state.currentUser?.is_premium == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A1A), Color(0xFF000000))
                )
            )
    ) {
        if (state.isSuccess) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(100.dp), tint = Color(0xFFFFD700))
                Spacer(Modifier.height(24.dp))
                Text(t("success"), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Hội viên Premium đã kích hoạt.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(40.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                    Text(t("close"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }

                Icon(Icons.Default.WorkspacePremium, null, modifier = Modifier.size(80.dp), tint = Color(0xFFFFD700))
                Text("GymHub Premium", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700))
                Text(
                    if (isAlreadyPremium) "Premium Member" else "Bứt phá giới hạn của bạn",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(40.dp))

                BenefitRow("Unlimited Posts", "Share your journey without limits.")
                BenefitRow("AI Workout Coach", "Gemini AI designs plans based on your PRs.")
                BenefitRow("Golden Badge", "Stand out in the community.")
                BenefitRow("Ad-Free", "Smooth experience, focus on goals.")

                Spacer(Modifier.weight(1f))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isAlreadyPremium) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFFFFD700), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("ACTIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        } else {
                            Text("LIFETIME PASS", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                            Text("199.000đ", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { vm.upgrade() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !state.isLoading
                            ) {
                                if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black)
                                else Text("UPGRADE NOW", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenefitRow(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
    }
}