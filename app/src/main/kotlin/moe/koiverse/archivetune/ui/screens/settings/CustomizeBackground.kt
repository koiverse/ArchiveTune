package moe.koiverse.archivetune.ui.screens.settings

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.ui.player.PlayerPreview
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.PlayerCustomBrightnessKey
import moe.koiverse.archivetune.constants.PlayerCustomContrastKey
import moe.koiverse.archivetune.constants.PlayerCustomImageUriKey
import moe.koiverse.archivetune.constants.PlayerCustomBlurKey
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBackground(
    navController: NavController,
) {
    val context = LocalContext.current

    val (imageUri, onImageUriChange) = rememberPreference(PlayerCustomImageUriKey, "")
    val (blur, onBlurChange) = rememberPreference(PlayerCustomBlurKey, 0f)
    val (contrast, onContrastChange) = rememberPreference(PlayerCustomContrastKey, 1f)
    val (brightness, onBrightnessChange) = rememberPreference(PlayerCustomBrightnessKey, 1f)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Persist permission
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onImageUriChange(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customize_background_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    playerBackground = PlayerBackgroundStyle.CUSTOM,
                    imageUri = imageUri,
                    blur = blur,
                    contrast = contrast,
                    brightness = brightness,
                )

                Button(onClick = { launcher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_image))
                }

                Text(stringResource(R.string.blur))
                Slider(
                    value = blur,
                    onValueChange = onBlurChange,
                    valueRange = 0f..50f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.contrast))
                Slider(
                    value = contrast,
                    onValueChange = onContrastChange,
                    valueRange = 0.5f..2f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.brightness))
                Slider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.5f..2f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                FilledTonalButton(
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.save), Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    )
}
