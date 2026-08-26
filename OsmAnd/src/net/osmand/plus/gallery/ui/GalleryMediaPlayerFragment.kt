package net.osmand.plus.gallery.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.osmand.PlatformUtil
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.plus.utils.InsetTarget.Type
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import kotlin.math.abs
import kotlin.math.min

class GalleryMediaPlayerFragment : BaseFullScreenFragment() {

	private var position = 0
	private var controller: GalleryPagerController? = null
	private var mediaItem: MediaItem? = null

	private lateinit var rootView: FrameLayout
	private lateinit var textureView: TextureView
	private lateinit var posterView: ImageView
	private lateinit var audioArtwork: ImageView
	private lateinit var playPauseButton: ImageView
	private lateinit var playbackBar: LinearLayout
	private lateinit var seekBar: SeekBar
	private lateinit var positionTime: TextView
	private lateinit var durationTime: TextView
	private lateinit var seekBackIndicator: TextView
	private lateinit var seekForwardIndicator: TextView

	private var player: MediaPlayer? = null
	private var surface: Surface? = null
	private var audioManager: AudioManager? = null
	private var audioFocusRequest: AudioFocusRequest? = null
	private var resumeAfterFocusGain = false
	private var prepared = false
	private var playWhenPrepared = false
	private var playbackStarted = false
	private var userSeeking = false
	private var controlsVisible = true

	private var pendingSeekMs = -1
	private var progressAnimator: ObjectAnimator? = null

	private var seekBurstDirection = 0
	private var seekBurstTotalMs = 0
	private var seekBurstUntil = 0L
	private var tapConsumedBySeek = false

	private var zoomScale = 1f

	private val handler = Handler(Looper.getMainLooper())
	private val isVideo: Boolean
		get() = mediaItem?.type == MediaType.VIDEO

	private val hideUiRunnable = Runnable {
		if (player?.isPlaying == true) {
			setUiVisible(false)
		}
	}

	private val progressRunnable = object : Runnable {
		override fun run() {
			tickProgress()
			if (player?.isPlaying == true) {
				handler.postDelayed(this, PROGRESS_TICK_MS)
			}
		}
	}

	private val hideBackIndicatorRunnable = Runnable { fadeOutIndicator(seekBackIndicator) }
	private val hideForwardIndicatorRunnable = Runnable { fadeOutIndicator(seekForwardIndicator) }
	private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
		handler.post { handleAudioFocusChange(focusChange) }
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		position = savedInstanceState?.getInt(SELECTED_POSITION_KEY)
			?: arguments?.getInt(SELECTED_POSITION_KEY) ?: 0
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		updateNightMode()
		controller = GalleryPagerController.getExistingInstance(app) ?: return null
		mediaItem = controller?.mediaItems?.getOrNull(position)?.mediaItem ?: return null

		rootView = inflate(R.layout.gallery_media_player_item, container, false) as FrameLayout
		textureView = rootView.findViewById(R.id.texture_view)
		posterView = rootView.findViewById(R.id.poster)
		audioArtwork = rootView.findViewById(R.id.audio_artwork)
		playPauseButton = rootView.findViewById(R.id.play_pause_button)
		playbackBar = rootView.findViewById(R.id.playback_bar)
		seekBar = rootView.findViewById(R.id.seek_bar)
		positionTime = rootView.findViewById(R.id.position_time)
		durationTime = rootView.findViewById(R.id.duration_time)
		seekBackIndicator = rootView.findViewById(R.id.seek_back_indicator)
		seekForwardIndicator = rootView.findViewById(R.id.seek_forward_indicator)

		setupArtwork()
		setupControls()
		setupGestures()
		setupSurface()
		updateProgressTexts(0, 0)

		return rootView
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		return super.getInsetTargets().apply { removeType(Type.ROOT_INSET) }
	}

	private fun setupArtwork() {
		val item = mediaItem ?: return
		if (isVideo) {
			audioArtwork.visibility = View.GONE
			app.galleryHelper.posterLoader.loadPoster(item) { poster ->
				if (poster != null && !playbackStarted) {
					posterView.setImageBitmap(poster)
				}
			}
		} else {
			textureView.visibility = View.GONE
			posterView.visibility = View.GONE
			audioArtwork.visibility = View.VISIBLE
			audioArtwork.setImageDrawable(app.uiUtilities.getIcon(R.drawable.ic_action_music_note))
			audioArtwork.imageTintList = ColorStateList.valueOf(whiteColor())
		}
	}

	private fun setupControls() {
		playPauseButton.imageTintList = ColorStateList.valueOf(whiteColor())
		updatePlayButtonIcon(false)
		playPauseButton.setOnClickListener { togglePlayPause() }

		seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					positionTime.text = formatTime(progress)
				}
			}

			override fun onStartTrackingTouch(bar: SeekBar) {
				userSeeking = true
				progressAnimator?.cancel()
				handler.removeCallbacks(hideUiRunnable)
			}

			override fun onStopTrackingTouch(bar: SeekBar) {
				userSeeking = false
				if (prepared) {
					preciseSeekTo(bar.progress)
				}
				rescheduleAutoHideIfPlaying()
			}
		})
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun setupGestures() {
		val gestureDetector = GestureDetector(requireContext(),
			object : GestureDetector.SimpleOnGestureListener() {
				override fun onSingleTapUp(e: MotionEvent): Boolean {
					val direction = seekZoneDirection(e.x)
					if (direction != 0 && SystemClock.uptimeMillis() < seekBurstUntil) {
						performBurstSeek(direction)
						tapConsumedBySeek = true
					}
					return false
				}

				override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
					if (tapConsumedBySeek) {
						tapConsumedBySeek = false
						return true
					}
					setUiVisible(!controlsVisible)
					rescheduleAutoHideIfPlaying()
					return true
				}

				override fun onDoubleTap(e: MotionEvent): Boolean {
					tapConsumedBySeek = false
					val direction = seekZoneDirection(e.x)
					if (direction != 0) {
						performBurstSeek(direction)
					} else {
						togglePlayPause()
					}
					return true
				}

				override fun onScroll(
					e1: MotionEvent?, e2: MotionEvent,
					distanceX: Float, distanceY: Float
				): Boolean {
					if (!isVideo || zoomScale <= 1f) return false
					panVideoBy(distanceX, distanceY)
					return true
				}
			})

		val scaleDetector = ScaleGestureDetector(requireContext(),
			object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
				override fun onScale(detector: ScaleGestureDetector): Boolean {
					if (!isVideo) return false
					applyZoom((zoomScale * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM))
					return true
				}

				override fun onScaleEnd(detector: ScaleGestureDetector) {
					if (isVideo && zoomScale < SNAP_BACK_ZOOM) {
						resetZoom(animated = true)
					}
				}
			})

		rootView.setOnTouchListener { v, event ->
			if (isVideo) {
				scaleDetector.onTouchEvent(event)
			}
			gestureDetector.onTouchEvent(event)
			// prevent viewpager from stealing swipes while zoomed or pinching
			if (zoomScale > 1f || event.pointerCount > 1) {
				v.parent?.requestDisallowInterceptTouchEvent(true)
			}
			true
		}
	}

	private fun setupSurface() {
		if (!isVideo) return
		textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
			override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {
				surface = Surface(texture)
				player?.setSurface(surface)
			}

			override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) {}

			override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
				player?.setSurface(null)
				surface?.release()
				surface = null
				return true
			}

			override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
		}
	}

	private fun ensurePlayer(): MediaPlayer? {
		player?.let { return it }
		val item = mediaItem ?: return null
		val uri = app.galleryHelper.mediaSourceResolver.getPlaybackUri(item)
		if (uri == null) {
			handlePlaybackError("Playback uri is unavailable for media item: ${item.id}")
			return null
		}
		val mp = MediaPlayer()
		return try {
			mp.setAudioAttributes(mediaAudioAttributes())
			mp.setDataSource(app, uri)
			mp.setOnPreparedListener { onPrepared(it) }
			mp.setOnCompletionListener { onCompletion() }
			mp.setOnSeekCompleteListener { completed ->
				val pending = pendingSeekMs
				if (pending >= 0 &&
					abs(completed.currentPosition - pending) <= SEEK_COMPLETE_TOLERANCE_MS
				) {
					pendingSeekMs = -1
					setProgressImmediate(completed.currentPosition)
				}
			}
			mp.setOnErrorListener { _, what, extra ->
				handlePlaybackError("MediaPlayer error: what=$what extra=$extra")
				true
			}
			mp.setOnVideoSizeChangedListener { _, w, h -> adjustVideoSize(w, h) }
			surface?.let { mp.setSurface(it) }
			mp.prepareAsync()
			player = mp
			mp
		} catch (e: Exception) {
			try {
				mp.release()
			} catch (_: Exception) {
			}
			handlePlaybackError("Failed to prepare media player", e)
			null
		}
	}

	private fun onPrepared(mp: MediaPlayer) {
		if (view == null) return
		prepared = true
		seekBar.max = mp.duration
		updateProgressTexts(0, mp.duration)
		adjustVideoSize(mp.videoWidth, mp.videoHeight)
		if (playWhenPrepared) {
			playWhenPrepared = false
			startPlayback()
		}
	}

	private fun onCompletion() {
		handler.removeCallbacks(progressRunnable)
		handler.removeCallbacks(hideUiRunnable)
		rootView.keepScreenOn = false
		abandonAudioFocus()
		updatePlayButtonIcon(false)
		player?.let { setProgressImmediate(it.duration) }
		setUiVisible(true)
	}

	private fun togglePlayPause() {
		val mp = ensurePlayer() ?: return
		if (!prepared) {
			playWhenPrepared = !playWhenPrepared
			updatePlayButtonIcon(playWhenPrepared)
			animatePlayButtonPop()
			return
		}
		if (mp.isPlaying) {
			pausePlayback()
		} else {
			startPlayback()
		}
	}

	private fun startPlayback() {
		val mp = player ?: return
		if (!requestAudioFocus()) {
			handlePlaybackError("Audio focus request was denied")
			return
		}
		try {
			mp.start()
		} catch (e: Exception) {
			handlePlaybackError("Failed to start media playback", e)
			return
		}
		resumeAfterFocusGain = false
		playbackStarted = true
		rootView.keepScreenOn = true
		updatePlayButtonIcon(true)
		animatePlayButtonPop()
		hidePoster()
		handler.removeCallbacks(progressRunnable)
		handler.post(progressRunnable)
		scheduleAutoHide()
	}

	private fun pausePlayback(abandonFocus: Boolean = true) {
		val mp = player ?: return
		playWhenPrepared = false
		if (mp.isPlaying) {
			mp.pause()
		}
		if (abandonFocus) {
			abandonAudioFocus()
		}
		rootView.keepScreenOn = false
		updatePlayButtonIcon(false)
		animatePlayButtonPop()
		handler.removeCallbacks(progressRunnable)
		handler.removeCallbacks(hideUiRunnable)
		setUiVisible(true)
	}

	private fun mediaAudioAttributes(): AudioAttributes =
		AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_MEDIA)
			.setContentType(
				if (isVideo) AudioAttributes.CONTENT_TYPE_MOVIE
				else AudioAttributes.CONTENT_TYPE_MUSIC
			)
			.build()

	private fun requestAudioFocus(): Boolean {
		val manager = audioManager ?: (app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
			?.also { audioManager = it }
			?: return false
		val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
				.setAudioAttributes(mediaAudioAttributes())
				.setAcceptsDelayedFocusGain(false)
				.setOnAudioFocusChangeListener(audioFocusChangeListener)
				.build()
				.also { audioFocusRequest = it }
			manager.requestAudioFocus(request)
		} else {
			@Suppress("DEPRECATION")
			manager.requestAudioFocus(
				audioFocusChangeListener,
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN
			)
		}
		return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
	}

	private fun abandonAudioFocus() {
		val manager = audioManager ?: return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
		} else {
			@Suppress("DEPRECATION")
			manager.abandonAudioFocus(audioFocusChangeListener)
		}
		resumeAfterFocusGain = false
	}

	private fun handleAudioFocusChange(focusChange: Int) {
		when (focusChange) {
			AudioManager.AUDIOFOCUS_GAIN -> {
				if (resumeAfterFocusGain && prepared && player?.isPlaying != true) {
					startPlayback()
				}
				resumeAfterFocusGain = false
			}
			AudioManager.AUDIOFOCUS_LOSS,
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
				resumeAfterFocusGain =
					focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT &&
							(player?.isPlaying == true || playWhenPrepared)
				playWhenPrepared = false
				pausePlayback(abandonFocus = false)
			}
		}
	}

	private fun handlePlaybackError(message: String, throwable: Throwable? = null) {
		if (throwable != null) {
			LOG.warn(message, throwable)
		} else {
			LOG.warn(message)
		}
		handler.removeCallbacks(progressRunnable)
		handler.removeCallbacks(hideUiRunnable)
		progressAnimator?.cancel()
		progressAnimator = null
		pendingSeekMs = -1
		playWhenPrepared = false
		playbackStarted = false
		rootView.keepScreenOn = false
		abandonAudioFocus()
		releasePlayer()
		if (view != null) {
			updatePlayButtonIcon(false)
			setUiVisible(true)
			app.showToastMessage(R.string.gallery_media_playback_error)
		}
	}

	private fun releasePlayer() {
		try {
			player?.release()
		} catch (_: Exception) {
		}
		player = null
		prepared = false
	}

	private fun seekZoneDirection(x: Float): Int {
		val width = rootView.width
		return when {
			x < width / 3f -> -1
			x > width * 2 / 3f -> 1
			else -> 0
		}
	}

	private fun performBurstSeek(direction: Int) {
		if (player == null || !prepared) return
		val now = SystemClock.uptimeMillis()
		if (direction != seekBurstDirection || now >= seekBurstUntil) {
			seekBurstTotalMs = 0
			if (direction != seekBurstDirection) {
				hideIndicatorNow(if (direction > 0) seekBackIndicator else seekForwardIndicator)
			}
		}
		seekBurstDirection = direction
		seekBurstUntil = now + SEEK_BURST_WINDOW_MS
		seekBurstTotalMs += SEEK_STEP_MS
		seekRelative(direction * SEEK_STEP_MS)
		showSeekIndicator(direction > 0, seekBurstTotalMs)
		rescheduleAutoHideIfPlaying()
	}

	private fun seekRelative(deltaMs: Int) {
		val mp = player ?: return
		if (!prepared) return
		val target = (currentDisplayPositionMs() + deltaMs).coerceIn(0, mp.duration)
		preciseSeekTo(target)
	}

	private fun preciseSeekTo(positionMs: Int) {
		val mp = player ?: return
		pendingSeekMs = positionMs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mp.seekTo(positionMs.toLong(), MediaPlayer.SEEK_CLOSEST)
		} else {
			mp.seekTo(positionMs)
		}
		setProgressImmediate(positionMs)
	}

	private fun currentDisplayPositionMs(): Int {
		val mp = player ?: return 0
		return if (pendingSeekMs >= 0) pendingSeekMs else mp.currentPosition
	}

	private fun tickProgress() {
		val mp = player ?: return
		if (!prepared || userSeeking) return
		// Hold the requested position until the seek completes.
		if (pendingSeekMs >= 0) return
		animateProgressTo(mp.currentPosition)
	}

	private fun animateProgressTo(targetMs: Int) {
		progressAnimator?.cancel()
		val current = seekBar.progress
		if (targetMs <= current || targetMs - current > PROGRESS_TICK_MS * 3) {
			seekBar.progress = targetMs
		} else {
			progressAnimator = ObjectAnimator.ofInt(seekBar, "progress", current, targetMs).apply {
				duration = PROGRESS_TICK_MS
				interpolator = LinearInterpolator()
				start()
			}
		}
		positionTime.text = formatTime(targetMs)
	}

	private fun setProgressImmediate(positionMs: Int) {
		progressAnimator?.cancel()
		seekBar.progress = positionMs
		positionTime.text = formatTime(positionMs)
	}

	private fun updateProgressTexts(positionMs: Int, durationMs: Int) {
		positionTime.text = formatTime(positionMs)
		durationTime.text = formatTime(durationMs)
	}

	private fun formatTime(ms: Int): String =
		DateUtils.formatElapsedTime((ms / 1000).toLong())

	private fun setUiVisible(visible: Boolean) {
		if (controlsVisible != visible) {
			controlsVisible = visible
			animateControl(playPauseButton, visible)
			animateControl(playbackBar, visible)
		}
		(parentFragment as? GalleryPhotoPagerFragment)?.let {
			if (it.isUiHidden() == visible) {
				it.toggleUi()
			}
		}
	}

	private fun animateControl(view: View, visible: Boolean) {
		view.animate().cancel()
		if (visible) {
			view.visibility = View.VISIBLE
			view.animate()
				.alpha(1f)
				.scaleX(1f)
				.scaleY(1f)
				.setDuration(UI_FADE_MS)
				.setInterpolator(uiInterpolator)
				.withEndAction(null)
				.start()
		} else {
			view.animate()
				.alpha(0f)
				.scaleX(UI_HIDE_SCALE)
				.scaleY(UI_HIDE_SCALE)
				.setDuration(UI_FADE_MS)
				.setInterpolator(uiInterpolator)
				.withEndAction { view.visibility = View.INVISIBLE }
				.start()
		}
	}

	private fun scheduleAutoHide() {
		handler.removeCallbacks(hideUiRunnable)
		handler.postDelayed(hideUiRunnable, AUTO_HIDE_DELAY_MS)
	}

	private fun rescheduleAutoHideIfPlaying() {
		handler.removeCallbacks(hideUiRunnable)
		if (player?.isPlaying == true && controlsVisible) {
			scheduleAutoHide()
		}
	}

	private fun updatePlayButtonIcon(playing: Boolean) {
		playPauseButton.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_dark)
		playPauseButton.contentDescription = getString(
			if (playing) R.string.shared_string_pause else R.string.shared_string_control_start
		)
	}

	private fun animatePlayButtonPop() {
		playPauseButton.scaleX = PLAY_POP_SCALE
		playPauseButton.scaleY = PLAY_POP_SCALE
		playPauseButton.animate()
			.scaleX(1f)
			.scaleY(1f)
			.setDuration(PLAY_POP_MS)
			.setInterpolator(OvershootInterpolator())
			.start()
	}

	private fun showSeekIndicator(forward: Boolean, totalMs: Int) {
		val indicator = if (forward) seekForwardIndicator else seekBackIndicator
		val hideRunnable = if (forward) hideForwardIndicatorRunnable else hideBackIndicatorRunnable
		indicator.text = getString(
			if (forward) R.string.gallery_seek_forward_seconds else R.string.gallery_seek_back_seconds,
			totalMs / 1000
		)
		handler.removeCallbacks(hideRunnable)
		indicator.animate().cancel()
		if (indicator.visibility != View.VISIBLE || indicator.alpha < 0.99f) {
			val slide = resources.displayMetrics.density * SEEK_INDICATOR_SLIDE_DP
			indicator.visibility = View.VISIBLE
			indicator.translationX = if (forward) -slide else slide
			indicator.scaleX = 1f
			indicator.scaleY = 1f
			indicator.animate()
				.alpha(1f)
				.translationX(0f)
				.setDuration(SEEK_INDICATOR_IN_MS)
				.setInterpolator(uiInterpolator)
				.start()
		} else {
			indicator.scaleX = SEEK_INDICATOR_PULSE_SCALE
			indicator.scaleY = SEEK_INDICATOR_PULSE_SCALE
			indicator.animate()
				.scaleX(1f)
				.scaleY(1f)
				.setDuration(SEEK_INDICATOR_PULSE_MS)
				.setInterpolator(OvershootInterpolator())
				.start()
		}
		handler.postDelayed(hideRunnable, SEEK_INDICATOR_HOLD_MS)
	}

	private fun fadeOutIndicator(indicator: View) {
		indicator.animate().cancel()
		indicator.animate()
			.alpha(0f)
			.setDuration(SEEK_INDICATOR_OUT_MS)
			.withEndAction { indicator.visibility = View.GONE }
			.start()
	}

	private fun hideIndicatorNow(indicator: View) {
		indicator.animate().cancel()
		indicator.alpha = 0f
		indicator.visibility = View.GONE
	}

	private fun hidePoster() {
		if (posterView.visibility != View.VISIBLE) return
		posterView.animate()
			.alpha(0f)
			.setDuration(UI_FADE_MS)
			.withEndAction { posterView.visibility = View.GONE }
			.start()
	}

	private fun adjustVideoSize(videoWidth: Int, videoHeight: Int) {
		if (!isVideo || videoWidth <= 0 || videoHeight <= 0 || view == null) return
		rootView.post {
			val containerWidth = rootView.width
			val containerHeight = rootView.height
			if (containerWidth == 0 || containerHeight == 0) return@post
			val scale = min(
				containerWidth.toFloat() / videoWidth,
				containerHeight.toFloat() / videoHeight
			)
			val params = FrameLayout.LayoutParams(
				(videoWidth * scale).toInt(),
				(videoHeight * scale).toInt(),
				Gravity.CENTER
			)
			textureView.layoutParams = params
		}
	}

	private fun applyZoom(scale: Float) {
		zoomScale = scale
		textureView.scaleX = scale
		textureView.scaleY = scale
		clampVideoPan()
	}

	private fun panVideoBy(distanceX: Float, distanceY: Float) {
		textureView.translationX -= distanceX
		textureView.translationY -= distanceY
		clampVideoPan()
	}

	private fun clampVideoPan() {
		val maxX = textureView.width * (zoomScale - 1f) / 2f
		val maxY = textureView.height * (zoomScale - 1f) / 2f
		textureView.translationX = textureView.translationX.coerceIn(-abs(maxX), abs(maxX))
		textureView.translationY = textureView.translationY.coerceIn(-abs(maxY), abs(maxY))
	}

	private fun resetZoom(animated: Boolean) {
		zoomScale = 1f
		if (animated) {
			textureView.animate()
				.scaleX(1f).scaleY(1f)
				.translationX(0f).translationY(0f)
				.setDuration(UI_FADE_MS)
				.setInterpolator(uiInterpolator)
				.start()
		} else {
			textureView.scaleX = 1f
			textureView.scaleY = 1f
			textureView.translationX = 0f
			textureView.translationY = 0f
		}
	}

	override fun onPause() {
		super.onPause()
		pausePlayback()
		resetZoom(animated = false)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		abandonAudioFocus()
		handler.removeCallbacksAndMessages(null)
		progressAnimator?.cancel()
		progressAnimator = null
		pendingSeekMs = -1
		releasePlayer()
		surface?.release()
		surface = null
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putInt(SELECTED_POSITION_KEY, position)
		super.onSaveInstanceState(outState)
	}

	private fun whiteColor(): Int =
		ContextCompat.getColor(app, R.color.active_buttons_and_links_text_light)

	companion object {
		const val TAG = "GalleryMediaPlayerFragment"
		private const val SELECTED_POSITION_KEY = "selected_position_key"

		private const val SEEK_STEP_MS = 10_000
		private const val SEEK_BURST_WINDOW_MS = 700L
		private const val SEEK_COMPLETE_TOLERANCE_MS = 600
		private const val AUTO_HIDE_DELAY_MS = 5_000L
		private const val PROGRESS_TICK_MS = 500L
		private const val UI_FADE_MS = 150L
		private const val UI_HIDE_SCALE = 0.9f
		private const val PLAY_POP_SCALE = 0.8f
		private const val PLAY_POP_MS = 220L
		private const val SEEK_INDICATOR_SLIDE_DP = 8f
		private const val SEEK_INDICATOR_IN_MS = 90L
		private const val SEEK_INDICATOR_HOLD_MS = 700L
		private const val SEEK_INDICATOR_OUT_MS = 180L
		private const val SEEK_INDICATOR_PULSE_SCALE = 1.15f
		private const val SEEK_INDICATOR_PULSE_MS = 160L

		private const val MIN_ZOOM = 1f
		private const val MAX_ZOOM = 4f
		private const val SNAP_BACK_ZOOM = 1.05f

		private val uiInterpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
		private val LOG = PlatformUtil.getLog(GalleryMediaPlayerFragment::class.java)

		@JvmStatic
		fun newInstance(position: Int): GalleryMediaPlayerFragment =
			GalleryMediaPlayerFragment().apply {
				arguments = Bundle().apply { putInt(SELECTED_POSITION_KEY, position) }
			}
	}
}
