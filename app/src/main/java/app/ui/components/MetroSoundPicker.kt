package app.metroclock.ui.components

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.ui.animation.MetroTurnstileEntrance
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.LocalMetroBackground
import app.metroclock.ui.theme.LocalMetroDivider
import app.metroclock.ui.theme.LocalMetroSubtextColor
import app.metroclock.ui.theme.LocalMetroTextColor

/**
 * One selectable entry inside the in-app Metro sound picker.
 *
 * id:  "" = silent, "default" = system default sound, otherwise the ringtone uri string.
 * uri: playable uri (null for silent).
 */
data class SoundOption(
  val id: String,
  val title: String,
  val uri: String?
)

/**
 * In-app Metro-styled sound picker used for both alarm and timer sounds.
 * Replaces the system RingtoneManager.ACTION_RINGTONE_PICKER intent, which crashes
 * on some devices right after a sound is selected. Keeps the app aesthetic
 * (flat tiles, light typography, accent highlight) and lists the device's
 * alarm/ringtone/notification sounds plus Default and Silent options.
 */
@Composable
fun MetroSoundPickerDialog(
  title: String,
  options: List<SoundOption>,
  selectedId: String,
  onSelect: (SoundOption) -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler { onDismiss() }

  val context = LocalContext.current
  val bgColor = LocalMetroBackground.current
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val dividerColor = LocalMetroDivider.current
  val accentColor = LocalAccentColor.current

  // Launcher for picking a custom audio file from the system file explorer
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      // Try to persist the permission so the alarm can access the file later
      try {
        context.contentResolver.takePersistableUriPermission(
          it,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (e: SecurityException) {
        // Some providers don't support persistable permissions, ignore.
      }
      val displayName = getFileNameFromUri(context, it) ?: "Custom Sound"
      onSelect(SoundOption(id = it.toString(), title = displayName, uri = it.toString()))
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
      .statusBarsPadding()
      .testTag("metro_sound_picker")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header matching the app's category + page title style
      MetroTurnstileEntrance(delayMillis = 0) {
        Column(
          modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 12.dp)
        ) {
          Text(
            text = "METRO CLOCK",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = subtextColor
          )
          Text(
            text = title,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp,
            color = textColor
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        items(options, key = { it.id }) { option ->
          val isSelected = option.id == selectedId
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .background(if (isSelected) accentColor else Color.Transparent)
              .border(1.dp, if (isSelected) accentColor else dividerColor)
              .clickable {
                if (option.id == "add_new") {
                  // Launch the system file explorer for audio files
                  filePickerLauncher.launch("audio/*")
                } else {
                  onSelect(option)
                }
              }
              .padding(horizontal = 14.dp)
              .testTag("sound_option_${option.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = if (isSelected) Icons.Default.Check else if (option.id == "add_new") Icons.Default.Add else Icons.Default.MusicNote,
              contentDescription = null,
              tint = if (isSelected) Color.Black else if (option.id == "add_new") accentColor else subtextColor,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = option.title,
              fontSize = 17.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) Color.Black else if (option.id == "add_new") accentColor else textColor,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Bottom cancel action with padding so it clears the Android gesture bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(top = 8.dp, bottom = 16.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, subtextColor.copy(alpha = 0.5f))
            .clickable { onDismiss() }
            .testTag("btn_sound_picker_cancel"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "cancel",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
          )
        }
      }
    }
  }
}

/**
 * Builds the list of sounds shown in [MetroSoundPickerDialog]:
 * "Default alarm sound", every alarm/ringtone/notification tone installed on the device,
 * "Add new" for custom sounds, and "Silent". Titles are read via RingtoneManager (no intents involved).
 */
fun buildMetroSoundOptions(context: Context): List<SoundOption> {
  val options = mutableListOf(
    SoundOption(id = "default", title = "Default alarm sound", uri = null)
  )
  val seen = mutableSetOf<String>()

  // Query for all potential ringtone/alarm/notification sounds
  var cursor: Cursor? = null
  try {
    cursor = context.contentResolver.query(
      MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
      arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE),
      "(is_ringtone=1 OR is_alarm=1 OR is_notification=1)",
      null,
      "${MediaStore.Audio.Media._ID} ASC"
    ) ?: return options + SoundOption(id = "add_new", title = "Add new", uri = null) + SoundOption(id = "", title = "Silent", uri = null)

    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

    while (cursor.moveToNext()) {
      val id = cursor.getString(idCol) ?: continue
      val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Sound $id"
      val uriString = "content://media/internal/audio/media/$id"
      if (seen.add(uriString)) {
        options.add(SoundOption(id = uriString, title = title, uri = uriString))
      }
    }
  } catch (e: Exception) {
    e.printStackTrace()
  } finally {
    cursor?.close()
  }

  options.add(SoundOption(id = "add_new", title = "Add new", uri = null))
  options.add(SoundOption(id = "", title = "Silent", uri = null))
  return options
}

/**
 * Helper function to extract the display name from a content URI returned by the file picker.
 */
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
  var name: String? = null
  val cursor = context.contentResolver.query(uri, null, null, null, null)
  cursor?.use {
    if (it.moveToFirst()) {
      val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIndex != -1) {
        name = it.getString(nameIndex)
      }
    }
  }
  return name
}
