package com.github.shannonbay.studybuddy

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.shannonbay.studybuddy.databinding.FragmentFirstBinding
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentFirstBinding? = null
    private var textToSpeech: TextToSpeech? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)

        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Called when speech synthesis starts.
            }

            override fun onDone(utteranceId: String) {
                // Called when speech synthesis is completed.
                if (utteranceId == "YOUR_UTTERANCE_ID") {
                    // Perform your action here after speech synthesis is done.
                }
            }

            override fun onError(utteranceId: String?) {
                // Called when there's an error in speech synthesis.
            }
        })

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for speech synthesis (e.g., English)
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "Language is not available.")
            } else {
                // TextToSpeech engine is ready.
                // Now you can send text snippets to be spoken.
//                val myText = "<speak>This <break time=\"1s\"/>is an important <emphasis level='strong'>word</emphasis>.</speak>\n"
                val myText = "Charge certain ones not to teach different things " +
                        "nor to give heed to myths and unending genealogies, " +
                        "which produce questionings rather than God’s economy, " +
                        "which is in faith."
                val i = TextToSSMLIterator(myText);
                i.next();
                val ssml = i.next();//.convertToSSML(myText)
                Log.e("speech: 100", ssml)
                speakText(ssml)
                /*"1 Tim. 1:3-4 ...Charge certain ones not to teach different things " +
                        "nor to give heed to myths and unending genealogies, " +
                        "which produce questionings rather than God’s economy, " +
                val strings = TextFilter.filterAndSplitText(myText)
                for(s in strings) {
                    speakText(s)
                }*/
            }
        } else {
            Log.e("TextToSpeech", "Initialization failed.")
        }
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }



    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}