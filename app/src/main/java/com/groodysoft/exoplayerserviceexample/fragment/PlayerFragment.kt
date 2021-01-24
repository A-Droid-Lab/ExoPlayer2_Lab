package com.groodysoft.exoplayerserviceexample.fragment

import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.airbnb.lottie.LottieDrawable
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.groodysoft.exoplayerserviceexample.MainActivity
import com.groodysoft.exoplayerserviceexample.MainActivity.Companion.playerServiceIsBound
import com.groodysoft.exoplayerserviceexample.MainApplication
import com.groodysoft.exoplayerserviceexample.R
import com.groodysoft.exoplayerserviceexample.SampleCatalog
import com.groodysoft.exoplayerserviceexample.adapter.PlayerImageSlideAdapter
import com.groodysoft.exoplayerserviceexample.service.*
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.fragment_player.*


class PlayerFragment : BottomSheetDialogFragment() {

    var wasPlayerServiceBound = false //서비스가 바인드를 된 적 있는지 여부 확인
    var playerService: PlayerService? = null
    lateinit var intent : Intent
    var imageSlideAdapter = PlayerImageSlideAdapter()

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.MyLocalBinder
            playerService = binder.getService()
            playerServiceIsBound = true
            bindPlayer()
            lav_loading.apply {
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }

            if (!wasPlayerServiceBound) {
                //서비스가 바인드 되어 있지 않다면?
                requireContext().sendServiceIntent(SERVICE_ACTION_CONTENT_TRACK_LIST, MainApplication.gson.toJson(SampleCatalog.tracks))
                requireContext().sendServiceIntent(SERVICE_ACTION_PLAY)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            playerServiceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wasPlayerServiceBound = MainActivity.Companion.playerServiceIsBound
        intent = Intent(requireContext(), PlayerService::class.java)
        requireContext().bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE)

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(metadataReceiver, metadataIntentFilter)
    }

    private fun initAdapter(){
        vp2.apply {
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 3
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            adapter = imageSlideAdapter
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val d = it as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            Log.d("BS STATE ::: ", "STATE _ HIDDEN")
                            dismiss()
                        }
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            Log.d("BS STATE ::: ", "HALF _EXPAND")
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            Log.d("BS STATE ::: ", " STATE _ COLLAPSEsD")
                            dismiss()
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            Log.d("BS STATE ::: ", " STATE _ EXPANDED")
                        }
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
        return dialog
    }

    private fun playerSet(){
        Log.d("플레이어서비스이즈 바운드 frag :: ", playerServiceIsBound.toString())

        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false
        playerView.useArtwork = false //audio stream의 경우, false를 하는 게 맞음.
        playerView.setShowShuffleButton(true)//suffle button 유무
        playerView.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //player view setting
        playerSet()
        initAdapter()
    }

    fun bindPlayer(){
        if (playerServiceIsBound){
            lav_loading.visibility = View.GONE
        }

        playerView.player = playerService?.player?.apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                    when (playbackState){
                        Player.STATE_READY -> {
                            lav_loading.visibility = View.GONE
                        }
                        Player.STATE_BUFFERING -> {
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
        playerView.showController()
    }

    override fun onResume() {
        super.onResume()
        if (playerServiceIsBound) {
            bindPlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(metadataReceiver)
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

                    if (DescriptionAdapter.useStreamExtraction) {
                        //stream을 생성하여 동작을 한다면
                        //coil을 쓰기에 가능한 lamda 식 함수
//                        coverArtImageView.load(DescriptionAdapter.getCurrentLargeIcon()) {
//                            placeholder(R.drawable.album_art_placeholder)
//                            crossfade(true)
//                            fallback(R.drawable.album_art_placeholder)
//                            error(R.drawable.album_art_placeholder)
//                        }
                        //imageSlideAdapter.setPlayerViewPagerData(DescriptionAdapter.getCurrentLargeIcon())

                    } else {
                        //로컬로 동작을 한다면?
                        val track = SampleCatalog.tracks[it.currentWindowIndex].frontCoverUrl
                        imageSlideAdapter.setPlayerViewPagerData(track)
                        imageSlideAdapter.refresh()
//                        coverArtImageView.load(track.frontCoverUrl[0]) {
//                            placeholder(R.drawable.album_art_placeholder)
//                            crossfade(true)
//                            fallback(R.drawable.album_art_placeholder)
//                            error(R.drawable.album_art_placeholder)
//                        }
                    }
                }
            }
        }
    }



    companion object {
        @JvmStatic
        fun newInstance() = PlayerFragment()
    }
}