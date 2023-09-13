package com.github.shannonbay.studybuddy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.VideoView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.shannonbay.studybuddy.databinding.FragmentFirstBinding
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadListener
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


private const val RESUME_MEDIA = "resumeMedia"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MediaControlFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentFirstBinding? = null
    private var textToSpeech: TextToSpeech? = null

    /**
     * Microphone setup for VAD
     */

    private val audioSource = MediaRecorder.AudioSource.DEFAULT
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
            Log.d("NOTIFY", intent.getStringExtra("notification_event") + "n study-buddy")
        }
    }

    private lateinit var videoView: VideoView
    private lateinit var mediaController: MediaController
    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context: Context = requireContext()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)


        // Find the VideoView by its id
        createSeekBar(_binding!!.seekBar)

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

        val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
        audioManager?.isBluetoothScoOn = true
        audioManager?.startBluetoothSco()
        audioManager?.mode = AudioManager.MODE_NORMAL

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

    private fun createSeekBar(seekBar: SeekBar) {
        // Initialize the SeekBar
        Log.i("SEEK", "INIT ")               // Called when the user starts interacting with the SeekBar
        DELAY = (Math.pow(seekBar.progress.toDouble(), 2.0)).toLong()
        _binding?.textviewFirst?.text = DELAY.toString()
        // Set an OnSeekBarChangeListener to listen for user interactions
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Handle progress change (value selected by the user)
                // 'progress' contains the selected value
                Log.i("SEEK", "and you shall find" + progress)
                DELAY = (Math.pow(progress.toDouble(), 2.0)).toLong()

                _binding?.textviewFirst?.text = DELAY.toString()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })
    }

    private val PERMISSION_REQUEST_MEDIA_CONTROL = 2
    private val PERMISSION_REQUEST_MANAGE_MEDIA = 3

    private var DELAY = 3100L

    private val utteranceProgressListener = object : UtteranceProgressListener() {
       override fun onStart(utteranceId: String?) {
            Log.d("speech", "Starting now")
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

        // Initialize MediaController

        // Create a MediaController object and set it to the VideoView
        mediaController = CustomMediaController(requireActivity())

        mediaController.setMediaPlayer(object : MediaController.MediaPlayerControl {

            override fun start() {
                val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
                if(audioManager?.isMusicActive == false) {
                    Log.d("GUI", "Music inactive: PLAY")
                    playMedia(audioManager)
                }
                startRecording()
                isRecording = true
                mediaController.refreshDrawableState()
                mediaController.invalidate()
            }

            override fun pause() {
                handler.cancel(RESUME_MEDIA)
                val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)

                // Request audio focus
                val result = audioManager?.requestAudioFocus(
                    null, // OnAudioFocusChangeListener (null for simplicity)
                    AudioManager.STREAM_MUSIC, // Stream type
                    AudioManager.AUDIOFOCUS_GAIN // Focus type
                )

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    sendPauseKeyEvent(audioManager)
                    audioManager?.abandonAudioFocus(null)
                }
                stopRecording()
                mediaController.refreshDrawableState()
                mediaController.invalidate()
            }

            override fun getDuration(): Int {
                return mediaPlayer.duration
            }

            override fun getCurrentPosition(): Int {
                return mediaPlayer.currentPosition
            }

            override fun seekTo(p0: Int) {
                mediaPlayer.seekTo(p0)
            }

            override fun isPlaying(): Boolean {
                return audioRecord?.state == AudioRecord.STATE_INITIALIZED || isRecording
            }

            override fun getBufferPercentage(): Int {
                return 100
            }

            override fun canPause(): Boolean {
                return audioRecord?.state == AudioRecord.STATE_INITIALIZED || isRecording
            }

            override fun canSeekBackward(): Boolean {
                return false
            }

            override fun canSeekForward(): Boolean {
                return false
            }

            override fun getAudioSessionId(): Int {
                return 76567
            }

            // Implement other required methods
        })
        videoView = _binding!!.videoView

        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.start()

        // Extension function for Handler that supports debouncing delayed jobs with unique keys

        TaskScheduler.reschedule(100 ) { mediaController.show(0)}

        Log.e("MEDIA", "" + mediaController.isShowing)
        textToSpeech = TextToSpeech(requireContext(), this)

        textToSpeech?.setOnUtteranceProgressListener(utteranceProgressListener)

        // Initialize the SpeechRecognizer
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with your task
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )

            Log.d("MIC", "Audio permission not granted!")
        }

        Log.d("MIC", "Listening is runinning")
        binding.buttonFirst.addOnCheckedChangeListener { button, isChecked ->
            if (isChecked){
                stopRecording()
                button.icon = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_mic_off_24)
            } else {
                startRecording()
                button.icon = ContextCompat.getDrawable(requireContext(),R.drawable.baseline_mic_24)
            }
        }
        startRecording()
    }

    private fun sendPauseKeyEvent(audioManager: AudioManager?) {
        if(audioManager?.isMusicActive == true) {
            handler.debounceAfter("pause", 1000) {
                Log.d("GUI", "Music Is ACTIVE: PAUSE")
                var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
                audioManager?.dispatchMediaKeyEvent(event)
                event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
                audioManager?.dispatchMediaKeyEvent(event)
            }
        }
    }

    private val myText = "In the year that King Uzziah died I saw the Lord sitting on a high and lofty throne, and the train of His robe filled the temple."

    private val i = TextToSSMLIterator(myText);
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for speech synthesis (e.g., English)
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TextToSpeech", "Language is not available.")
            } else {
                val ssml = i.next();//.convertToSSML(myText)
                Log.d("speech: 100", ssml)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    var startTime: Long = 0
                    override fun onStart(utteranceId: String) {
                        Log.d("TTS", "Starting new speech")
                        startTime = System.currentTimeMillis()
                    }

                    override fun onDone(p0: String?) {
                        Log.d("TTS", "Utterance done: " );
                        val delayMillis = System.currentTimeMillis() - startTime
                        Log.d("TTS", "Next in " + delayMillis / 1000 + "s" );
                        scheduleNext(delayMillis/3)
                        // Called when speech synthesis is completed.
                    }

                    override fun onError(utteranceId: String) {
                        // Called when there's an error in speech synthesis.
                    }
                })
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
            Log.d("TTS", ssml)
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
        handler.cancel(RESUME_MEDIA)
        if (audioRecord != null && audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord?.stop()
            audioRecord?.release()
        }
        isRecording = false
    }
    override fun onStop() {
        super.onStop()
        Log.d("VAD", "onStop called!")
        //stopRecording()

        if (textToSpeech != null ) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }

    override fun onDestroy() {
        Log.d("VAD", "onDestroy called!")
        stopRecording()

        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("VAD", "onDestroyView called!")
        stopRecording()
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }

        _binding = null
    }

    @Synchronized fun onEndOfSpeech() {
//        Log.d("VAD", "You went quiet! REWIND and PLAY")
        val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)

        if(shouldRewind) {
            shouldRewind = false

           Log.d("VAD", "REWIND BROADCAST")

            var rewindEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND)
            audioManager?.dispatchMediaKeyEvent(rewindEvent)
            rewindEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND)
            audioManager?.dispatchMediaKeyEvent(rewindEvent)
        }

        // In principle, we should rely on the real state of the system, not a model of that state
        playMedia(audioManager)
    }

    val handler = Debouncer()

    private fun playMedia(audioManager: AudioManager?) {
        handler.debounceAfter("playMedia", 1000) {
            if (audioManager?.isMusicActive == false) {
                Log.d("VAD", "Go")
                var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
                audioManager?.dispatchMediaKeyEvent(event) //TODO make this optional since it's really fast
                event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
                audioManager?.dispatchMediaKeyEvent(event) //TODO make this optional since it's really fast
            }
        }
    }

    var shouldRewind = false

    private val audioData = ShortArray(512)
    var isSpeech = 0L

    val resumeMedia = Runnable {
        Log.e("VAD", "Resuming via callback!!!!!!!!!!!!!")
        onEndOfSpeech()
    }

    private fun startRecording() {
        Log.d("VAD", "Starting recording")
        onEndOfSpeech()
        isRecording = true
        // Start a coroutine on the IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure the audioRecord instance is properly initialized
                audioRecord?.startRecording()
                Log.d("MIC", "Started Recording " + audioData.slice(IntRange(1, 2)))

                while (isRecording) {
                    val bytesRead = audioRecord?.read(audioData, 0, audioData.size)
                    // Process audioData or send it to another component

                    vad?.setContinuousSpeechListener(audioData, object : VadListener {
                        override fun onSpeechDetected() {
                            Log.d("VAD", "Speech detected: delay media")
                            handler.debounceUntil(RESUME_MEDIA, DELAY) {resumeMedia}

                            val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)

                            // TODO make this not be so dumb - if speaking persistent while already playing, will not pause again
                            // couild just fire pause all the tiem?
                            sendPauseKeyEvent(audioManager)
                            isSpeech = System.currentTimeMillis()

                            shouldRewind = true
                        }

                        override fun onNoiseDetected() {
                            if (System.currentTimeMillis() - isSpeech > DELAY) {
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