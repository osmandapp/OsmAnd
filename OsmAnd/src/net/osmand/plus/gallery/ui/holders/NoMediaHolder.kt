package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.contract.IGalleryActionListener
import net.osmand.plus.gallery.model.GalleryItem.NoMedia
import net.osmand.plus.gallery.model.GalleryItem.NoMedia.ActionButtonStyle
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton

class NoMediaHolder(
	private val app: OsmandApplication,
	itemView: View
) : RecyclerView.ViewHolder(itemView) {

	private val imageView: ImageView = itemView.findViewById(R.id.icon)
	private val titleView: TextView = itemView.findViewById(R.id.title)
	private val descriptionView: TextView = itemView.findViewById(R.id.description)
	private val simpleButton: View = itemView.findViewById(R.id.no_media_action_button)
	private val dialogButton: DialogButton = itemView.findViewById(R.id.no_media_action_dialog_button)

	fun bindView(
		item: NoMedia,
		nightMode: Boolean,
		listener: IGalleryActionListener?
	) {
		imageView.setImageDrawable(
			app.uiUtilities.getPaintedIcon(
				item.iconResId,
				ColorUtilities.getDefaultIconColor(app, nightMode)
			)
		)
		titleView.setText(item.titleResId)
		descriptionView.setText(item.descriptionResId)

		val action = item.action
		val useDialogButton = item.buttonStyle == ActionButtonStyle.DIALOG
		val activeButton: View = if (useDialogButton) dialogButton else simpleButton
		val inactiveButton: View = if (useDialogButton) simpleButton else dialogButton

		AndroidUiHelper.updateVisibility(inactiveButton, false)
		AndroidUiHelper.updateVisibility(activeButton, action != null)
		activeButton.setOnClickListener(
			if (action != null) View.OnClickListener { listener?.handleGalleryAction(it, action) }
			else null
		)
	}
}