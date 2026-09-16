package com.jarvis.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.UiState
import com.jarvis.ai.ui.components.HudOrb

@Composable
fun OnboardingScreen(state: UiState, vm: JarvisViewModel) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HudOrb(com.jarvis.ai.core.AssistantState.Idle)
        Text("JARVIS", style = MaterialTheme.typography.displaySmall)
        Text("رفيقك الصوتي مع JARVIS", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(26.dp))
        OutlinedTextField(state.draftName, vm::onNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("اسمك") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.draftWakePhrase,
            onValueChange = vm::onWakePhraseChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة الإيقاظ") },
            supportingText = { Text("الموديل المضمن يدعم حاليًا: Hey Jarvis") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(state.draftTitle, vm::onTitleChange, modifier = Modifier.fillMaxWidth(), label = { Text("طريقة المخاطبة") }, singleLine = true)
        Spacer(Modifier.height(20.dp))
        Button(onClick = vm::finishOnboarding, enabled = state.draftName.isNotBlank() && state.draftWakePhrase.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("ابدأ مع JARVIS")
        }
    }
}
