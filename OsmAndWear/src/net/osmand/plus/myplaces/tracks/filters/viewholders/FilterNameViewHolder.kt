package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import studio.carbonylgroup.textfieldboxes.ExtendedEditText

class FilterNameViewHolder(itemView: View, nightMode: Boolean) : RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private var listener: TextChangedListener? = null
	private val title: ExtendedEditText
	private val textWatcher: TextWatcher = object : TextWatcher {
		override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
		override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
		override fun afterTextChanged(s: Editable) {
			if (listener != null) {
				listener!!.onTextChanged(s.toString())
			}
		}
	}

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.query_et)
	}

	fun bindView(query: String?, listener: TextChangedListener?) {
		title.removeTextChangedListener(textWatcher)
		title.setText(query)
		this.listener = listener
		title.addTextChangedListener(textWatcher)
	}

	interface TextChangedListener {
		fun onTextChanged(neeText: String)
	}
}