package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode

object CheckoutSuggester {
    fun suggestion(
        remaining: Int,
        mode: X01CheckoutMode,
        dartsAvailable: Int = 3,
    ): List<String>? = allSuggestions(remaining, mode, dartsAvailable).firstOrNull()

    fun allSuggestions(
        remaining: Int,
        mode: X01CheckoutMode,
        dartsAvailable: Int = 3,
    ): List<List<String>> {
        if (dartsAvailable < 1) return emptyList()
        if (remaining < 2) {
            if (mode == X01CheckoutMode.SingleOut && remaining == 1) return listOf(listOf("1"))
            return emptyList()
        }
        for (darts in 1..minOf(dartsAvailable, 3)) {
            val routes = allRoutes(remaining, darts, mode)
            if (routes.isNotEmpty()) return routes
        }
        return emptyList()
    }

    private fun allRoutes(remaining: Int, darts: Int, mode: X01CheckoutMode): List<List<String>> = when (darts) {
        1 -> finishLabel(remaining, mode)?.let { listOf(listOf(it)) } ?: emptyList()
        2 -> {
            val routes = mutableListOf<List<String>>()
            for (finisher in finishers(mode)) {
                val setup = remaining - finisher.value
                if (setup < 1) continue
                val setupLabel = setupLabel(setup) ?: continue
                routes.add(listOf(setupLabel, finisher.label))
            }
            routes
        }
        else -> {
            val routes = mutableListOf<List<String>>()
            for (opener in setups) {
                val rest = remaining - opener.value
                if (rest < 2) continue
                for (tail in allRoutes(rest, 2, mode)) {
                    routes.add(listOf(opener.label) + tail)
                }
            }
            routes
        }
    }

    private fun finishLabel(value: Int, mode: X01CheckoutMode): String? = when (mode) {
        X01CheckoutMode.SingleOut -> setupLabel(value)
        X01CheckoutMode.DoubleOut -> when {
            value == 50 -> "Bull"
            value % 2 == 0 && (value / 2) in 1..20 -> "D${value / 2}"
            else -> null
        }
        X01CheckoutMode.MasterOut -> when {
            value % 2 == 0 && (value / 2) in 1..20 -> "D${value / 2}"
            value % 3 == 0 && (value / 3) in 1..20 -> "T${value / 3}"
            value == 50 -> "Bull"
            else -> null
        }
    }

    private fun setupLabel(value: Int): String? = when {
        value % 3 == 0 && (value / 3) in 1..20 -> "T${value / 3}"
        value in 1..20 -> "$value"
        value % 2 == 0 && (value / 2) in 1..20 -> "D${value / 2}"
        value == 25 -> "25"
        value == 50 -> "Bull"
        else -> null
    }

    private data class Finisher(val value: Int, val label: String)

    private fun finishers(mode: X01CheckoutMode): List<Finisher> = when (mode) {
        X01CheckoutMode.DoubleOut -> {
            val result = (20 downTo 1).map { Finisher(it * 2, "D$it") }.toMutableList()
            result.add(Finisher(50, "Bull"))
            result
        }
        X01CheckoutMode.MasterOut -> {
            val result = (20 downTo 1).map { Finisher(it * 2, "D$it") }.toMutableList()
            result.addAll((20 downTo 1).map { Finisher(it * 3, "T$it") })
            result.add(Finisher(50, "Bull"))
            result
        }
        X01CheckoutMode.SingleOut -> buildList {
            for (face in 20 downTo 1) add(Finisher(face * 3, "T$face"))
            add(Finisher(50, "Bull"))
            add(Finisher(25, "25"))
            for (face in 20 downTo 1) add(Finisher(face * 2, "D$face"))
            for (face in 20 downTo 1) add(Finisher(face, "$face"))
        }
    }

    private data class Setup(val value: Int, val label: String)

    private val setups: List<Setup> = buildList {
        for (face in 20 downTo 1) add(Setup(face * 3, "T$face"))
        add(Setup(50, "Bull"))
        add(Setup(25, "25"))
        for (face in 20 downTo 1) add(Setup(face, "$face"))
    }
}
