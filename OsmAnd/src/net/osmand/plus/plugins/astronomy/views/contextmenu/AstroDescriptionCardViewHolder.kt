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

		val wikipediaString = app.getString(R.string.shared_string_wikipedia)
		val text = app.getString(R.string.read_on, wikipediaString)
		val start = text.indexOf(wikipediaString)
		val end = start + wikipediaString.length
		val sp = SpannableString(text).apply {
			setSpan(
				ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)),
				start,
				end,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}

		readButton.text = sp
		readButton.icon = app.uiUtilities.getPaintedIcon(
			R.drawable.ic_plugin_wikipedia,
			ColorUtilities.getDefaultIconColor(app, nightMode)
		)

		readButton.setOnClickListener {
			onReadClick(item.wikiUri)
		}
	}
}
