package moe.koiverse.archivetune.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarDialog(
    onDismissRequest: () -> Unit,
    onStar: () -> Unit,
    onLater: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(id = R.string.star_dialog_title), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.star_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/koiverse/ArchiveTune"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                    onStar()
                },
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.star),
                    contentDescription = stringResource(id = R.string.star_dialog_star),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.star_dialog_star))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(text = stringResource(id = R.string.star_dialog_later))
            }
        }
    )
}
