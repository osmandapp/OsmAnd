package net.osmand.plus.plugins.astro.contextmenu

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard
import net.osmand.plus.utils.ColorUtilities

class ContextMenuCardsAdapter(
	var app: OsmandApplication,
	var mapActivity: MapActivity,
	var nightMode: Boolean,
	var astroContextMenuFragment: AstroContextMenuFragment,
	var layoutInflater: LayoutInflater
) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	var cards: MutableList<AbstractCard?> = ArrayList()

	val items = mutableListOf<AstroContextCard>()

	companion object {
		private const val VIEW_TYPE_DESCRIPTION = 0
	}

	fun setItems(newItems: List<AstroContextCard>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}

	override fun getItemViewType(position: Int): Int {
		val item = items[position]

		return when (item) {
			is AstroDescriptionCardModel -> {
				VIEW_TYPE_DESCRIPTION
			}

			else -> {
				-1
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			VIEW_TYPE_DESCRIPTION -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_context_description_card, parent, false)
				DescriptionCardViewHolder(view)
			}

			else -> throw IllegalArgumentException("Unknown view type: $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		when (holder) {
			is DescriptionCardViewHolder -> holder.bind(
				app,
				nightMode,
				item as AstroDescriptionCardModel,
				astroContextMenuFragment
			)
		}
	}

	override fun getItemCount(): Int = items.size

	inline fun <reified T> getItemPosition(): Int =
		items.indexOfFirst { it is T }

	class DescriptionCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
			val text =
				app.getString(R.string.read_on, wikipediaString)

			val start = text.indexOf(wikipediaString)
			val end = start + wikipediaString.length

			val sp = SpannableString(text).apply {
				setSpan(
					ForegroundColorSpan(
						ColorUtilities.getActiveColor(
							app,
							nightMode
						)
					),
					start, end,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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
}