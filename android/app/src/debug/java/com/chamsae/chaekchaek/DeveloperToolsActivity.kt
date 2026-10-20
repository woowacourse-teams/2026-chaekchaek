package com.chamsae.chaekchaek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.chaekchaek.app.ui.bookdetail.BookQuestionAPreviewScreen
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.chaekchaek.app.ui.theme.ChaekchaekTheme

class DeveloperToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val items = remember {
                listOf(
                    DeveloperInfoItem(
                        label = getString(R.string.developer_tools_app_version),
                        value = getString(
                            R.string.developer_tools_version_value,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    ),
                    DeveloperInfoItem(
                        label = getString(R.string.developer_tools_build_type),
                        value = BuildConfig.BUILD_TYPE,
                    ),
                    DeveloperInfoItem(
                        label = getString(R.string.developer_tools_server_environment),
                        value = ApiConfiguration.environment,
                    ),
                    DeveloperInfoItem(
                        label = getString(R.string.developer_tools_api_base_url),
                        value = ApiConfiguration.current.baseUrl,
                    ),
                )
            }
            var questionPreview by rememberSaveable { mutableStateOf(intent.getBooleanExtra("bookQuestionA", false)) }
            ChaekchaekTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (questionPreview) Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                        BookQuestionAPreviewScreen(onExit = { questionPreview = false })
                    } else DeveloperToolsScreen(items = items, onBack = ::finish, onQuestionPreview = { questionPreview = true })
                }
            }
        }
    }
}

private data class DeveloperInfoItem(
    val label: String,
    val value: String,
)

@Composable
private fun DeveloperToolsScreen(
    items: List<DeveloperInfoItem>,
    onBack: () -> Unit,
    onQuestionPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        DeveloperToolsTopBar(onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            ) {
                Text(
                    text = stringResource(R.string.developer_tools_debug_only),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = stringResource(R.string.developer_tools_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onQuestionPreview) { Text("질문 A 개발 프리뷰") }
            DeveloperInfoCard(items)
        }
    }
}

@Composable
private fun DeveloperToolsTopBar(onBack: () -> Unit) {
    val backLabel = stringResource(R.string.developer_tools_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = backLabel },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, ChaekBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "‹",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = ChaekIconFontFamily(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        Text(
            text = stringResource(R.string.developer_tools_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeveloperInfoCard(items: List<DeveloperInfoItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChaekSurfaceMuted,
    ) {
        Column {
            items.forEachIndexed { index, item ->
                DeveloperInfoRow(item)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ChaekBorder,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperInfoRow(item: DeveloperInfoItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = item.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = item.value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
