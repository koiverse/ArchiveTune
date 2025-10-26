package moe.koiverse.archivetune.ui.screens.settings

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
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
                // Two previews side-by-side: player overlay and lyrics overlay
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Player overlay preview
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri.isNotBlank()) {
                            val t = (1f - contrast) * 128f + (brightness - 1f) * 255f
                            val matrix = floatArrayOf(
                                contrast, 0f, 0f, 0f, t,
                                0f, contrast, 0f, 0f, t,
                                0f, 0f, contrast, 0f, t,
                                0f, 0f, 0f, 1f, 0f,
                            )
                            val cm = ColorMatrix(matrix)

                            AsyncImage(
                                model = Uri.parse(imageUri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(blur.dp),
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(cm)
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                            Image(
                                painter = painterResource(R.drawable.player_preview),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(R.drawable.image), contentDescription = null)
                                Text(stringResource(R.string.add_image))
                            }
                        }
                    }

                    // Lyrics overlay preview
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri.isNotBlank()) {
                            val t2 = (1f - contrast) * 128f + (brightness - 1f) * 255f
                            val matrix2 = floatArrayOf(
                                contrast, 0f, 0f, 0f, t2,
                                0f, contrast, 0f, 0f, t2,
                                0f, 0f, contrast, 0f, t2,
                                0f, 0f, 0f, 1f, 0f,
                            )
                            val cm2 = ColorMatrix(matrix2)

                            AsyncImage(
                                model = Uri.parse(imageUri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(blur.dp),
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(cm2)
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                            Image(
                                painter = painterResource(R.drawable.lyrics_preview),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(R.drawable.image), contentDescription = null)
                                Text(stringResource(R.string.add_image))
                            }
                        }
                    }
                }

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
