package net.osmand.telegram.ui.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

class EmptyStateRecyclerView : RecyclerView {

	private var emptyView: View? = null

	private val emptyStateObserver = object : RecyclerView.AdapterDataObserver() {
		override fun onChanged() {
			checkIfEmpty()
		}

		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			checkIfEmpty()
		}

		override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
			checkIfEmpty()
		}
	}

	constructor(context: Context) : super(context)

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

	constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
		context,
		attrs,
		defStyle
	)

	override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
		val oldAdapter = getAdapter()
		oldAdapter?.unregisterAdapterDataObserver(emptyStateObserver)
		super.setAdapter(adapter)
		adapter?.registerAdapterDataObserver(emptyStateObserver)
		checkIfEmpty()
	}

	fun setEmptyView(emptyView: View) {
		this.emptyView = emptyView
		checkIfEmpty()
	}

	private fun checkIfEmpty() {
		adapter?.apply {
			val empty = itemCount == 0
			visibility = if (empty) View.GONE else View.VISIBLE
			emptyView?.visibility = if (empty) View.VISIBLE else View.GONE
		}
	}
}