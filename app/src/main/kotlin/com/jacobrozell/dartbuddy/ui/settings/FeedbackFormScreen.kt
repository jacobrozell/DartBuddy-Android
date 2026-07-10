package com.jacobrozell.dartbuddy.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.support.feedback.FeedbackCategory
import com.jacobrozell.dartbuddy.support.feedback.FeedbackDraft
import com.jacobrozell.dartbuddy.support.feedback.FeedbackMail
import com.jacobrozell.dartbuddy.support.feedback.FeedbackMostWantedFeature
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
private fun FeedbackSectionFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = DS.s1, end = DS.s1, bottom = DS.s2),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackFormScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var mostWanted by rememberSaveable { mutableStateOf(FeedbackMostWantedFeature.NOT_SURE.name) }
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.IMPROVEMENT.name) }
    var specificItem by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var showMailUnavailable by rememberSaveable { mutableStateOf(false) }

    val mostWantedLabels = FeedbackMostWantedFeature.pickerOptions.associateWith { stringResource(it.labelRes) }
    val categoryLabels = FeedbackCategory.entries.associateWith { stringResource(it.labelRes) }

    val selectedMostWanted = FeedbackMostWantedFeature.valueOf(mostWanted)
    val selectedCategory = FeedbackCategory.valueOf(category)
    val draft = FeedbackDraft(
        mostWantedFeature = selectedMostWanted,
        category = selectedCategory,
        specificItem = specificItem,
        summary = summary,
        details = details,
    )

    fun sendFeedback() {
        if (!draft.isValid) return
        val intent = FeedbackMail.mailtoIntent(context, draft)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            showMailUnavailable = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feedback_nav_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DS.s4, vertical = DS.s2),
            verticalArrangement = Arrangement.spacedBy(DS.s4),
        ) {
            Text(
                stringResource(R.string.feedback_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.feedback_most_wanted_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            SetupSectionCard {
                HorizontalOptionChips(
                    label = stringResource(R.string.feedback_most_wanted_title),
                    options = FeedbackMostWantedFeature.pickerOptions,
                    selected = selectedMostWanted,
                    onSelect = { mostWanted = it.name },
                    labelFor = { mostWantedLabels[it].orEmpty() },
                )
            }
            FeedbackSectionFooter(stringResource(R.string.feedback_most_wanted_footer))

            Text(stringResource(R.string.feedback_category_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            SetupSectionCard {
                HorizontalOptionChips(
                    label = stringResource(R.string.feedback_category_title),
                    options = FeedbackCategory.entries,
                    selected = selectedCategory,
                    onSelect = { category = it.name },
                    labelFor = { categoryLabels[it].orEmpty() },
                )
            }

            Text(stringResource(R.string.feedback_suggestion_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            SetupSectionCard {
                OutlinedTextField(
                    value = specificItem,
                    onValueChange = { specificItem = it },
                    label = { Text(stringResource(selectedCategory.specificItemLabelRes)) },
                    placeholder = { Text(stringResource(selectedCategory.specificItemPlaceholderRes)) },
                    modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.FEEDBACK_SPECIFIC_ITEM),
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text(stringResource(R.string.feedback_summary)) },
                    placeholder = { Text(stringResource(selectedCategory.summaryPlaceholderRes)) },
                    modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.FEEDBACK_SUMMARY),
                )
            }
            FeedbackSectionFooter(stringResource(R.string.feedback_summary_footer))

            Text(stringResource(R.string.feedback_details_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            SetupSectionCard {
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(stringResource(R.string.feedback_details_field)) },
                    placeholder = { Text(stringResource(selectedCategory.detailsPlaceholderRes)) },
                    modifier = Modifier.fillMaxWidth().a11yId(AccessibilityIds.FEEDBACK_DETAILS),
                    minLines = 4,
                )
            }
            FeedbackSectionFooter(stringResource(selectedCategory.detailsPlaceholderRes))

            PrimaryActionButton(
                title = stringResource(R.string.feedback_send),
                onClick = ::sendFeedback,
                enabled = draft.isValid,
                modifier = Modifier.a11yId(AccessibilityIds.FEEDBACK_SEND),
            )
            FeedbackSectionFooter(stringResource(R.string.feedback_send_footer))
        }
    }

    if (showMailUnavailable) {
        AlertDialog(
            onDismissRequest = { showMailUnavailable = false },
            title = { Text(stringResource(R.string.feedback_mail_unavailable_title)) },
            text = {
                Text(stringResource(R.string.feedback_mail_unavailable_message, com.jacobrozell.dartbuddy.support.AppLinks.FEEDBACK_EMAIL))
            },
            confirmButton = {
                TextButton(onClick = {
                    copyFeedbackDraft(context, draft)
                    showMailUnavailable = false
                }) {
                    Text(stringResource(R.string.feedback_mail_unavailable_copy), color = Brand.Green)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        context.startActivity(FeedbackMail.mailtoIntent(context, draft))
                        showMailUnavailable = false
                    }) {
                        Text(stringResource(R.string.feedback_mail_unavailable_open), color = Brand.Green)
                    }
                    TextButton(onClick = { showMailUnavailable = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            },
        )
    }
}

private fun copyFeedbackDraft(context: Context, draft: FeedbackDraft) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText("Dart Buddy feedback", FeedbackMail.mailBody(context, draft)),
    )
    Toast.makeText(context, R.string.feedback_mail_unavailable_copy, Toast.LENGTH_SHORT).show()
}
