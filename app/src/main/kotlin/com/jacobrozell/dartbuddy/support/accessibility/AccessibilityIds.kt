package com.jacobrozell.dartbuddy.support.accessibility

object AccessibilityIds {
    const val TAB_PLAY = "tab_play"
    const val TAB_PLAYERS = "tab_players"
    const val TAB_ACTIVITY = "tab_activity"
    const val TAB_SETTINGS = "tab_settings"

    const val ACTIVITY_SEGMENT_HISTORY = "activity_segment_history"
    const val ACTIVITY_SEGMENT_STATISTICS = "activity_segment_statistics"
    const val ACTIVITY_MODE_FILTER_MENU = "activityModeFilterMenu"
    const val ACTIVITY_PLAYER_FILTER_MENU = "activityPlayerFilterMenu"
    const val HISTORY_DETAIL_RESULT_CARD = "historyDetailResultCard"
    const val HISTORY_DETAIL_TIMELINE_TOGGLE = "historyDetailTimelineToggle"

    const val LAUNCH_SPLASH = "launch_splash"

    const val ONBOARDING_PLAYER_NAME = "onboarding_player_name"
    const val ONBOARDING_GET_STARTED = "onboarding_get_started"
    const val PLAYERS_SEARCH = "players_searchField"
    const val PLAYERS_ADD_FAB = "players_addFab"
    const val PLAYERS_EMPTY = "players_empty"
    const val PLAYERS_SEARCH_EMPTY = "players_search_empty"
    const val EMPTY_STATE_START_MATCH = "emptyStateStartMatchButton"

    const val START_MATCH_BUTTON = "startMatchButton"
    const val PLAY_HOME_RESUME = "play_home_resumeButton"
    const val SETUP_ADD_PLAYER = "setup_footer_addPlayer"
    const val SETUP_ADD_BOT = "setup_footer_addBot"
    const val SETUP_RANDOM_ORDER = "setup_randomOrderToggle"
    const val SETUP_EDIT_OPTIONS = "setup_editOptionsButton"
    const val SETUP_LEARN_TO_PLAY = "setup_learnToPlayButton"
    const val SETUP_START_SCORE_CHIP = "setup_startScoreChip"
    const val SETUP_CHECKOUT_CHIP = "setup_checkoutChip"
    const val SETUP_CHECKIN_CHIP = "setup_checkInChip"
    const val SETUP_LEGS_CHIP = "setup_legsChip"
    const val SETUP_SETS_CHIP = "setup_setsChip"
    const val SETUP_LEG_FORMAT_CHIP = "setup_setLegChip"

    const val MATCH_EXIT = "match_exit"
    const val MATCH_UNDO = "match_undo"
    const val LEG_WON_BANNER = "legWonBanner"
    const val BUST_BANNER = "bustBanner"

    const val MATCH_SUMMARY_HEADER = "matchSummaryHeader"
    const val MATCH_SUMMARY_GAME_RECORDED = "matchSummaryGameRecorded"
    const val MATCH_SUMMARY_REMATCH = "matchSummaryRematch"
    const val MATCH_SUMMARY_DONE = "matchSummaryDone"
    const val MATCH_SUMMARY_UNDO_LAST_THROW = "matchSummaryUndoLastThrow"

    const val SETTINGS_FORM = "settings_form"
    const val SETTINGS_LOADING = "settings_loading"
    const val SETTINGS_RETRY = "settings_retryButton"
    const val SETTINGS_HAPTICS = "settings_hapticsToggle"
    const val SETTINGS_SOUND = "settings_soundToggle"
    const val SETTINGS_TURN_CALLER = "settings_turnTotalCallerToggle"
    const val SETTINGS_BOT_STAGGER = "settings_botStaggerToggle"
    const val SETTINGS_BOT_HAPTICS = "settings_botDartHapticsToggle"
    const val SETTINGS_INSTANT_BOT_TURNS = "settings_instantBotTurnsToggle"

    const val FEEDBACK_SPECIFIC_ITEM = "feedback.specificItem"
    const val FEEDBACK_SUMMARY = "feedback.summary"
    const val FEEDBACK_DETAILS = "feedback.details"
    const val FEEDBACK_SEND = "feedback.send"

    const val PLAYERS_LOADING = "players_loading"
    const val SETTINGS_RESET_ALL = "settings_resetAllDataButton"
    const val SETTINGS_REPLAY_ONBOARDING = "settings_viewOnboardingButton"
    const val SETTINGS_RATE_APP = "settings_rateAppLink"

    const val PAD_DOUBLE = "pad_double"
    const val PAD_TRIPLE = "pad_triple"
    const val PAD_UNDO = "pad_undo"
    const val PAD_MISS = "pad_0"
    const val PAD_BULL = "pad_25"
    const val DART_VISIT_PREVIEW = "dart_visit_preview"

    fun padSegment(segment: Int): String = "pad_$segment"
}
