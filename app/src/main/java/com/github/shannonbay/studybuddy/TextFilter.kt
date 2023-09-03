package com.github.shannonbay.studybuddy

import java.util.regex.Pattern

object TextFilter {
    @JvmStatic
    fun main(args: Array<String>) {
        val inputText = "Hello123, World! This is a text filter example."

        // Remove digits and split into phrases
        val phrases = filterAndSplitText(inputText)

        // Print the filtered phrases
        for (phrase in phrases) {
            println(phrase)
        }
    }

    fun filterAndSplitText(inputText: String): Array<String> {
        // Remove digits and split into phrases
        val phrases = inputText.replace(Regex("\\d"), "") // Remove digits
            .split(Regex("[^\\p{Alpha}\\s]+")) // Split on non-alpha, non-whitespace characters
            .toTypedArray()

        // Remove empty phrases (if any)
        return phrases.filter { !it.trim().isEmpty() }.toTypedArray()
    }
}
