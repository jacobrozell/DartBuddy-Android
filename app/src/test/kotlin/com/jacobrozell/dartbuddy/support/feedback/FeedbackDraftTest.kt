package com.jacobrozell.dartbuddy.support.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackDraftTest {
    @Test
    fun isValid_requiresNonBlankSummary() {
        assertFalse(FeedbackDraft().isValid)
        assertFalse(FeedbackDraft(summary = "   ").isValid)
        assertTrue(FeedbackDraft(summary = "Faster Play setup").isValid)
    }
}
