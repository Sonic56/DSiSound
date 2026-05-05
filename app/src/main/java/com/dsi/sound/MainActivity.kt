package com.dsi.sound

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var songList: RecyclerView
    private lateinit var permissionView: View
    private lateinit var emptyView: View
    private lateinit var songCountText: TextView
    private lateinit var grantPermissionBtn: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) loadSongs()
        else showPermissionView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songList = findViewById(R.id.songList)
        permissionView = findViewById(R.id.permissionView)
        emptyView = findViewById(R.id.emptyView)
        songCountText = findViewById(R.id.songCountText)
        grantPermissionBtn = findViewById(R.id.grantPermissionBtn)

        songList.layoutManager = LinearLayoutManager(this)

        grantPermissionBtn.setOnClickListener { requestPermission() }

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> loadSongs()
            else -> requestPermission()
        }
    }

    private fun requestPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissionLauncher.launch(permissions)
    }

    private fun loadSongs() {
        permissionView.visibility = View.GONE
        val songs = MusicRepository.loadSongs(this)

        if (songs.isEmpty()) {
            songList.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            songCountText.text = "No songs"
            return
        }

        emptyView.visibility = View.GONE
        songList.visibility = View.VISIBLE
        songCountText.text = "${songs.size} songs"

        songList.adapter = SongAdapter(songs) { song, index ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_SONG_INDEX, index)
                putParcelableArrayListExtra(PlayerActivity.EXTRA_SONG_LIST,
                    ArrayList(songs.map { it.uri.toString() }))
                putExtra(PlayerActivity.EXTRA_SONG_TITLES,
                    ArrayList(songs.map { it.title }))
                putExtra(PlayerActivity.EXTRA_SONG_ARTISTS,
                    ArrayList(songs.map { it.artist }))
                putExtra(PlayerActivity.EXTRA_SONG_DURATIONS,
                    ArrayList(songs.map { it.duration }))
            }
            startActivity(intent)
        }
    }

    private fun showPermissionView() {
        songList.visibility = View.GONE
        emptyView.visibility = View.GONE
        permissionView.visibility = View.VISIBLE
    }
}
