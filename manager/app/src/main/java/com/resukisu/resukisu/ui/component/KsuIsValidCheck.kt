package com.vortexsu.vortexsu.ui.component

import androidx.compose.runtime.Composable
import com.vortexsu.vortexsu.Natives
import com.vortexsu.vortexsu.ksuApp

@Composable
fun KsuIsValid(
    content: @Composable () -> Unit
) {
    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    if (ksuVersion != null) {
        content()
    }
}