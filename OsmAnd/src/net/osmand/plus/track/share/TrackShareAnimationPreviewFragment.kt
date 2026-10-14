package net.osmand.plus.track.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.TextViewEx
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.plus.widgets.dialogbutton.DialogButtonType
import java.io.File

/** The video of a track made by "Share as animation", playing before it goes to the share sheet. */
class TrackShareAnimationPreviewFragment : BaseFullScreenDialogFragment() {

	private var file: File? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		file = arguments?.getString(FILE_KEY)?.let { File(it) }
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		val view = inflate(R.layout.fragment_track_share_animation_preview, container, false)
		setupToolbar(view.findViewById(R.id.appbar))
		val player = view.findViewById<VideoView>(R.id.video)
		file?.takeIf { it.isFile }?.let { file ->
			val controller = MediaController(view.context)
			controller.setAnchorView(player)
			player.setMediaController(controller)
			player.setVideoURI(Uri.fromFile(file))
			player.setOnPreparedListener { it.isLooping = true; player.start() }
		}
		setupButtons(view)
		return view
	}

	private fun setupToolbar(appbar: View) {
		ViewCompat.setElevation(appbar, 5.0f)
		appbar.findViewById<TextViewEx>(R.id.toolbar_title).setText(R.string.share_as_animation)
		val closeButton = appbar.findViewById<ImageView>(R.id.close_button)
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close))
		closeButton.setOnClickListener { dismiss() }
	}

	private fun setupButtons(view: View) {
		view.findViewById<View>(R.id.bottom_buttons_container)
			.setBackgroundColor(AndroidUtils.getColorFromAttr(view.context, R.attr.bg_color))
		val closeButton = view.findViewById<DialogButton>(R.id.dismiss_button)
		closeButton.setButtonType(DialogButtonType.SECONDARY)
		closeButton.setTitleId(R.string.shared_string_close)
		closeButton.setOnClickListener { dismiss() }
		val shareButton = view.findViewById<DialogButton>(R.id.right_bottom_button)
		shareButton.setButtonType(DialogButtonType.PRIMARY)
		shareButton.setTitleId(R.string.shared_string_share)
		shareButton.visibility = View.VISIBLE
		view.findViewById<View>(R.id.buttons_divider).visibility = View.VISIBLE
		shareButton.setOnClickListener { file?.let { share(it) } }
	}

	private fun share(file: File) {
		val activity = activity ?: return
		val intent = Intent(Intent.ACTION_SEND)
		intent.type = MIME_TYPE
		intent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, file))
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		AndroidUtils.startActivityIfSafe(activity, Intent.createChooser(intent, getString(R.string.shared_string_share)))
	}

	companion object {
		private val TAG: String = TrackShareAnimationPreviewFragment::class.java.simpleName
		private const val FILE_KEY = "file"
		private const val MIME_TYPE = "video/mp4"

		@JvmStatic
		fun showInstance(manager: FragmentManager, file: File) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = TrackShareAnimationPreviewFragment()
				fragment.arguments = Bundle().apply { putString(FILE_KEY, file.path) }
				fragment.show(manager, TAG)
			}
		}
	}
}
