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

    fun isSimpleWord(word: String): Boolean {
        val simpleWords = setOf("the", "to", "it", "that") // Define your list of simple words
        return word.toLowerCase() in simpleWords
    }

    override fun next(): String {
        val sentence = sentences[sentenceIndex]
        val phrases = sentence.split(Regex("[,;]|\\b(And|Or|But)\\b"))

        if (phraseIndex < phrases.size) {
            val phrase = phrases[phraseIndex]
            val words = phrase.trim().split(" ")

            // Generate SSML for the current word with emphasis
            var closed: Boolean = false
            val emphasizedWord = words.mapIndexed { index, w ->
                if (index == wordIndex*3) {
                    /*if (isSimpleWord(w)) {
                        // Skip simple words and move to the next wordIndex
                        wordIndex = (wordIndex + 1) % words.size
                        w // Return the original word without emphasis
                    } else {*/
                        "<prosody rate=\"slow\" pitch=\"+10%\"><emphasis level='x-strong'>$w"
                } else if (index == wordIndex*3+2) {
                    closed = true
                    "$w</emphasis></prosody>"
                } else w
            }.joinToString(" ")

            // Increment indices for the next iteration
            wordIndex = (wordIndex + 1) % (words.size/3)
            if (wordIndex == 0) {
                phraseIndex++
            }

            // Emphasize a different word in each repetition
            emphasisLevel = (emphasisLevel % words.size) + 1

            if(!closed) return "<speak>$emphasizedWord</emphasis></prosody></speak>"
            // Return the generated SSML
            return "<speak>$emphasizedWord</speak>"
        } else {
            // Move to the next phrase
            phraseIndex = 0
            sentenceIndex++
            return "<speak><break time=\"500ms\"/></speak>"
        }
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
