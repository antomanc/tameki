package dev.antomanc.tameki.feature.aibridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun AibridgeScreen(
    modifier: Modifier = Modifier,
    onOpenPaste: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: AiBridgeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AibridgeScreen(
        state = state,
        onCopyPrompt = { prompt ->
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("tameki", prompt))
            viewModel.onPromptCopied()
        },
        onClearSnackbar = viewModel::clearSnackbar,
        onOpenChatbot = { pkg ->
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
            }
        },
        onOpenPaste = onOpenPaste,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AibridgeScreen(
    state: AiBridgeUiState,
    onCopyPrompt: (String) -> Unit,
    onClearSnackbar: () -> Unit,
    onOpenChatbot: (String) -> Unit,
    onOpenPaste: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    val copied = stringResource(R.string.ai_prompt_copied)
    val imported = stringResource(R.string.ai_imported)
    val singleBot = state.installedChatbots.singleOrNull()
    val singleOpenLabel = singleBot?.let { stringResource(R.string.ai_open_chatbot, it.label) }
    var showChatbotSheet by remember { mutableStateOf(false) }
    val spacing = MaterialTheme.spacing

    LaunchedEffect(state.snackbar) {
        when (state.snackbar) {
            "copied" -> {
                when {
                    singleBot != null && singleOpenLabel != null -> {
                        val result = snackbar.showSnackbar(
                            message = copied,
                            actionLabel = singleOpenLabel,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onOpenChatbot(singleBot.packageName)
                        }
                    }
                    state.installedChatbots.size > 1 -> {
                        snackbar.showSnackbar(copied)
                        showChatbotSheet = true
                    }
                    else -> snackbar.showSnackbar(copied)
                }
                onClearSnackbar()
            }
            "imported" -> {
                snackbar.showSnackbar(imported)
                onClearSnackbar()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = stringResource(R.string.ai_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.ai_navigate_up),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(tonalElevation = 0.dp, shadowElevation = 0.dp) {
                Button(
                    onClick = onOpenPaste,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.gutter, vertical = spacing.group),
                ) {
                    Text(stringResource(R.string.ai_paste_action))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = spacing.gutter,
                vertical = spacing.group,
            ),
        ) {
            item {
                Text(
                    text = stringResource(R.string.ai_fact),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(spacing.group))
            }
            items(state.prompts, key = { it.kind.name }) { card ->
                PromptRow(
                    kind = card.kind,
                    onCopy = { onCopyPrompt(card.prompt) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }

    if (showChatbotSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChatbotSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter)
                    .padding(bottom = spacing.section),
            ) {
                Text(
                    text = stringResource(R.string.ai_choose_chatbot),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = spacing.tight),
                )
                state.installedChatbots.forEach { bot ->
                    Text(
                        text = bot.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenChatbot(bot.packageName)
                                showChatbotSheet = false
                            }
                            .padding(vertical = spacing.rowPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptRow(
    kind: AiPromptKind,
    onCopy: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = spacing.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = promptIcon(kind),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.between),
        ) {
            Text(promptTitle(kind), style = MaterialTheme.typography.titleMedium)
            Text(
                text = promptFact(kind),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.ai_copy_prompt),
            )
        }
    }
}

private fun promptIcon(kind: AiPromptKind): ImageVector = when (kind) {
    AiPromptKind.RECEIPT -> Icons.AutoMirrored.Filled.ReceiptLong
    AiPromptKind.NATURAL_LANGUAGE -> Icons.AutoMirrored.Filled.ShortText
    AiPromptKind.SUBSCRIPTIONS -> Icons.Default.AccountBalance
    AiPromptKind.MONTH_REVIEW -> Icons.AutoMirrored.Filled.ShowChart
}

@Composable
private fun promptTitle(kind: AiPromptKind): String = stringResource(
    when (kind) {
        AiPromptKind.RECEIPT -> R.string.ai_prompt_receipt
        AiPromptKind.NATURAL_LANGUAGE -> R.string.ai_prompt_natural
        AiPromptKind.SUBSCRIPTIONS -> R.string.ai_prompt_subscriptions
        AiPromptKind.MONTH_REVIEW -> R.string.ai_prompt_month
    },
)

@Composable
private fun promptFact(kind: AiPromptKind): String = stringResource(
    when (kind) {
        AiPromptKind.RECEIPT -> R.string.ai_prompt_receipt_fact
        AiPromptKind.NATURAL_LANGUAGE -> R.string.ai_prompt_natural_fact
        AiPromptKind.SUBSCRIPTIONS -> R.string.ai_prompt_subscriptions_fact
        AiPromptKind.MONTH_REVIEW -> R.string.ai_prompt_month_fact
    },
)

@Preview(showBackground = true, backgroundColor = 0xFFF2F0EA)
@Composable
private fun AibridgePreview() {
    TamekiTheme(darkTheme = false, dynamicColor = false, motionEnabled = false) {
        AibridgeScreen(
            state = sampleAiState(),
            onCopyPrompt = {},
            onClearSnackbar = {},
            onOpenChatbot = {},
            onOpenPaste = {},
            onBack = {},
        )
    }
}

internal fun sampleAiState() = AiBridgeUiState(
    prompts = listOf(
        PromptCardUi(AiPromptKind.RECEIPT, "Extract…"),
        PromptCardUi(AiPromptKind.NATURAL_LANGUAGE, "Convert…"),
        PromptCardUi(AiPromptKind.SUBSCRIPTIONS, "Find…"),
        PromptCardUi(AiPromptKind.MONTH_REVIEW, "Review…"),
    ),
    installedChatbots = listOf(ChatbotApps.all.first()),
)
