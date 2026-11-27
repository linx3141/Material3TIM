package com.material3tim.ui.screen.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.material3tim.BuildConfig
import com.material3tim.R
import com.material3tim.data.repository.LiveProfile
import com.material3tim.telegram.TelegramLibraries
import com.material3tim.telegram.TelegramSyncActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberStickerAccount(): String? {
    val host = LocalContext.current.packageName == "com.tencent.tim"
    val profile by LiveProfile.flow.collectAsState()
    return if (host) profile?.timId?.takeIf { it.isNotBlank() } else "demo"
}

@Composable
internal fun rememberTelegramSyncAction(): () -> Unit {
    val context = LocalContext.current
    val account = rememberStickerAccount()
    var requestedAccount by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val target = requestedAccount
        requestedAccount = null
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && target != null &&
            uri.scheme == "content" && uri.authority == "${BuildConfig.APPLICATION_ID}.stickers") {
            importing = true
            Toast.makeText(context, context.getString(R.string.tg_importing), Toast.LENGTH_SHORT).show()
            scope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                            TelegramLibraries.install(input, TelegramLibraries.root(context, target))
                        }
                    }
                    Toast.makeText(context, context.getString(R.string.tg_done, count), Toast.LENGTH_LONG).show()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.tg_failed), Toast.LENGTH_LONG).show()
                } finally {
                    importing = false
                }
            }
        }
    }
    return {
        when {
            importing -> Toast.makeText(context, context.getString(R.string.tg_importing), Toast.LENGTH_SHORT).show()
            account == null -> Toast.makeText(context, context.getString(R.string.tg_account_missing), Toast.LENGTH_SHORT).show()
            requestedAccount == null -> {
                requestedAccount = account
                try {
                    launcher.launch(Intent().setClassName(BuildConfig.APPLICATION_ID, TelegramSyncActivity::class.java.name))
                } catch (_: android.content.ActivityNotFoundException) {
                    requestedAccount = null
                    Toast.makeText(context, context.getString(R.string.tg_companion_missing), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
