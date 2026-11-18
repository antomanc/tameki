package dev.antomanc.tameki.feature.aibridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    LaunchedEffect(state.snackbar) {
        when (state.snackbar) {
            "copied" -> {
                snackbar.showSnackbar(copied)
                onClearSnackbar()
            }
            "imported" -> {
                snackbar.showSnackbar(imported)
                onClearSnackbar()
            }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.ai_back)) }
                    }
                },
                actions = {
                    TextButton(onClick = onOpenPaste) {
                        Text(stringResource(R.string.ai_paste_action))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.spacing.gutter,
                vertical = MaterialTheme.spacing.group,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.group),
        ) {
            items(state.prompts, key = { it.kind.name }) { card ->
                PromptCard(
                    title = promptTitle(card.kind),
                    onCopy = { onCopyPrompt(card.prompt) },
                    chatbots = state.installedChatbots,
                    onOpenChatbot = onOpenChatbot,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(
    title: String,
    onCopy: () -> Unit,
    chatbots: List<ChatbotApps.Chatbot>,
    onOpenChatbot: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(MaterialTheme.spacing.tight))
        Button(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ai_copy_prompt))
        }
        if (chatbots.isNotEmpty()) {
            Spacer(Modifier.height(MaterialTheme.spacing.tight))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.tight)) {
                chatbots.forEach { bot ->
                    FilterChip(
                        selected = false,
                        onClick = { onOpenChatbot(bot.packageName) },
                        label = { Text(stringResource(R.string.ai_open_chatbot, bot.label)) },
                    )
                }
            }
        }
    }
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
