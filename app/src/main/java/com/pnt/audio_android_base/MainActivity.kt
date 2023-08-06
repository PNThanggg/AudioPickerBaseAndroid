package com.pnt.audio_android_base

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var textview2: TextView? = null
    var textview3: TextView? = null
    private var button1: Button? = null
    private var button2: Button? = null
    var seekbar1: SeekBar? = null
    var duration: String? = null
    var mediaPlayer: MediaPlayer? = null
    private var timer: ScheduledExecutorService? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        textview2 = findViewById(R.id.textView2)
        textview3 = findViewById(R.id.textView3)
        seekbar1 = findViewById(R.id.seekbar1)

        button1!!.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "audio/*"
            someActivityResultLauncher.launch(intent)
        }
        button2!!.setOnClickListener {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                    button2!!.text = "PLAY"
                    timer!!.shutdown()
                } else {
                    mediaPlayer!!.start()
                    button2!!.text = "PAUSE"
                    timer = Executors.newScheduledThreadPool(1)
                    timer!!.scheduleAtFixedRate({
                        if (mediaPlayer != null) {
                            if (!seekbar1!!.isPressed) {
                                seekbar1!!.progress = mediaPlayer!!.currentPosition
                            }
                        }
                    }, 10, 10, TimeUnit.MILLISECONDS)
                }
            }
        }
        seekbar1!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mediaPlayer != null) {
                    val millis = mediaPlayer!!.currentPosition
                    val totalSecs =
                        TimeUnit.SECONDS.convert(millis.toLong(), TimeUnit.MILLISECONDS)
                    val mins = TimeUnit.MINUTES.convert(totalSecs, TimeUnit.SECONDS)
                    val secs = totalSecs - mins * 60
                    textview3!!.text = "$mins:$secs / $duration"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer!!.seekTo(seekbar1!!.progress)
                }
            }
        })
        button2!!.isEnabled = false
    }

    private var someActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data = result.data
            if (data != null) {
                val uri = data.data
                createMediaPlayer(uri)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createMediaPlayer(uri: Uri?) {
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        try {
            mediaPlayer!!.setDataSource(applicationContext, uri!!)
            mediaPlayer!!.prepare()
            textview2!!.text = getNameFromUri(uri)
            button2!!.isEnabled = true
            val millis = mediaPlayer!!.duration
            val totalSecs = TimeUnit.SECONDS.convert(millis.toLong(), TimeUnit.MILLISECONDS)
            val mins = TimeUnit.MINUTES.convert(totalSecs, TimeUnit.SECONDS)
            val secs = totalSecs - mins * 60
            duration = "$mins:$secs"
            textview3!!.text = "00:00 / $duration"
            seekbar1!!.max = millis
            seekbar1!!.progress = 0
            mediaPlayer!!.setOnCompletionListener { releaseMediaPlayer() }
        } catch (e: IOException) {
            textview2!!.text = e.toString()
        }
    }

    @SuppressLint("Range")
    fun getNameFromUri(uri: Uri?): String {
        var fileName = ""
        val cursor: Cursor? = contentResolver.query(
            uri!!, arrayOf(
                MediaStore.Images.ImageColumns.DISPLAY_NAME
            ), null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            fileName =
                cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME))
        }
        cursor?.close()
        return fileName
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    @SuppressLint("SetTextI18n")
    private fun releaseMediaPlayer() {
        if (timer != null) {
            timer!!.shutdown()
        }
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        button2!!.isEnabled = false
        textview2!!.text = "TITLE"
        textview3!!.text = "00:00 / 00:00"
        seekbar1!!.max = 100
        seekbar1!!.progress = 0
    }
}