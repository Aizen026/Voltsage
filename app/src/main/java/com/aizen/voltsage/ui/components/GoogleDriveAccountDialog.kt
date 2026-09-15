package com.aizen.voltsage.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aizen.voltsage.data.remote.DriveAccountInfo
import com.aizen.voltsage.data.remote.DrivePermissionException
import com.aizen.voltsage.ui.VoltSageViewModel
import com.aizen.voltsage.ui.theme.SuccessGreen
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltBlue
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltNeonBlue
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val APP_PACKAGE_NAME = "com.aizen.voltsage"
const val APP_CERT_SHA1 = "0A:89:60:AD:EE:1D:2F:6F:5F:B6:83:2B:CD:6F:D8:3B:13:B9:76:C9"
const val APP_CERT_SHA256 = "98:F4:AE:48:BD:30:11:48:E2:FC:6A:43:6C:7D:B0:2F:A6:40:1D:2B:7E:88:5C:13:5C:C6:4D:D6:97:83:84:5A"

/**
 * Parses Google Sign-In intent results and provides clear, actionable diagnostics
 * instead of hiding behind a generic cancellation message.
 */
fun extractGoogleSignInError(resultData: Intent?, fallbackMessage: String): String {
    if (resultData == null) {
        return fallbackMessage
    }
    val task = GoogleSignIn.getSignedInAccountFromIntent(resultData)
    return try {
        task.getResult(ApiException::class.java)
        "Sign-in succeeded."
    } catch (e: ApiException) {
        when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR -> { // 10
                "Google Sign-In Error 10 (DEVELOPER_ERROR): This APK's SHA-1 certificate is not yet registered in Google Cloud / Firebase Console. To enable direct 1-tap Google Sign-In, add this SHA-1 to your Cloud Console OAuth Client. In the meantime, use 'Universal File Backup' below to save & restore directly with your Google Drive app!"
            }
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> { // 12500
                "Google Sign-In Error 12500 (SIGN_IN_FAILED): Google Play Services could not complete authentication. Please verify Google Play Services is updated, or use 'Universal File Backup' below to save directly to Google Drive."
            }
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> { // 12501
                "Sign-in was cancelled."
            }
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> { // 12502
                "Sign-in is currently in progress. Please wait a moment."
            }
            CommonStatusCodes.NETWORK_ERROR -> { // 7
                "Network error: Please verify your internet connection and try again."
            }
            else -> {
                "Google Sign-In failed (Code ${e.statusCode}): ${e.localizedMessage ?: "Please try again or use the file backup option below."}"
            }
        }
    } catch (e: Exception) {
        "Sign-in error: ${e.localizedMessage ?: fallbackMessage}"
    }
}

@Composable
fun GoogleDriveAccountDialog(
    viewModel: VoltSageViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveManager = viewModel.driveManager

    var accountInfo by remember { mutableStateOf(driveManager.getAccountInfo()) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    // Google Sign-In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            accountInfo = driveManager.getAccountInfo()
            val hasDrive = driveManager.hasDrivePermission(account)
            statusMessage = if (hasDrive) {
                "Signed in as ${account.email}. Starting auto-backup..."
            } else {
                "Signed in as ${account.email}. Tap 'Authorize' to grant Google Drive backup permission."
            }
            
            if (hasDrive) {
                // Automatically trigger a backup after signing in with drive permission
                scope.launch {
                    isBackingUp = true
                    val res = driveManager.uploadBackupToDrive()
                    isBackingUp = false
                    res.onSuccess {
                        accountInfo = driveManager.getAccountInfo()
                        statusMessage = "Auto-backup complete: $it"
                    }.onFailure {
                        statusMessage = "Auto-backup failed: ${it.localizedMessage}"
                    }
                }
            }
        } catch (e: Exception) {
            statusMessage = extractGoogleSignInError(
                result.data,
                "Sign-in was cancelled or could not be completed."
            )
        }
    }

    // Direct Google Drive / Local File Export Launcher (SAF)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isBackingUp = true
                statusMessage = "Exporting backup file..."
                val res = driveManager.exportBackupToUri(uri)
                isBackingUp = false
                res.onSuccess {
                    accountInfo = driveManager.getAccountInfo()
                    statusMessage = it
                }.onFailure {
                    statusMessage = "Export failed: ${it.localizedMessage}"
                }
            }
        }
    }

    // Direct Google Drive / Local File Import Launcher (SAF)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isRestoring = true
                statusMessage = "Restoring from selected file..."
                val res = driveManager.restoreBackupFromUri(uri)
                isRestoring = false
                res.onSuccess {
                    statusMessage = it
                }.onFailure {
                    statusMessage = "Restore failed: ${it.localizedMessage}"
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VoltCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Google Drive",
                                tint = VoltCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Account & Cloud Sync",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Google Drive & Local Storage",
                                fontSize = 11.sp,
                                color = VoltAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Account Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (accountInfo.isSignedIn) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = accountInfo.displayName ?: "Connected Account",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = accountInfo.email ?: "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        driveManager.signOut {
                                            accountInfo = driveManager.getAccountInfo()
                                            statusMessage = "Signed out."
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Text("Sign Out", fontSize = 11.sp)
                                }
                            }

                            if (!accountInfo.hasDrivePermission) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = VoltAmber.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, VoltAmber.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = VoltAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Drive permission needed for cloud backups",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                val client = driveManager.getGoogleSignInClient(requestDriveScope = true)
                                                signInLauncher.launch(client.signInIntent)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VoltAmber)
                                        ) {
                                            Text("Authorize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (accountInfo.lastBackupTimestamp > 0L) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val formattedDate = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault())
                                    .format(Date(accountInfo.lastBackupTimestamp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Last backup: $formattedDate",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = VoltAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Google Account (Optional)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Connect Google account or use direct file backups below",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val client = driveManager.getGoogleSignInClient(requestDriveScope = true)
                                    signInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_sign_in_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Direct Google Drive API Backup & Restore (When account is connected)
                if (accountInfo.isSignedIn) {
                    Text(
                        text = "DIRECT GOOGLE DRIVE SYNC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!accountInfo.hasDrivePermission) {
                                    statusMessage = "Requesting Google Drive permission..."
                                    val client = driveManager.getGoogleSignInClient(requestDriveScope = true)
                                    signInLauncher.launch(client.signInIntent)
                                    return@Button
                                }
                                scope.launch {
                                    isBackingUp = true
                                    statusMessage = "Uploading backup to Google Drive..."
                                    val res = driveManager.uploadBackupToDrive()
                                    isBackingUp = false
                                    res.onSuccess {
                                        accountInfo = driveManager.getAccountInfo()
                                        statusMessage = it
                                    }.onFailure { err ->
                                        if (err is DrivePermissionException) {
                                            val client = driveManager.getGoogleSignInClient(requestDriveScope = true)
                                            signInLauncher.launch(client.signInIntent)
                                        }
                                        statusMessage = "Drive upload error: ${err.localizedMessage}"
                                    }
                                }
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_to_drive_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltBlue)
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saving...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (!accountInfo.hasDrivePermission) {
                                    statusMessage = "Requesting Google Drive permission..."
                                    val client = driveManager.getGoogleSignInClient(requestDriveScope = true)
                                    signInLauncher.launch(client.signInIntent)
                                    return@OutlinedButton
                                }
                                showRestoreConfirm = true
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_drive_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restoring...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Universal File Backup (Works with Google Drive app, Files, SD Card, etc.)
                Text(
                    text = "UNIVERSAL BACKUP & RESTORE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Export and import directly into your Google Drive folder or internal storage via Android's native file picker. Works 100% reliably on all devices without requiring cloud console setup.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("voltsage_backup_$timestamp.json")
                        },
                        enabled = !isBackingUp && !isRestoring,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_file_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        enabled = !isBackingUp && !isRestoring,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_file_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Status message
                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (statusMessage!!.contains("failed", ignoreCase = true) || statusMessage!!.contains("error", ignoreCase = true)) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            } else {
                                SuccessGreen.copy(alpha = 0.15f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (statusMessage!!.contains("failed", ignoreCase = true) || statusMessage!!.contains("error", ignoreCase = true)) {
                                    Icons.Default.Info
                                } else {
                                    Icons.Default.CloudDone
                                },
                                contentDescription = null,
                                tint = if (statusMessage!!.contains("failed", ignoreCase = true) || statusMessage!!.contains("error", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    SuccessGreen
                                },
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage!!,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = VoltCyan)
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from Google Drive", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will download your saved study packs and study squad invites from your Google Drive backup. Existing notes won't be duplicated.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        scope.launch {
                            isRestoring = true
                            statusMessage = "Downloading backup from Google Drive..."
                            val res = driveManager.restoreBackupFromDrive()
                            isRestoring = false
                            res.onSuccess {
                                statusMessage = it
                            }.onFailure {
                                statusMessage = "Restore error: ${it.localizedMessage}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                ) {
                    Text("Proceed Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GoogleSignInStartDialog(
    viewModel: VoltSageViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val driveManager = viewModel.driveManager
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Toast.makeText(context, "Welcome, ${account.displayName ?: account.email}!", Toast.LENGTH_SHORT).show()
            viewModel.onGoogleSignInSuccess()
        } catch (e: Exception) {
            errorMessage = extractGoogleSignInError(
                result.data,
                "Sign-in could not be completed. You can continue with local storage."
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(26.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Brand Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VoltCyan.copy(alpha = 0.2f), VoltAmber.copy(alpha = 0.2f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Backup",
                        tint = VoltCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to VoltSage",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign in to backup your study packs & active recall progress, or continue with private local storage.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Local Storage & Privacy Guarantee Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy Shield",
                            tint = SuccessGreen,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "100% Private & Offline Ready",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "All flashcards, quizzes, AI notes, and study squads work directly on your device. Sign-in is optional.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Google Sign In Button
                Button(
                    onClick = {
                        isSigningIn = true
                        errorMessage = null
                        val client = driveManager.getGoogleSignInClient(requestDriveScope = true)
                        signInLauncher.launch(client.signInIntent)
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_google_sign_in_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...", fontSize = 14.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign In with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Continue as Guest / Local storage Button
                Button(
                    onClick = { viewModel.dismissGoogleSignInPrompt() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("start_continue_guest_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Continue with Local Storage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
