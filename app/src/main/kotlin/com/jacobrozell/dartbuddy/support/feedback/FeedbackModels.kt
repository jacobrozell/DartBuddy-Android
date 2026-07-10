package com.jacobrozell.dartbuddy.support.feedback

import com.jacobrozell.dartbuddy.R

enum class FeedbackMostWantedFeature(val labelRes: Int) {
    MORE_PARTY_GAME_MODES(R.string.feedback_most_wanted_party),
    MORE_COOP_GAME_MODES(R.string.feedback_most_wanted_coop),
    MORE_PRACTICE_GAME_MODES(R.string.feedback_most_wanted_practice),
    MORE_STANDARD_GAME_MODES(R.string.feedback_most_wanted_standard),
    ONLINE_PLAY(R.string.feedback_most_wanted_online),
    ACCESSIBILITY(R.string.feedback_most_wanted_accessibility),
    ACHIEVEMENTS_AND_PROGRESS(R.string.feedback_most_wanted_achievements),
    WIDGETS_AND_WATCH(R.string.feedback_most_wanted_widgets),
    AUTO_SCORING(R.string.feedback_most_wanted_auto_scoring),
    IPAD_LAYOUT(R.string.feedback_most_wanted_large_screen),
    SOMETHING_ELSE(R.string.feedback_most_wanted_something_else),
    NOT_SURE(R.string.feedback_most_wanted_not_sure),
    ;

    companion object {
        val pickerOptions: List<FeedbackMostWantedFeature> =
            entries.filter { it != NOT_SURE } + NOT_SURE
    }
}

enum class FeedbackCategory(
    val labelRes: Int,
    val subjectTagRes: Int,
    val specificItemLabelRes: Int,
    val specificItemPlaceholderRes: Int,
    val summaryPlaceholderRes: Int,
    val detailsPlaceholderRes: Int,
) {
    GAME_MODE(
        R.string.feedback_category_game_mode,
        R.string.feedback_subject_game_mode,
        R.string.feedback_specific_game_mode,
        R.string.feedback_placeholder_game_mode,
        R.string.feedback_summary_placeholder_game_mode,
        R.string.feedback_details_placeholder_game_mode,
    ),
    SCORING_RULES(
        R.string.feedback_category_scoring_rules,
        R.string.feedback_subject_rules,
        R.string.feedback_specific_rules,
        R.string.feedback_placeholder_rules,
        R.string.feedback_summary_placeholder_rules,
        R.string.feedback_details_placeholder_rules,
    ),
    BOT_OPPONENT(
        R.string.feedback_category_bots,
        R.string.feedback_subject_bots,
        R.string.feedback_specific_bots,
        R.string.feedback_placeholder_bots,
        R.string.feedback_summary_placeholder_bots,
        R.string.feedback_details_placeholder_bots,
    ),
    SCORING_UX(
        R.string.feedback_category_scoring_ux,
        R.string.feedback_subject_scoring_ux,
        R.string.feedback_specific_scoring_ux,
        R.string.feedback_placeholder_scoring_ux,
        R.string.feedback_summary_placeholder_scoring_ux,
        R.string.feedback_details_placeholder_scoring_ux,
    ),
    STATS_ACTIVITY(
        R.string.feedback_category_stats,
        R.string.feedback_subject_stats,
        R.string.feedback_specific_stats,
        R.string.feedback_placeholder_stats,
        R.string.feedback_summary_placeholder_stats,
        R.string.feedback_details_placeholder_stats,
    ),
    BUG(
        R.string.feedback_category_bug,
        R.string.feedback_subject_bug,
        R.string.feedback_specific_bug,
        R.string.feedback_placeholder_bug,
        R.string.feedback_summary_placeholder_bug,
        R.string.feedback_details_placeholder_bug,
    ),
    IMPROVEMENT(
        R.string.feedback_category_improvement,
        R.string.feedback_subject_improvement,
        R.string.feedback_specific_improvement,
        R.string.feedback_placeholder_improvement,
        R.string.feedback_summary_placeholder_improvement,
        R.string.feedback_details_placeholder_improvement,
    ),
    OTHER(
        R.string.feedback_category_other,
        R.string.feedback_subject_other,
        R.string.feedback_specific_other,
        R.string.feedback_placeholder_other,
        R.string.feedback_summary_placeholder_other,
        R.string.feedback_details_placeholder_other,
    ),
}

data class FeedbackDraft(
    val mostWantedFeature: FeedbackMostWantedFeature = FeedbackMostWantedFeature.NOT_SURE,
    val category: FeedbackCategory = FeedbackCategory.IMPROVEMENT,
    val specificItem: String = "",
    val summary: String = "",
    val details: String = "",
) {
    val trimmedSummary: String get() = summary.trim()
    val isValid: Boolean get() = trimmedSummary.isNotEmpty()
}
