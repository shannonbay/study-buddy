package com.github.shannonbay.studybuddy

class TextToSSMLIterator(inputText: String) : Iterator<String> {
    private val sentences: List<String> = inputText.split(Regex("[.!?]"))
    private var sentenceIndex = 0
    private var phraseIndex = 0
    private var wordIndex = 0
    private var emphasisLevel = 1

    override fun hasNext(): Boolean {
        return sentenceIndex < sentences.size
    }

    override fun next(): String {
        val sentence = sentences[sentenceIndex]
        val phrases = sentence.split(Regex("[,;]|\\b (and|or|but|nor) \\b"))
        val phrase = phrases[phraseIndex]
        val words = phrase.trim().split(" ")

        // Generate SSML for the current word with emphasis
        val emphasizedWord = words.mapIndexed { index, w ->
            if (index == wordIndex) "<emphasis level='strong'>$w</emphasis>" else w
        }.joinToString(" ")

        // Increment indices for the next iteration
        wordIndex = (wordIndex + 1) % words.size
        if (wordIndex == 0) {
            phraseIndex = (phraseIndex + 1) % phrases.size
            if (phraseIndex == 0) {
                sentenceIndex++
            }
        }

        // Emphasize a different word in each repetition
        emphasisLevel = (emphasisLevel % words.size) + 1

        // Return the generated SSML
        return "<speak>$emphasizedWord</speak>"
    }
}

fun main() {
    val inputText = "Hello, World! This is a test sentence and another example."

    val ssmlIterator = TextToSSMLIterator(inputText)

    while (ssmlIterator.hasNext()) {
        val ssmlPhrase = ssmlIterator.next()
        println(ssmlPhrase)
    }
}
