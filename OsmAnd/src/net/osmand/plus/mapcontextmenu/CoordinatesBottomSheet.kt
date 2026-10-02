package net.osmand.plus.mapcontextmenu

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.data.PointDescription
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialBottomSheetWithHeader
import net.osmand.plus.mapcontextmenu.other.ShareMenu
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow

class CoordinatesBottomSheet : BaseMaterialBottomSheetWithHeader() {

    override fun getThemedInflater(): LayoutInflater = layoutInflater

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        mainView.findViewById<TextView>(R.id.title).setText(R.string.coordinates)
        val mapActivity = requireMapActivity()
        val latitude = requireArguments().getDouble(ARG_LATITUDE)
        val longitude = requireArguments().getDouble(ARG_LONGITUDE)
        val preferred = PointDescription.getPreferredLocationData(mapActivity, latitude, longitude)
        val coordinates = preferred.take(1) +
                PointDescription.getCollapsedLocationData(mapActivity, latitude, longitude, preferred)
        val itemsContainer = mainView.findViewById<LinearLayout>(R.id.itemsContainer)
        coordinates.forEach { coordinate ->
            val row = SettingRow(inflate(R.layout.item_ui_setting_row, itemsContainer, false))
            val format = coordinate.format
            val title = format.epsgCode?.let { "${format.title} (EPSG:$it)" } ?: format.title
            row.setIcon(null)
            row.setTitle(title.ifEmpty { coordinate.text })
            row.setSubtitle(if (title.isEmpty()) null else coordinate.text)
            row.setOnClickListener {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    ShareMenu.copyToClipboardWithToast(mapActivity, coordinate.text, false)
                } else {
                    ShareMenu.copyToClipboard(mapActivity, coordinate.text)
                }
                dismiss()
            }
            itemsContainer.addView(row.view)
        }
        SegmentedList.apply(itemsContainer)
        return mainView
    }

    override fun isUsedOnMap(): Boolean = true

    override fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_EXPANDED

    override fun shouldSkipCollapsed(): Boolean = true

    companion object {
        val TAG: String = CoordinatesBottomSheet::class.java.simpleName
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"

        @JvmStatic
        fun showInstance(manager: FragmentManager, latitude: Double, longitude: Double) {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                CoordinatesBottomSheet().apply {
                    arguments = Bundle().apply {
                        putDouble(ARG_LATITUDE, latitude)
                        putDouble(ARG_LONGITUDE, longitude)
                    }
                }.show(manager, TAG)
            }
        }
    }
}
