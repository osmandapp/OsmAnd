package net.osmand.plus.plugins.srtm

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteCard
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.chooseplan.OsmAndFeature
import net.osmand.plus.configmap.MultiStateColorPaletteController
import net.osmand.plus.configmap.MultiStateColorPaletteFragment
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities

class Buildings3DColorFragment : MultiStateColorPaletteFragment() {

	companion object {

		@JvmStatic
		fun showInstance(manager: FragmentManager): Boolean {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, Buildings3DColorFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
				return true
			}
			return false
		}
	}

	override fun setupMainContent(container: ViewGroup) {
		super.setupMainContent(container)
		container.minimumHeight = dpToPx(240f)
	}

	override fun buildCustomStateContent(activity: FragmentActivity, container: ViewGroup) {
		val controller = screenController as Buildings3DColorScreenController

		if (controller.colorType == Buildings3DColorType.MAP_STYLE) {
			// State 1: Map Style
			val view = inflate(R.layout.card_3d_buildings_color_map_style, container)
			val tvSummary = view.findViewById<TextView>(R.id.custom_color_description)
			val pattern = getString(R.string.buildings_3d_use_map_style_color_description)
			val rendererName = app.rendererRegistry.selectedRendererName
			tvSummary.text = String.format(pattern, rendererName)

		} else {
			// State 2: Custom Color
			if (InAppPurchaseUtils.isBuildingsCustomColorAvailable(app)) {
				val paletteController = controller.colorsPaletteController
				val paletteCard = ModedColorsPaletteCard(activity, paletteController)
				container.addView(paletteCard.build(activity))
			} else {
				val promoView = inflate(R.layout.card_3d_buildings_color_promo, container)
				val button = promoView.findViewById<View>(R.id.get_btn_container)
				button.setOnClickListener {
					ChoosePlanFragment.showInstance(requireActivity(), OsmAndFeature.TERRAIN)
				}
				UiUtilities.setupListItemBackground(activity, button, getAppModeColor(nightMode))
			}
		}
	}

	override fun getScreenController(): MultiStateColorPaletteController {
		return Buildings3DColorScreenController.getExistedInstance(app) ?:
		throw IllegalStateException("Buildings3DColorScreenController is missing from DialogManager")
	}
}
