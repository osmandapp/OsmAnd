package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton

class NoInternetHolder(
	itemView: View,
	private val app: OsmandApplication,
	private val onReloadMediaItems: () -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val imageView: ImageView = itemView.findViewById(R.id.icon)
	private val tryAgainButton: DialogButton = itemView.findViewById(R.id.try_again_button)
	private val progressBar: ProgressBar = itemView.findViewById(R.id.progress)

	init {
		tryAgainButton.setOnClickListener { onReloadMediaItems() }
	}

	fun bindView(nightMode: Boolean, loadingImages: Boolean) {
		imageView.setImageDrawable(
			app.uiUtilities.getPaintedIcon(
				R.drawable.ic_action_wifi_off,
				ColorUtilities.getDefaultIconColor(app, nightMode)
			)
		)
		updateProgressBar(loadingImages)
	}

	fun updateProgressBar(loadingImages: Boolean) {
		tryAgainButton.findViewById<View>(R.id.button_text).visibility =
			if (loadingImages) View.INVISIBLE else View.VISIBLE
		AndroidUiHelper.updateVisibility(progressBar, loadingImages)
	}
}