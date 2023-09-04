package com.github.shannonbay.studybuddy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.shannonbay.studybuddy.databinding.FragmentFirstBinding
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), TextToSpeech.OnInitListener, RecognitionListener {

    private var _binding: FragmentFirstBinding? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var mediaControllerManager: MediaControllerManager
    private lateinit var speechRecognizer: SpeechRecognizer

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private  var  nReceiver: NotificationReceiver? = null

    internal class NotificationReceiver : BroadcastReceiver() {
       override fun onReceive(context: Context, intent: Intent) {
            Log.e("NOTIFY", intent.getStringExtra("notification_event") + "n study-buddy")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        mediaControllerManager = MediaControllerManager(requireContext())

        nReceiver = NotificationReceiver()
        val filter = IntentFilter()
//        filter.addAction("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE")
        requireActivity().registerReceiver(nReceiver, filter)

        requestMicrophone()
        return binding.root


    }

    private val PERMISSION_REQUEST_MICROPHONE = 1
    private val PERMISSION_REQUEST_MEDIA_CONTROL = 2
    private val PERMISSION_REQUEST_MANAGE_MEDIA = 3

    private fun requestMicrophone() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.MEDIA_CONTENT_CONTROL) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.MEDIA_CONTENT_CONTROL), PERMISSION_REQUEST_MEDIA_CONTROL)
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.MANAGE_MEDIA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.MANAGE_MEDIA), PERMISSION_REQUEST_MANAGE_MEDIA)
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("MIC2", "PERMISSION GRANTED!!!!!!!!!!!!!!")

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_MICROPHONE
                )
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_MICROPHONE
                )
            }
        }
    }

    private val utteranceProgressListener = object : UtteranceProgressListener() {
       override fun onStart(utteranceId: String?) {
            Log.e("speech", "Starting now")
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)

        textToSpeech!!.setOnUtteranceProgressListener(utteranceProgressListener)

        // Initialize the SpeechRecognizer
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with your task
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )

            Log.e("MIC", "Audio permission not granted!")
        }
        if(!SpeechRecognizer.isRecognitionAvailable(requireContext()))
           Log.e("MIC", "Not available")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e("MIC", "ON DEVICE")
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(requireContext())
        } else {
            Log.e("MIC", "ON DEVICE2")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(
                requireContext(),
                ComponentName(
                    "com.google.android.tts",
                    "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
                )
            )
//            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        }
        speechRecognizer.setRecognitionListener(this)
        startListening()
        Log.e("MIC", "Listening is runinning")
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }


    private val myText = "In the year that King Uzziah died I saw the Lord sitting on a high and lofty throne, and the train of His robe filled the temple."

    private val i = TextToSSMLIterator(myText);
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for speech synthesis (e.g., English)
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "Language is not available.")
            } else {
                val ssml = i.next();//.convertToSSML(myText)
                Log.e("speech: 100", ssml)
                textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    var startTime: Long = 0
                    override fun onStart(utteranceId: String) {
                        Log.e("TTS", "Starting new speech")
                        startTime = System.currentTimeMillis()
                    }

                    override fun onDone(p0: String?) {
                        Log.i("TTS", "Utterance done: " );
                        val delayMillis = System.currentTimeMillis() - startTime
                        Log.i("TTS", "Next in " + delayMillis / 1000 + "s" );
                        scheduleNext(delayMillis/3)
                        // Called when speech synthesis is completed.
                    }

                    override fun onError(utteranceId: String) {
                        // Called when there's an error in speech synthesis.
                    }
                })
                //speakText(ssml)
                // TextToSpeech engine is ready.
                // Now you can send text snippets to be spoken.
//                val myText = "<speak>This <break time=\"1s\"/>is an important <emphasis level='strong'>word</emphasis>.</speak>\n"

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

    fun scheduleNext(delayMillis: Long) {
        // Create a ScheduledExecutorService with a single thread
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        // Delay in seconds

        // Define the code you want to run after the delay
        val callback = Runnable {
            // Code to be executed after the delay
            // This is your one-shot callback function
            // For example, you can update UI elements or perform other actions here
            val ssml = i.next()
            Log.i("TTS", ssml)
            speakText(ssml)

            // Shutdown the executor when no longer needed
            executor.shutdown()
        }

        // Schedule the callback to run after the specified delay
        executor.schedule(callback, delayMillis, TimeUnit.MILLISECONDS)
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID)
    }



    override fun onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }

        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()


        if (speechRecognizer != null) {
            speechRecognizer.cancel()
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }

        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }

        _binding = null
    }

    private fun startListening() {

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE , false)

        speechRecognizer.startListening(recognizerIntent)
        Log.d("MIC", "Started listening")
    }

    override fun onReadyForSpeech(p0: Bundle?) {
        Log.d("MIC", "Ready to listen")
    }

    override fun onBeginningOfSpeech() {
        Log.d("MIC", "Hrd you!")

        // Create a MediaButtonReceiver object and a KeyEvent object
        val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
        audioManager!!.dispatchMediaKeyEvent(event)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.MEDIA_CONTENT_CONTROL) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.MEDIA_CONTENT_CONTROL), PERMISSION_REQUEST_MEDIA_CONTROL)
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.MANAGE_MEDIA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.MANAGE_MEDIA), PERMISSION_REQUEST_MANAGE_MEDIA)
        }


        mediaControllerManager.pauseActiveMediaSessions()
    }

    override fun onRmsChanged(p0: Float) {
//        Log.i("MIC", "Volume or something?$p0")
    }

    override fun onBufferReceived(p0: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun onEndOfSpeech() {
        Log.i("MIC", "You went quiet!")
    }

    override fun onError(p0: Int) {
        Log.i("MIC", "Got an error $p0 :(")
        val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
        audioManager!!.dispatchMediaKeyEvent(event)

        startListening()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val detectedSpeech = matches[0]
            Log.e("MIC", "Heard you!$detectedSpeech")
        }
        startListening() // Start listening again for more speech
    }

    override fun onPartialResults(p0: Bundle?) {
        Log.i("MIC", "Partial result $p0")
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        TODO("Not yet implemented")
    }
}