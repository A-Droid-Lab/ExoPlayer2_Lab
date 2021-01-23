package com.groodysoft.exoplayerserviceexample

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.load
import com.airbnb.lottie.LottieDrawable
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.groodysoft.exoplayerserviceexample.MainActivity.Companion.playerServiceIsBound
import com.groodysoft.exoplayerserviceexample.service.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.custom_audio_player.*
import kotlinx.android.synthetic.main.exo_playback_control_view.*

class MainActivity : AppCompatActivity() {

    object Companion {
        var playerServiceIsBound = false //서비스가 바인드 되어있는지를 확인하는 boolean 속
    }

    var wasPlayerServiceBound = false //서비스가 바인드를 된 적 있는지 여부 확인
    var playerService: PlayerService? = null

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.MyLocalBinder
            playerService = binder.getService()
            playerServiceIsBound = true
            bindPlayer()

            if (!wasPlayerServiceBound) {
                //서비스가 바인드 되어 있지 않다면?
                sendServiceIntent(SERVICE_ACTION_CONTENT_TRACK_LIST, MainApplication.gson.toJson(SampleCatalog.tracks))
                sendServiceIntent(SERVICE_ACTION_PLAY)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            playerServiceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //player view setting
        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false
        playerView.useArtwork = false //audio stream의 경우, false를 하는 게 맞음.
        playerView.setShowShuffleButton(true)//suffle button 유무
        playerView.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL)

        // bind to the service whether it's already running or not
        // save a flag so we know to initialize and play the content
        // application 동작과 상관 없이 서비스를 바인드 시킨다.
        // 플레그를 저장함으로써 우린 콘텐츠를 플레이하고 초기화 여부를 알 수 있다.
        wasPlayerServiceBound = playerServiceIsBound
        val intent = Intent(this, PlayerService::class.java)
        bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE)

        LocalBroadcastManager.getInstance(this).registerReceiver(metadataReceiver, metadataIntentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(metadataReceiver)
    }

    override fun onResume() {
        super.onResume()
        if (playerServiceIsBound) {
            bindPlayer()
        }
    }

    private fun bindPlayer() {
        //player view setting
        lav_loading.apply {
            repeatCount = LottieDrawable.INFINITE
            playAnimation()
        }

        playerView.player = playerService?.player?.apply {
            addListener(object : EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                    when (playbackState){
                        STATE_READY -> {
                            lav_loading.visibility = View.GONE
                        }
                        STATE_BUFFERING -> {
                            lav_loading.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onPlayerError(error: ExoPlaybackException) {
                    super.onPlayerError(error)
                    Log.d(" @@@@@@ 에러 발생 @@@@@", " XXXXX ERROR XXXXX")
                }
            })
        }

        //control view setting
        playerControlView.apply {
            player = playerService?.player
            showTimeoutMs = 0
        }
        playerView.showController()
        LocalBroadcastManager.getInstance(MainApplication.context).sendBroadcast(Intent(ACTION_METADATA))
    }

    private val metadataIntentFilter = IntentFilter(ACTION_METADATA)
    private val metadataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_METADATA == intent.action) {

                playerService?.player?.let {
                    // based on the boolean in the DescriptionAdapter, this will either
                    // extract the metadata (title, album, cover art) from the embedded
                    // ID3 tags in the HTTP stream, or load it from the local data in the
                    // sample catalog
                    // DescriptionAdapter의 boolean값에 기반하여, HTTP Stream으로 metadata를 가져오거나 Local에서 불려지게 되어 있음.
                    trackTitle.text = DescriptionAdapter.getCurrentContentTitle(it)
                    trackSubtitle.text = DescriptionAdapter.getCurrentContentText(it)

                    custom_music_controller_tv_song.text = DescriptionAdapter.getCurrentContentTitle(it)


                    if (DescriptionAdapter.useStreamExtraction) {
                        //stream을 생성하여 동작을 한다면
                        //coil을 쓰기에 가능한 lamda 식 함수
                        coverArtImageView.load(DescriptionAdapter.getCurrentLargeIcon()) {
                            placeholder(R.drawable.album_art_placeholder)
                            crossfade(true)
                            fallback(R.drawable.album_art_placeholder)
                            error(R.drawable.album_art_placeholder)
                        }

                    } else {
                        //로컬로 동작을 한다면?
                        val track = SampleCatalog.tracks[it.currentWindowIndex]
                        coverArtImageView.load(track.frontCoverUrl) {
                            placeholder(R.drawable.album_art_placeholder)
                            crossfade(true)
                            fallback(R.drawable.album_art_placeholder)
                            error(R.drawable.album_art_placeholder)
                        }
                    }
                }
            }
        }
    }
}
