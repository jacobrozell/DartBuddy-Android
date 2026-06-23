package com.jacobrozell.dartbuddy.support.version

object AppVersionComparator {
    fun isStoreVersionNewer(store: String, installed: String): Boolean =
        compare(store, installed) > 0

    fun compare(lhs: String, rhs: String): Int {
        val left = components(lhs)
        val right = components(rhs)
        val count = maxOf(left.size, right.size)
        for (index in 0 until count) {
            val leftValue = left.getOrElse(index) { 0 }
            val rightValue = right.getOrElse(index) { 0 }
            when {
                leftValue > rightValue -> return 1
                leftValue < rightValue -> return -1
            }
        }
        return 0
    }

    fun components(version: String): List<Int> =
        version.split(".").map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
}
