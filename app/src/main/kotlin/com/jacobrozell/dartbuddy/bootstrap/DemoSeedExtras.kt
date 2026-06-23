package com.jacobrozell.dartbuddy.bootstrap

import android.content.Intent

object DemoSeedExtras {
    const val RESET = "seed_reset"
    const val SEED_PLAYERS = "seed_players"
    const val SEED_TRAINING_LOCKED = "seed_training_locked"
    const val SEED_TRAINING_ELIGIBLE = "seed_training_eligible"
    const val SEED_TRAINING_PARTNER = "seed_training_partner"
    const val ENQUEUE_TRAINING_MATCH = "enqueue_training_match"
    const val SKIP_ONBOARDING = "skip_onboarding"
}

fun Intent.hasSeedExtras(): Boolean =
    getBooleanExtra(DemoSeedExtras.RESET, false) ||
        getBooleanExtra(DemoSeedExtras.SEED_PLAYERS, false) ||
        getBooleanExtra(DemoSeedExtras.SEED_TRAINING_LOCKED, false) ||
        getBooleanExtra(DemoSeedExtras.SEED_TRAINING_ELIGIBLE, false) ||
        getBooleanExtra(DemoSeedExtras.SEED_TRAINING_PARTNER, false) ||
        getBooleanExtra(DemoSeedExtras.ENQUEUE_TRAINING_MATCH, false)
