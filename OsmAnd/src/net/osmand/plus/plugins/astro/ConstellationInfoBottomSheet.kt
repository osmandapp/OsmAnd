package net.osmand.plus.plugins.astro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.osmand.plus.R

class ConstellationInfoBottomSheet : BottomSheetDialogFragment() {

	private lateinit var sheetTitle: TextView
	private lateinit var sheetWikiButton: View

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.bottom_sheet_constellation, container, false)
		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetWikiButton = view.findViewById(R.id.sheet_wiki_button)
		return view
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.setDimAmount(0f)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		arguments?.getString("name")?.let { sheetTitle.text = it }
		val wid = arguments?.getString("wid")
		if (!wid.isNullOrEmpty()) {
			sheetWikiButton.isVisible = true
			sheetWikiButton.setOnClickListener {
				val uri = Uri.parse("https://www.wikidata.org/wiki/$wid")
				val intent = Intent(Intent.ACTION_VIEW, uri)
				try {
					startActivity(intent)
				} catch (_: Exception) {
				}
			}
		} else {
			sheetWikiButton.isVisible = false
		}
	}

	companion object {
		const val TAG = "ConstellationInfoBottomSheet"
		fun newInstance(constellation: Constellation): ConstellationInfoBottomSheet {
			val fragment = ConstellationInfoBottomSheet()
			val args = Bundle()
			args.putString("name", constellation.name)
			args.putString("wid", constellation.wid)
			fragment.arguments = args
			return fragment
		}
	}
}