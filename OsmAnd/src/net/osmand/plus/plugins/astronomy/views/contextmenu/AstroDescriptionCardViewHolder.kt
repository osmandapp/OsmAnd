package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.ColorUtilities

class AstroDescriptionCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	private val descriptionTv: TextView = itemView.findViewById(R.id.description)
	private val readButton: MaterialButton = itemView.findViewById(R.id.read_article_button)

	fun bind(
		app: OsmandApplication,
		nightMode: Boolean,
		item: AstroDescriptionCardItem,
		onReadClick: (Uri) -> Unit
	) {
		val description = item.description
		descriptionTv.text = description
		descriptionTv.isVisible = description.isNotBlank()

		val readMoreUri = item.readMoreUri
		val linkType = item.linkType
		readButton.isVisible = readMoreUri != null && linkType != null
		if (readMoreUri == null || linkType == null) {
			readButton.setOnClickListener(null)
			return
		}

		val targetName = when (linkType) {
			AstroDescriptionLinkType.WIKIPEDIA -> app.getString(R.string.shared_string_wikipedia)
			AstroDescriptionLinkType.WIKIDATA -> app.getString(R.string.wikidata)
		}
		val text = app.getString(R.string.read_on, targetName)
		val start = text.indexOf(targetName)
		val end = start + targetName.length
		val sp = SpannableString(text).apply {
			if (start >= 0) {
				setSpan(
					ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)),
					start,
					end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
		}

		readButton.text = sp
		readButton.setTypeface(readButton.typeface, android.graphics.Typeface.NORMAL)
		readButton.icon = app.uiUtilities.getPaintedIcon(
			when (linkType) {
				AstroDescriptionLinkType.WIKIPEDIA -> R.drawable.ic_plugin_wikipedia
				AstroDescriptionLinkType.WIKIDATA -> R.drawable.ic_action_logo_wikidata
			},
			ColorUtilities.getDefaultIconColor(app, nightMode)
		)

		readButton.setOnClickListener {
			onReadClick(readMoreUri)
		}
	}
}
