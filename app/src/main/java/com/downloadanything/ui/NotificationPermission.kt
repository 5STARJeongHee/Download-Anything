// Android 13+에서 다운로드 알림 권한을 한 번 요청한다
package com.downloadanything.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    // 거부해도 다운로드는 된다. 진행·완료 알림만 보이지 않는다.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!isGranted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
