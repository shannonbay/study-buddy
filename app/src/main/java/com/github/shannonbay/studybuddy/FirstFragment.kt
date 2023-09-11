package com.github.shannonbay.studybuddy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecord
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var mediaSession: MediaSession
    private var _binding: FragmentFirstBinding? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var mediaControllerManager: MediaControllerManager

    private var handler = Handler(Looper.getMainLooper())
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
            Log.d("NOTIFY", intent.getStringExtra("notification_event") + "n study-buddy")
        }
    }

    private lateinit var videoView: VideoView
    private lateinit var mediaController: MediaController
    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context: Context = requireContext()
        val componentName = ComponentName(context, MediaControlNotificationListener::class.java)
        mediaSession = MediaSession(context, "hi")

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        // Initialize MediaController

        // Find the VideoView by its id

        val session = MediaSession(requireContext(), "Study")

// Set the metadata and playback state to the session
//        session.setMetadata(metadata)
//        session.setPlaybackState(playbackState)

// Activate the session
        session.isActive = true


        createSeekBar(_binding!!.seekBar)
//        mediaControllerManager = MediaControllerManager(requireContext())

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

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                Log.i("SEEK", "and you shall find" + seekBar)               // Called when the user starts interacting with the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Called when the user stops interacting with the SeekBar
 //               Log.i("SEEK", "and you shall find" + seekBar)
            }
        })
    }

    private val PERMISSION_REQUEST_MEDIA_CONTROL = 2
    private val PERMISSION_REQUEST_MANAGE_MEDIA = 3

    private var DELAY = 3100L

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
            Log.d("MIC2", "PERMISSION GRANTED!!!!!!!!!!!!!!")

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

        val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.registerAudioPlaybackCallback(
                object : AudioManager.AudioPlaybackCallback(){
                    override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                        super.onPlaybackConfigChanged(configs)
                        Log.e("GUI", "How are you? $configs")
                    }
                },
                handler
            )
        }
    /*        val mc = android.media.session.MediaController(requireContext(), mediaSession.sessionToken)
            mc.registerCallback(object : android.media.session.MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    super.onPlaybackStateChanged(state)
                    val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)
                    playMedia(audioManager)
                }
            })*/
        mediaController = CustomMediaController(requireActivity())
        // Create a MediaController object and set it to the VideoView

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
                resumeMediaScheduledFuture?.cancel(true)
                val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)

                // Request audio focus
                val result = audioManager?.requestAudioFocus(
                    null, // OnAudioFocusChangeListener (null for simplicity)
                    AudioManager.STREAM_MUSIC, // Stream type
                    AudioManager.AUDIOFOCUS_GAIN // Focus type
                )

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                    if (audioManager?.isMusicActive == true) {
                        Log.d("GUI", "Music Is ACTIVE: PAUSE")
                        var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
                        audioManager?.dispatchMediaKeyEvent(event)
                        event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
                        audioManager?.dispatchMediaKeyEvent(event)
                    }
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
                return audioRecord!!.state == AudioRecord.STATE_INITIALIZED || isRecording
            }

            override fun getBufferPercentage(): Int {
                return 100
            }

            override fun canPause(): Boolean {
                return audioRecord!!.state == AudioRecord.STATE_INITIALIZED || isRecording
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

        // Create a MediaSession
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.start()
        handler.postDelayed({
            mediaController.show(0)
        }, 100)



        Log.e("MEDIA", "" + mediaController.isShowing)
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

            Log.d("MIC", "Audio permission not granted!")
        }

        Log.d("MIC", "Listening is runinning")
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
                Log.d("TextToSpeech", "Language is not available.")
            } else {
                val ssml = i.next();//.convertToSSML(myText)
                Log.d("speech: 100", ssml)
                textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
        resumeMediaScheduledFuture?.cancel(true)
        if (audioRecord != null && audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord!!.stop()
            audioRecord!!.release()
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
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)
            rewindEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND)
            audioManager!!.dispatchMediaKeyEvent(rewindEvent)
       }

        if(!pending) {
            pending = true
            // In principle, we should rely on the real state of the system, not a model of that state
            handler.postDelayed(Runnable { pending = false; playMedia(audioManager) }, 100)
        }
   }

    var pending = false;

    private fun playMedia(audioManager: AudioManager?) {
        if (audioManager?.isMusicActive == false) {
            Log.d("VAD", "Go")
            var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
            audioManager!!.dispatchMediaKeyEvent(event) //TODO make this optional since it's really fast
            event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
            audioManager!!.dispatchMediaKeyEvent(event) //TODO make this optional since it's really fast
        }
    }

    var shouldRewind = false

    private val audioData = ShortArray(512)
    var isSpeech = 0L

    var resumeMediaScheduledFuture: ScheduledFuture<*>? = null
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
                            handler.removeCallbacks(resumeMedia)
                            handler.postDelayed(resumeMedia, DELAY)

/*                            resumeMediaScheduledFuture?.cancel(true)
                            resumeMediaScheduledFuture = scheduleNext(resumeMedia, DELAY)*/

                            val audioManager = ActivityCompat.getSystemService(requireContext(), AudioManager::class.java)

                            // TODO make this not be so dumb - if speaking persistent while already playing, will not pause again
                            // couild just fire pause all the tiem?
                            if (System.currentTimeMillis() - isSpeech > 300) {
                                Log.d("VAD", "PAUSE: Got speech!")
                                if(audioManager?.isMusicActive == true) {
                                    Log.d("VAD", "Music Is ACTIVE => PAUSE!")
                                    var event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
                                    audioManager!!.dispatchMediaKeyEvent(event)
                                    event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
                                    audioManager!!.dispatchMediaKeyEvent(event)

                                }

                            }
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