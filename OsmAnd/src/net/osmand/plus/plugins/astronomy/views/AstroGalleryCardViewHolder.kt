package net.osmand.plus.plugins.astronomy.views

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridFragment
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroGalleryCardModel
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.util.Algorithms

class AstroGalleryCardViewHolder(
	itemView: View,
	private val app: OsmandApplication,
	private val mapActivity: MapActivity,
	private val getShowAllTitle: () -> String?,
	private val listener: GalleryGridAdapter.ImageCardListener,
	private val onToggle: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val recyclerView: RecyclerView = itemView.findViewById(R.id.gallery_grid_recycler_view)
	private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progress_bar)
	private val contentContainer: LinearLayout = itemView.findViewById(R.id.content_container)
	private val viewAllButton: DialogButton = itemView.findViewById(R.id.view_all)
	private val collapseButton: View = itemView.findViewById(R.id.collapse_button)
	private val arrowCard: ImageView = itemView.findViewById(R.id.arrow_icon)
	private var galleryGridAdapter: GalleryGridAdapter? = null
	private var adapterNightMode: Boolean? = null

	init {
		setupViewAllButton()
	}

	fun bind(model: AstroGalleryCardModel, nightMode: Boolean) {
		ensureRecyclerInitialized(nightMode)
		progressBar.visibility = View.GONE
		applyViewAllStyle(nightMode)

		collapseButton.setOnClickListener {
			onToggle(model.wid)
		}

		when (val state = model.state) {
			is AstroGalleryCardModel.GalleryState.Collapsed -> showCollapsed()
			is AstroGalleryCardModel.GalleryState.Loading -> showLoading()
			is AstroGalleryCardModel.GalleryState.Ready -> showCards(state.cards)
			is AstroGalleryCardModel.GalleryState.Error -> showError(state.message)
		}

		arrowCard.setImageDrawable(
			app.uiUtilities.getIcon(
				if (model.state is AstroGalleryCardModel.GalleryState.Collapsed) {
					R.drawable.ic_action_arrow_down
				} else {
					R.drawable.ic_action_arrow_up
				},
				ColorUtilities.getDefaultIconColorId(nightMode)
			)
		)
	}

	fun bindGallery(model: AstroGalleryCardModel, nightMode: Boolean) {
		ensureRecyclerInitialized(nightMode)
		applyViewAllStyle(nightMode)

		when (model.state) {
			is AstroGalleryCardModel.GalleryState.Collapsed -> {
				contentContainer.visibility = View.GONE
				progressBar.visibility = View.GONE
			}

			is AstroGalleryCardModel.GalleryState.Loading -> {
				progressBar.visibility = View.VISIBLE
				contentContainer.visibility = View.GONE
			}

			is AstroGalleryCardModel.GalleryState.Ready -> {
				showCards(ArrayList<Any>((model.state as AstroGalleryCardModel.GalleryState.Ready).cards.filterNotNull()))
				progressBar.visibility = View.GONE
			}

			else -> {
				progressBar.visibility = View.GONE
				contentContainer.visibility = View.VISIBLE
			}
		}

		arrowCard.setImageDrawable(
			app.uiUtilities.getIcon(
				if (model.state is AstroGalleryCardModel.GalleryState.Collapsed) {
					R.drawable.ic_action_arrow_down
				} else {
					R.drawable.ic_action_arrow_up
				},
				ColorUtilities.getDefaultIconColorId(nightMode)
			)
		)
	}

	private fun showError(message: String) {
		progressBar.visibility = View.GONE
		contentContainer.visibility = View.VISIBLE
	}

	private fun showLoading() {
		progressBar.visibility = View.VISIBLE
		contentContainer.visibility = View.GONE
	}

	private fun showCards(cards: List<Any?>) {
		contentContainer.visibility = View.VISIBLE
		val items: MutableList<Any?> = ArrayList()

		val containsImage = cards.any { it is ImageCard }
		val connectionAvailable = app.getSettings().isInternetConnectionAvailable

		if (connectionAvailable || !Algorithms.isEmpty(cards)) {
			items.addAll(cards)
			viewAllButton.visibility = if (containsImage) View.VISIBLE else View.GONE
		} else {
			items.add(GalleryGridAdapter.NO_INTERNET_TYPE)
			viewAllButton.visibility = View.GONE
		}
		galleryGridAdapter?.setItems(items)
		recyclerView.post {
			recyclerView.invalidateItemDecorations()
			recyclerView.requestLayout()
		}
	}

	private fun showCollapsed() {
		contentContainer.visibility = View.GONE
	}

	private fun getGridLayoutManager(): GridLayoutManager {
		val gridLayoutManager = GridLayoutManager(app, 2, GridLayoutManager.HORIZONTAL, false)
		gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
			override fun getSpanSize(position: Int): Int {
				val adapter = galleryGridAdapter
				return if (
					adapter != null &&
					position in 0 until adapter.itemCount &&
					adapter.getItemViewType(position) == GalleryGridAdapter.IMAGE_TYPE
				) {
					1
				} else {
					2
				}
			}
		}
		return gridLayoutManager
	}

	private fun ensureRecyclerInitialized(nightMode: Boolean) {
		if (galleryGridAdapter != null && adapterNightMode == nightMode) {
			return
		}
		galleryGridAdapter = GalleryGridAdapter(mapActivity, listener, null, true, nightMode)
		adapterNightMode = nightMode
		recyclerView.layoutManager = getGridLayoutManager()
		if (recyclerView.itemDecorationCount == 0) {
			recyclerView.addItemDecoration(GalleryGridItemDecorator(app))
		}
		recyclerView.adapter = galleryGridAdapter
		recyclerView.itemAnimator = galleryGridAdapter?.animator
	}

	private fun setupViewAllButton() {
		viewAllButton.setTitleId(R.string.shared_string_show_all)
		viewAllButton.setOnClickListener {
			GalleryGridFragment.showInstance(mapActivity, getShowAllTitle())
		}
	}

	private fun applyViewAllStyle(nightMode: Boolean) {
		viewAllButton.buttonView.setBackgroundResource(R.drawable.bg_catalog_chip)
		val textView = viewAllButton.findViewById<TextView>(R.id.button_text)
		textView?.setTextColor(ColorUtilities.getActiveColor(app, nightMode))
	}
}
