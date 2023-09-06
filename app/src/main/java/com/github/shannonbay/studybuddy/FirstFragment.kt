package com.github.shannonbay.studybuddy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MicrophoneInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognizerIntent
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
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadListener
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ScheduledFuture

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentFirstBinding? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var mediaControllerManager: MediaControllerManager

    /**
     * Microphone setup for VAD
     */

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 8000 // You can change this to your desired sample rate
    private val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize =
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private  var  nReceiver: NotificationReceiver? = null

    private var vad: VadSilero? = null

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

        // Request audio recording permission if not granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST
            )
        }

        // Initialize AudioRecord
        audioRecord = AudioRecord(
            audioSource,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        vad = Vad.builder()
            .setContext(requireContext())
            .setSampleRate(SampleRate.SAMPLE_RATE_8K)
            .setFrameSize(FrameSize.FRAME_SIZE_512)
            .setMode(Mode.NORMAL)
            .setSilenceDurationMs(300)
            .setSpeechDurationMs(50)
            .build()


        return binding.root


    }

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
                    RECORD_AUDIO_PERMISSION_REQUEST
                )
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_REQUEST
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

        Log.e("MIC", "Listening is runinning")
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        startRecording()
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

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    fun scheduleNext(runnable: Runnable, delayMillis: Long): ScheduledFuture<*>? {
        return executor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS)
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

    private fun stopRecording() {
        resumeMediaScheduledFuture?.cancel(true)
        if (audioRecord != null && audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord!!.stop()
            audioRecord!!.release()
            isRecording = false
        }
    }
    override fun onStop() {
        super.onStop()
        Log.e("VAD", "onStop called!")
        //stopRecording()

        if (textToSpeech != null ) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }

    override fun onDestroy() {
        Log.e("VAD", "onDestroy called!")
        stopRecording()

        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.e("VAD", "onDestroyView called!")
        stopRecording()
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }

        _binding = null
    }

    fun onEndOfSpeech() {
        Log.i("VAD", "You went quiet! REWIND and PLAY")
        val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
//        val rewindEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
//        val rewindEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND)

        if(shouldRewind) {
            shouldRewind = false

/*            Log.i("VAD", "REWIND BROADCAST")
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
            requireActivity().sendOrderedBroadcast(intent, null)*/
            Log.i("VAD", "REWIND BROADCAST")
/*            val eventTime3 = SystemClock.uptimeMillis()
            val intent3 = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent3.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime3, eventTime3, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND, 0))
            requireActivity().sendOrderedBroadcast(intent3, null)*/

            var rewindEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND)
//            audioManager!!.requestAudioFocus(AudioFocusRequest.)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)
            rewindEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)
/*
            rewindEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)
            rewindEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)

            rewindEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)
            rewindEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)*/
        }

/*        val intent2 = Intent(Intent.ACTION_MEDIA_BUTTON)
        val eventTime2 = SystemClock.uptimeMillis()
        //        val eventTime2 = System.nanoTime()
        intent2.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime2, eventTime2, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
        requireActivity().sendOrderedBroadcast(intent2, null)*/

        /*        val intent2 = Intent(Intent.ACTION_MEDIA_BUTTON)
                val eventTime2 = SystemClock.uptimeMillis() + 1
        //        val eventTime2 = System.nanoTime()
                intent2.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime2, eventTime2, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
                requireActivity().sendOrderedBroadcast(intent2, null)*/
/*        val eventTime = SystemClock.uptimeMillis() + 1000
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime+1, eventTime+1, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
//                            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime+1, eventTime+1, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        requireActivity().sendOrderedBroadcast(intent, null)

*/

        // In principle, we should rely on the real state of the system, not a model of that state
        if(audioManager?.isMusicActive == false) {
            var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
            audioManager!!.dispatchMediaKeyEvent(event) //TODO make this optional since it's really fast
            event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
            audioManager!!.dispatchMediaKeyEvent(event) //TODO make this optional since it's really fast
        }

/*
        val extractor = MediaExtractor()
extractor.setDataSource("path/to/media/file")
extractor.selectTrack(0) // select the first track
extractor.seekTo(1000000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC) // rewind to 1 second

         */
    }

    var shouldRewind = false

    private val audioData = ShortArray(512)
    var isSpeech = 0L

    var resumeMediaScheduledFuture: ScheduledFuture<*>? = null
    val resumeMedia = Runnable {
        // Code to be executed after the delay
        // This is your one-shot callback function
        // For example, you can update UI elements or perform other actions here
        onEndOfSpeech()
    }

    private fun startRecording() {
        Log.d("VAD", "Starting recording")
        onEndOfSpeech()
        // Start a coroutine on the IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure the audioRecord instance is properly initialized
                audioRecord?.startRecording()
                Log.d("MIC", "Started Recording " + audioData.slice(IntRange(1, 2)))
                isRecording = true
                while (isRecording) {
                    val bytesRead = audioRecord?.read(audioData, 0, audioData.size)
                    // Process audioData or send it to another component

                    vad?.setContinuousSpeechListener(audioData, object : VadListener {
                        override fun onSpeechDetected() {
                            Log.d("VAD", "Speech detected: delay media")
                            resumeMediaScheduledFuture?.cancel(true)
                            resumeMediaScheduledFuture = scheduleNext(resumeMedia, 3200)

/*                            val eventTime = SystemClock.uptimeMillis()
                            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
                            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
//                            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime+1, eventTime+1, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
                            requireActivity().sendOrderedBroadcast(intent, null)*/

                            val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)

/*                            event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
                            audioManager!!.dispatchMediaKeyEvent(event)*/

                            // TODO make this not be so dumb - if speaking persistent while already playing, will not pause again
                            // couild just fire pause all the tiem?
                            if (System.currentTimeMillis() - isSpeech > 300) {
                                //:w
                                //onBeginningOfSpeech()
                                Log.d("VAD", "PAUSE: Got speech!")
                                if(audioManager?.isMusicActive == true) {
                                    Log.d("VAD", "Music Is ACTIVE => PAUSE!")
                                    var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
                                    audioManager!!.dispatchMediaKeyEvent(event)
                                }

                                isSpeech = System.currentTimeMillis()
                            }

                            shouldRewind = true
                        }

                        override fun onNoiseDetected() {
                            if (System.currentTimeMillis() - isSpeech > 3000) {
                                onEndOfSpeech()
                                if (shouldRewind) {
                                    Log.d("VAD", "Speech ended - rewind/resume!")
                                }
                            }
                        }
                    })
                }

                vad?.close()
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                // Handle exceptions here
            }
        }
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_REQUEST = 123
    }
}