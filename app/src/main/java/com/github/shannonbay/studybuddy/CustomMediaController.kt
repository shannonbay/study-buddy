package com.github.shannonbay.studybuddy

import android.content.Context
import android.util.AttributeSet
import android.widget.MediaController

class CustomMediaController(context: Context) : MediaController(context) {
    override fun hide() {
        // no-op
    }
}
