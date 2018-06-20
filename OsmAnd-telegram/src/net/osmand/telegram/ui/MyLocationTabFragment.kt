package net.osmand.telegram.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.osmand.telegram.R

class MyLocationTabFragment : Fragment() {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val mainView = inflater.inflate(R.layout.fragment_my_location_tab, container, false)
		return mainView
	}
}
