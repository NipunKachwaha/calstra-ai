package com.apoorvdarshan.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.ui.settings.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.AppColors

/**
 * "Camera + Note" intermediate sheet. Shows the just-captured photo and a
 * multiline text field for the user to add context (e.g. "no oil", "extra
 * cheese") before sending the image off to the AI. Mirrors iOS
 * ContextDescriptionSheet which gates `cameraMode == .snapFoodWithContext`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextNoteSheet(
    imageBytes: ByteArray,
    onAnalyze: (note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassTheme.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by remember { mutableStateOf("") }
    val bitmap = remember(imageBytes) {
        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop // Glass Theme UI Background
    ) {
        SheetReviewToolbar(
            title = "Add Note",
            primaryLabel = "Analyze",
            onCancel = onDismiss,
            onPrimary = { onAnalyze(note) }
        )

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            SheetSectionHeader("Add a note (optional)")

            // Glassmorphism Text Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text(
                        "e.g. no oil, extra cheese, large portion",
                        color = glass.textPrimary.copy(alpha = 0.4f)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = glass.textPrimary,
                    unfocusedTextColor = glass.textPrimary,
                    focusedContainerColor = glass.surface.copy(alpha = 0.3f),
                    unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
                    focusedBorderColor = AppColors.Calorie,
                    unfocusedBorderColor = glass.border,
                    cursorColor = AppColors.Calorie
                )
            )

            Spacer(Modifier.height(4.dp))

            // Sleek Gradient Analyze Button replacing default Material Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd)))
                    .clickable { onAnalyze(note) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Analyze", 
                    color = Color.White, 
                    fontSize = 17.sp, 
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}