package com.jacobrozell.dartbuddy.support.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.jacobrozell.dartbuddy.BuildConfig
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.AppLinks

object FeedbackMail {
    fun mailSubject(context: Context, draft: FeedbackDraft): String {
        val tag = context.getString(draft.category.subjectTagRes)
        val item = draft.specificItem.trim()
        return if (item.isEmpty()) {
            context.getString(R.string.feedback_mail_subject_format, tag, draft.trimmedSummary)
        } else {
            context.getString(R.string.feedback_mail_subject_with_item_format, tag, item)
        }
    }

    fun mailBody(context: Context, draft: FeedbackDraft): String {
        val item = draft.specificItem.trim()
        val details = draft.details.trim()
        val lines = buildList {
            add(
                context.getString(
                    R.string.feedback_mail_line_most_wanted,
                    context.getString(draft.mostWantedFeature.labelRes),
                ),
            )
            add(
                context.getString(
                    R.string.feedback_mail_line_category,
                    context.getString(draft.category.labelRes),
                ),
            )
            if (item.isNotEmpty()) {
                add(
                    context.getString(
                        R.string.feedback_mail_line_specific_item,
                        context.getString(draft.category.specificItemLabelRes),
                        item,
                    ),
                )
            }
            add(context.getString(R.string.feedback_mail_line_summary, draft.trimmedSummary))
            add("")
            add(context.getString(R.string.feedback_mail_details_header))
            add(if (details.isEmpty()) context.getString(R.string.feedback_mail_details_none) else details)
            add("")
            add("---")
            add(deviceDiagnostics(context))
        }
        return lines.joinToString("\n")
    }

    fun mailtoIntent(context: Context, draft: FeedbackDraft): Intent {
        val subject = mailSubject(context, draft)
        val body = mailBody(context, draft)
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${AppLinks.FEEDBACK_EMAIL}")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AppLinks.FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }

    fun deviceDiagnostics(context: Context): String = buildString {
        append(context.getString(R.string.feedback_mail_device_app, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        append('\n')
        append(context.getString(R.string.feedback_mail_device_model, Build.MANUFACTURER, Build.MODEL))
        append('\n')
        append(context.getString(R.string.feedback_mail_device_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
    }
}
