package net.osmand.plus.plugins.aistracker.fragments

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonCard

/**
 * Common parts of the Material 3 screens of the Vessel tracker (AIS) plugin: the app bar, the
 * plugin instance and the navigation between the screens of the plugin.
 */
abstract class AisBaseFragment : BaseMaterialFragment() {

	protected val plugin: AisTrackerPlugin by lazy {
		PluginsHelper.requirePlugin(AisTrackerPlugin::class.java)
	}

	override fun getMaterialThemeRes(nightMode: Boolean): Int =
		if (nightMode) R.style.OsmandMaterialExpressiveDarkTheme
		else R.style.OsmandMaterialExpressiveLightTheme

	/**
	 * @param resetActionId title of the trailing app bar action, used for its content description.
	 */
	protected fun setupToolbar(
		view: View,
		@StringRes titleId: Int,
		@StringRes resetActionId: Int,
		@DrawableRes resetIconId: Int = R.drawable.ic_action_reset,
		onReset: () -> Unit
	) {
		val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
		toolbar.setTitle(titleId)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
		toolbar.menu.clear()
		toolbar.menu.add(resetActionId).apply {
			val icon = AppCompatResources.getDrawable(view.context, resetIconId)?.mutate()
			icon?.setTint(themedColor(view, R.attr.colorOnSurfaceVariant))
			setIcon(icon)
			setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
			setOnMenuItemClickListener {
				onReset()
				true
			}
		}
	}

	protected fun showFragment(fragment: Fragment) {
		val manager = requireActivity().supportFragmentManager
		val tag = fragment.javaClass.name
		if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
			fragment.arguments = arguments
			manager.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, tag)
				.addToBackStack(DRAWER_SETTINGS_ID)
				.commitAllowingStateLoss()
		}
	}

	/**
	 * Colours must be resolved against the Material 3 context of the screen - the activity theme
	 * does not carry the Material 3 attributes and would silently return the wrong colour.
	 */
	@ColorInt
	protected fun themedColor(view: View, @AttrRes attrId: Int): Int =
		AndroidUtils.getColorFromAttr(view.context, attrId)

	protected fun dp(value: Int): Int = AndroidUtils.dpToPx(osmandApp, value.toFloat())

	/**
	 * The artwork carries no background of its own - the water behind it comes from the selected
	 * map style, so the illustration matches the map the user is looking at.
	 */
	protected fun setupHeroImage(view: View, @DrawableRes dayImageId: Int, @DrawableRes nightImageId: Int) {
		val container: View = view.findViewById(R.id.image_card)
		val image: ImageView = view.findViewById(R.id.image)
		image.setImageResource(if (nightMode) nightImageId else dayImageId)

		val waterColor = MapButtonCard.getMapWaterColor(osmandApp, nightMode)
		if (waterColor != null) {
			val background = ContextCompat.getDrawable(container.context, R.drawable.bg_ui_card_16)
			if (background is GradientDrawable) {
				background.mutate()
				background.setColor(waterColor)
				container.background = background
			}
		}
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		/* the app bar has to sit below the status bar, and the content has to scroll under the
		 * navigation bar instead of ending above it */
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}
}
