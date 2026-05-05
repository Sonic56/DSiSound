package com.dsi.sound

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SONG_INDEX     = "song_index"
        const val EXTRA_SONG_LIST      = "song_list"
        const val EXTRA_SONG_TITLES    = "song_titles"
        const val EXTRA_SONG_ARTISTS   = "song_artists"
        const val EXTRA_SONG_DURATIONS = "song_durations"
    }

    private lateinit var visualizerView: VisualizerView
    private lateinit var marioView: MarioVisualizerView
    private lateinit var chickView: ChickView
    private lateinit var waveformView: WaveformView
    private lateinit var pitchSpeedPad: PitchSpeedPad
    private lateinit var vizTabsContainer: LinearLayout
    private lateinit var songTitle: TextView
    private lateinit var artistName: TextView
    private lateinit var topSongTitle: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var backBtn: ImageButton
    private lateinit var vizToggleBtn: ImageButton
    private lateinit var pitchValue: TextView
    private lateinit var speedValue: TextView

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var songUris: List<String> = emptyList()
    private var songTitles: List<String> = emptyList()
    private var songArtists: List<String> = emptyList()
    private var songDurations: List<Long> = emptyList()
    private var currentIndex = 0
    private var currentPitch = 1.0f
    private var currentSpeed = 1.0f
    private var isPlaying = false
    private var currentVizIndex = 0

    private val progressUpdater = object : Runnable {
        override fun run() {
            updateProgress()
            waveformView.tick()
            val amp = if (isPlaying) 0.4f + (Math.random() * 0.45f).toFloat() else 0f
            visualizerView.setAmplitude(amp)
            marioView.setAmplitude(amp)
            chickView.setAmplitude(amp)
            waveformView.setAmplitude(amp)
            handler.postDelayed(this, 80)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        bindViews()
        loadIntentData()
        buildVisualizerTabs()
        setupControls()
        playSong(currentIndex)
    }

    private fun bindViews() {
        visualizerView  = findViewById(R.id.visualizerView)
        marioView       = findViewById(R.id.marioView)
        chickView       = findViewById(R.id.chickView)
        waveformView    = findViewById(R.id.waveformView)
        pitchSpeedPad   = findViewById(R.id.pitchSpeedPad)
        vizTabsContainer = findViewById(R.id.vizTabsContainer)
        songTitle       = findViewById(R.id.songTitle)
        artistName      = findViewById(R.id.artistName)
        topSongTitle    = findViewById(R.id.topSongTitle)
        seekBar         = findViewById(R.id.seekBar)
        currentTime     = findViewById(R.id.currentTime)
        totalTime       = findViewById(R.id.totalTime)
        playPauseBtn    = findViewById(R.id.playPauseBtn)
        prevBtn         = findViewById(R.id.prevBtn)
        nextBtn         = findViewById(R.id.nextBtn)
        backBtn         = findViewById(R.id.backBtn)
        vizToggleBtn    = findViewById(R.id.vizToggleBtn)
        pitchValue      = findViewById(R.id.pitchValue)
        speedValue      = findViewById(R.id.speedValue)
    }

    private fun loadIntentData() {
        currentIndex  = intent.getIntExtra(EXTRA_SONG_INDEX, 0)
        songUris      = intent.getStringArrayListExtra(EXTRA_SONG_LIST) ?: emptyList()
        songTitles    = intent.getStringArrayListExtra(EXTRA_SONG_TITLES) ?: emptyList()
        songArtists   = intent.getStringArrayListExtra(EXTRA_SONG_ARTISTS) ?: emptyList()
        val durArr    = intent.getSerializableExtra(EXTRA_SONG_DURATIONS)
        songDurations = if (durArr is ArrayList<*>) durArr.filterIsInstance<Long>() else emptyList()
    }

    private fun buildVisualizerTabs() {
        vizTabsContainer.removeAllViews()
        visualizerView.bgNames.forEachIndexed { i, name ->
            val btn = TextView(this).apply {
                text = name
                textSize = 9f
                setTextColor(0xFF223355.toInt())
                setPadding(10, 2, 10, 2)
                background = resources.getDrawable(R.drawable.viz_tab_bg, theme)
                isSelected = i == 0
                setOnClickListener { selectVizTab(i) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { marginEnd = 4 }
            vizTabsContainer.addView(btn, lp)
        }
    }

    private fun selectVizTab(index: Int) {
        currentVizIndex = index
        val isMario = visualizerView.bgNames.getOrNull(index) == "Mario"
        val isChick = index == visualizerView.bgNames.size - 1
        chickView.setShowChicks(isChick)

        if (isMario) {
            marioView.visibility = View.VISIBLE
            visualizerView.visibility = View.INVISIBLE
            if (isPlaying) marioView.startAnimation()
        } else {
            marioView.visibility = View.GONE
            marioView.stopAnimation()
            visualizerView.visibility = View.VISIBLE
            visualizerView.currentBg = index
        }

        for (i in 0 until vizTabsContainer.childCount)
            vizTabsContainer.getChildAt(i).isSelected = i == index
    }

    private fun setupControls() {
        backBtn.setOnClickListener { finish() }

        playPauseBtn.setOnClickListener {
            mediaPlayer?.let { if (it.isPlaying) pausePlayback() else resumePlayback() }
        }

        prevBtn.setOnClickListener {
            if (currentIndex > 0) playSong(currentIndex - 1)
        }

        nextBtn.setOnClickListener {
            if (currentIndex < songUris.size - 1) playSong(currentIndex + 1)
        }

        vizToggleBtn.setOnClickListener {
            selectVizTab((currentVizIndex + 1) % visualizerView.bgNames.size)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val dur = mediaPlayer?.duration ?: return
                    mediaPlayer?.seekTo((dur * progress / 1000f).toInt())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // 2D pitch/speed pad
        pitchSpeedPad.onPitchSpeedChanged = { pitch, speed ->
            currentPitch = pitch
            currentSpeed = speed
            pitchValue.text = "%.1fx".format(pitch)
            speedValue.text = "%.1fx".format(speed)
            applyPlaybackParams()
        }

        // Double-tap pad to reset
        pitchSpeedPad.setOnLongClickListener {
            pitchSpeedPad.resetToCenter()
            currentPitch = 1f
            currentSpeed = 1f
            pitchValue.text = "1.0x"
            speedValue.text = "1.0x"
            applyPlaybackParams()
            true
        }
    }

    private fun applyPlaybackParams() {
        try {
            mediaPlayer?.playbackParams = PlaybackParams()
                .setPitch(currentPitch)
                .setSpeed(currentSpeed)
        } catch (_: Exception) {}
    }

    private fun playSong(index: Int) {
        if (songUris.isEmpty() || index !in songUris.indices) return
        currentIndex = index

        val title  = songTitles.getOrElse(index) { "Unknown" }
        val artist = songArtists.getOrElse(index) { "" }
        val artistDisplay = if (artist.isEmpty() || artist == "<unknown>") "Unknown Artist" else artist

        songTitle.text    = title
        artistName.text   = artistDisplay
        topSongTitle.text = "♪  $title  —  $artistDisplay"
        totalTime.text    = formatTime(songDurations.getOrElse(index) { 0L })

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlayerActivity, Uri.parse(songUris[index]))
            prepare()
            setOnCompletionListener {
                if (currentIndex < songUris.size - 1) playSong(currentIndex + 1)
                else pausePlayback()
            }
            start()
            applyPlaybackParams()
        }
        onPlaybackStarted()
    }

    private fun onPlaybackStarted() {
        isPlaying = true
        playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
        visualizerView.startAnimation()
        chickView.startAnimation()
        if (marioView.visibility == View.VISIBLE) marioView.startAnimation()
        handler.post(progressUpdater)
    }

    private fun pausePlayback() {
        isPlaying = false
        mediaPlayer?.pause()
        playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
        visualizerView.stopAnimation()
        chickView.stopAnimation()
        marioView.stopAnimation()
        handler.removeCallbacks(progressUpdater)
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        applyPlaybackParams()
        onPlaybackStarted()
    }

    private fun updateProgress() {
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) return
        val pos = mp.currentPosition
        val dur = mp.duration
        if (dur > 0) seekBar.progress = (pos * 1000L / dur).toInt()
        currentTime.text = formatTime(pos.toLong())
    }

    private fun formatTime(ms: Long): String {
        val secs = (ms / 1000) % 60
        val mins = (ms / 1000) / 60
        return "%d:%02d".format(mins, secs)
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer?.isPlaying == true) pausePlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
