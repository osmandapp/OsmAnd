package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
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
		descriptionCardModel: AstroDescriptionCardModel,
		astroContextMenuFragment: AstroContextMenuFragment
	) {
		descriptionCardModel.astroArticle?.apply { descriptionTv.text = description }

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
			val uri = descriptionCardModel.getWikiUri()
			val intent = Intent(Intent.ACTION_VIEW, uri)
			try {
				astroContextMenuFragment.startActivity(intent)
			} catch (_: Exception) {
			}
		}
	}
}