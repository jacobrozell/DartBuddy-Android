package com.jacobrozell.dartbuddy.support.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.testTag as semanticsTestTag

fun Modifier.a11yId(id: String): Modifier = this
    .semantics {
        testTagsAsResourceId = true
        semanticsTestTag = id
    }
    .testTag(id)
