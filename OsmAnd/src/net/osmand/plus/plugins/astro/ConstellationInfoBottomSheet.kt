package net.osmand.plus.plugins.astro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils

class ConstellationInfoFragment : Fragment() {

	private lateinit var sheetTitle: TextView
	private lateinit var sheetWikiButton: View

	private val parent: StarMapFragment
		get() = requireParentFragment() as StarMapFragment

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.bottom_sheet_constellation, container, false)
		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetWikiButton = view.findViewById(R.id.sheet_wiki_button)

		view.findViewById<View>(R.id.close_button).setOnClickListener {
			parent.hideBottomSheet()
		}

		ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			val basePadding = AndroidUtils.dpToPx(v.context, 16f)
			v.updatePadding(bottom = insets.bottom + basePadding)
			windowInsets
		}

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		arguments?.getString("displayName")?.let { sheetTitle.text = it }
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
		const val TAG = "ConstellationInfoFragment"
		fun newInstance(constellation: Constellation): ConstellationInfoFragment {
			val fragment = ConstellationInfoFragment()
			val args = Bundle()
			args.putString("displayName", constellation.localizedName ?: constellation.name)
			args.putString("wid", constellation.wid)
			fragment.arguments = args
			return fragment
		}
	}
}