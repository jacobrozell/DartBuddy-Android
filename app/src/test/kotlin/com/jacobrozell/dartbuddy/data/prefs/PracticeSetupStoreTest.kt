package com.jacobrozell.dartbuddy.data.prefs

import com.jacobrozell.dartbuddy.domain.model.MatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeSetupStoreTest {
    @Test
    fun enqueueAndConsumeClearsPending() {
        val store = PracticeSetupStore()
        store.enqueuePractice("human", "bot", MatchType.X01)

        val setup = store.consume()
        assertEquals("human", setup?.humanId)
        assertEquals("bot", setup?.trainingBotId)
        assertEquals(MatchType.X01, setup?.mode)
        assertNull(store.consume())
    }
}
