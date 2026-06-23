package com.jacobrozell.dartbuddy.support

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class L10n @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun get(resId: Int): String = context.getString(resId)

    fun format(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
