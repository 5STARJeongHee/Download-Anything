// 계정 연결 화면: 플랫폼별 연결 상태, 앱 안 로그인, cookies.txt 가져오기, 연결 해제
package com.downloadanything.ui.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloadanything.DownloadAnythingApp
import com.downloadanything.data.account.AccountStore
import com.downloadanything.domain.AccountCatalog
import com.downloadanything.domain.AccountCookies
import com.downloadanything.domain.AccountSpec
import com.downloadanything.domain.model.Platform
import com.downloadanything.ui.label
import com.downloadanything.ui.theme.DownloadAnythingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class AccountsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = (application as DownloadAnythingApp).accountStore
        setContent {
            DownloadAnythingTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AccountsScreen(store, onClose = ::finish, modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, AccountsActivity::class.java)
    }
}

/** 실패 화면 등에서 부른다. 앱 안 로그인이 되면 바로 로그인 화면, 아니면 계정 연결 화면을 연다. */
fun openAccountConnect(context: Context, platform: Platform) {
    val spec = AccountCatalog.spec(platform)
    val intent = if (spec?.loginUrl != null) AccountLoginActivity.intent(context, platform) else AccountsActivity.intent(context)
    context.startActivity(intent)
}

private const val MAX_IMPORT_BYTES = 1_000_000

@Composable
private fun AccountsScreen(store: AccountStore, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connected by store.connected.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    var importTarget by remember { mutableStateOf<AccountSpec?>(null) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val spec = importTarget ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { message = importCookies(context, store, spec, uri) }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("계정 연결", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text(
            "로그인해야 보이는 콘텐츠(연령 제한, 민감한 콘텐츠, 비공개 계정 등)를 받을 때만 필요합니다. " +
                "연결한 플랫폼의 링크에만 로그인 정보(쿠키)를 쓰며, 이 기기 안에만 저장합니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        AccountCatalog.specs.forEach { spec ->
            AccountRow(
                spec = spec,
                isConnected = spec.platform in connected,
                onLogin = { context.startActivity(AccountLoginActivity.intent(context, spec.platform)) },
                onImport = {
                    importTarget = spec
                    importer.launch(arrayOf("text/*", "application/octet-stream"))
                },
                onDisconnect = {
                    store.disconnect(spec.platform)
                    WebCookies.clear(spec)
                    message = "${spec.platform.label()} 연결을 해제했습니다."
                },
            )
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
        Text(
            "cookies.txt는 PC 브라우저에서 해당 사이트에 로그인한 뒤 쿠키 내보내기 확장 프로그램(Netscape 형식)으로 만듭니다. " +
                "쿠키는 비밀번호와 같으니 다른 사람에게 보내지 마세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        TextButton(onClick = onClose) { Text("닫기") }
    }
}

@Composable
private fun AccountRow(
    spec: AccountSpec,
    isConnected: Boolean,
    onLogin: () -> Unit,
    onImport: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(spec.platform.label(), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                if (isConnected) "연결됨" else "연결 안 됨",
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        if (spec.loginUrl == null) {
            Text("Google 계정은 앱 안 로그인이 막혀 있어 cookies.txt로만 연결합니다.", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (spec.loginUrl != null) Button(onClick = onLogin) { Text(if (isConnected) "다시 로그인" else "로그인") }
            OutlinedButton(onClick = onImport) { Text("cookies.txt") }
            if (isConnected) TextButton(onClick = onDisconnect) { Text("해제") }
        }
    }
}

private suspend fun importCookies(context: Context, store: AccountStore, spec: AccountSpec, uri: Uri): String {
    val label = spec.platform.label()
    val text = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> readLimited(input, MAX_IMPORT_BYTES) }
        }.getOrNull()
    } ?: return "파일을 읽지 못했습니다. 1MB 이하의 cookies.txt 파일을 골라 주세요."
    val cookies = AccountCookies.fromImportedFile(spec, text)
        ?: return "$label 로그인 쿠키를 찾지 못했습니다. $label 에 로그인한 상태에서 내보낸 Netscape 형식 파일인지 확인해 주세요."
    withContext(Dispatchers.IO) { store.save(spec.platform, cookies) }
    return "$label 계정을 연결했습니다."
}

/** 최대 [limit]바이트까지 읽는다. 더 크면 잘못 고른 파일로 보고 null. */
private fun readLimited(input: InputStream, limit: Int): String? {
    val buffer = ByteArray(limit + 1)
    var total = 0
    while (total < buffer.size) {
        val read = input.read(buffer, total, buffer.size - total)
        if (read < 0) break
        total += read
    }
    return if (total > limit) null else buffer.decodeToString(0, total)
}
