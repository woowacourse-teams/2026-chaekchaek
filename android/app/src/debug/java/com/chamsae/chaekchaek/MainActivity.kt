package com.chamsae.chaekchaek

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

class MainActivity : ChaekchaekActivity() {
    override val analyticsEnvironment = "test"

    @Composable
    override fun HomeHeaderAction() {
        val context = LocalContext.current
        DeveloperToolsEntryButton(
            onClick = {
                context.startActivity(Intent(context, DeveloperToolsActivity::class.java))
            },
        )
    }
}

@Composable
private fun DeveloperToolsEntryButton(onClick: () -> Unit) {
    val label = stringResource(R.string.developer_tools_entry_label)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(44.dp)
            .semantics { contentDescription = label },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.developer_tools_entry_text),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
