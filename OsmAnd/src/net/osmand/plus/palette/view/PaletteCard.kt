package net.osmand.plus.palette.view

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.main.ColorsPaletteElements
import net.osmand.plus.card.color.palette.v2.PaletteCardAdapter
import net.osmand.plus.palette.contract.IPaletteInteractionListener
import net.osmand.plus.palette.contract.IPaletteView
import net.osmand.plus.palette.controller.BasePaletteController
import net.osmand.plus.palette.view.binder.PaletteItemViewBinder
import net.osmand.plus.routepreparationmenu.cards.BaseCard
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.tools.HorizontalSpaceItemDecoration
import net.osmand.shared.palette.domain.PaletteItem

abstract class PaletteCard(
	activity: FragmentActivity,
	protected val controller: BasePaletteController,
	appMode: ApplicationMode? = null,
	usedOnMap: Boolean = true
) : BaseCard(activity, appMode, usedOnMap), IPaletteView {

	private val paletteElements = ColorsPaletteElements(activity, nightMode) // TODO: PaletteElements

	protected val adapter: PaletteCardAdapter = PaletteCardAdapter(paletteListener, controller, createViewBinder())
	protected var recyclerView: RecyclerView? = null

	private val paletteListener: IPaletteInteractionListener
		get() = controller as? IPaletteInteractionListener
			?: throw IllegalStateException("Controller must implement ${IPaletteInteractionListener::class.simpleName}")

	init {
		controller.attachView(this)
	}

	@LayoutRes
	override fun getCardLayoutId() = R.layout.card_colors_palette

	override fun updateContent() {
		setupRecyclerView()
		setupAddButton()
		setupShowAllButton()
		askScrollToTargetColorPosition(controller.getSelectedPaletteItem(), smoothScroll = false)
	}

	protected open fun setupRecyclerView() {
		val rv = view.findViewById<RecyclerView>(R.id.colors_list) ?: return
		this.recyclerView = rv

		rv.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
		rv.addItemDecoration(HorizontalSpaceItemDecoration(getDimen(R.dimen.content_padding_small_half)))
		rv.clipToPadding = false
		rv.adapter = adapter
	}

	protected fun setupAddButton() {
		val container = view.findViewById<ViewGroup>(R.id.add_button_container) ?: return
		container.addView(paletteElements.createAddButtonView(container))
		container.setOnClickListener {
			paletteListener.onAddButtonClick(activity)
		}
	}

	protected open fun setupShowAllButton() {
		val buttonAllColors = view.findViewById<View>(R.id.button_all_colors) ?: return

		buttonAllColors.setOnClickListener {
			paletteListener.onShowAllClick(activity)
		}

		// Оновлюємо фон кнопки "Всі"
		updateShowAllButton()
	}

	private fun updateShowAllButton() {
		val btnAll = view.findViewById<View>(R.id.button_all_colors) ?: return
		val accentColor = controller.getControlsAccentColor(nightMode)
		UiUtilities.setupListItemBackground(activity, btnAll, accentColor)
	}

	// --- IPaletteView Implementation ---

	override fun updatePaletteItems(targetItem: PaletteItem?) {
		adapter.updateItemsList()
		if (targetItem != null) {
			askScrollToTargetColorPosition(targetItem, smoothScroll = true)
		}
		updateShowAllButton()
	}

	override fun updatePaletteSelection(oldItem: PaletteItem?, newItem: PaletteItem) {
		adapter.askNotifyItemChanged(oldItem)
		adapter.askNotifyItemChanged(newItem)
		askScrollToTargetColorPosition(newItem, smoothScroll = true)

		// TODO: we can force it as a state
		if (controller.isAccentColorCanBeChanged()) {
			updateShowAllButton()
		}
	}

	override fun getActivity(): FragmentActivity? = super.activity

	// --- Helpers ---

	private fun askScrollToTargetColorPosition(targetItem: PaletteItem?, smoothScroll: Boolean) {
		if (targetItem == null || recyclerView == null) return

		val targetPosition = adapter.indexOf(targetItem)
		val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return

		val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
		val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()

		if (targetPosition >= 0 && (targetPosition < firstVisible || targetPosition > lastVisible)) {
			if (smoothScroll) {
				recyclerView?.smoothScrollToPosition(targetPosition)
			} else {
				recyclerView?.scrollToPosition(targetPosition)
			}
		}
	}

	abstract fun createViewBinder(): PaletteItemViewBinder

	abstract fun getShowAllButtonTitle(): String
}