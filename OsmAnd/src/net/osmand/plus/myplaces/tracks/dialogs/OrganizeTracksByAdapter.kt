package net.osmand.plus.myplaces.tracks.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.containers.ScreenItem
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.gpx.enums.OrganizeByGroupType
import net.osmand.shared.gpx.enums.OrganizeByType

class OrganizeTracksByAdapter(
    val app: OsmandApplication,
    val appMode: ApplicationMode,
    val controller: OrganizeTracksByController
): RecyclerView.Adapter<ViewHolder>() {

    companion object {
        const val DIALOG_SUMMARY = 1
        const val GROUP_HEADER = 2
        const val SELECTABLE_ITEM = 3
        const val DIVIDER_FULL = 4
        const val DIVIDER_WITH_PADDING = 5
        const val SPACE = 6
    }

    private var parent: ViewGroup? = null
    private var context: Context? = null
    private var screenItems: List<ScreenItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.parent = parent
        context = parent.context
        return when (viewType) {
            DIALOG_SUMMARY -> SummaryViewHolder(inflate(R.layout.list_item_description_header))
            GROUP_HEADER -> HeaderViewHolder(inflate(R.layout.list_item_header_paragraph))
            SELECTABLE_ITEM -> SelectableItemViewHolder(inflate(R.layout.list_item_icon_and_radio_button))
            DIVIDER_FULL -> DividerViewHolder(inflate(R.layout.list_item_divider_basic))
            DIVIDER_WITH_PADDING -> DividerWithPaddingViewHolder(inflate(R.layout.list_item_divider_with_paragraph_padding))
            SPACE -> SpaceViewHolder(View(context), app.resources.getDimensionPixelSize(R.dimen.dialog_button_ex_height))
            else -> throw IllegalArgumentException("Unsupported view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val screenItem = screenItems[position]
        when (holder) {
            is SummaryViewHolder -> {
                holder.tvSummary!!.setText(R.string.organize_by_summary)
            }

            is HeaderViewHolder -> {
                val organizeByGroup = screenItem.value as OrganizeByGroupType
                holder.tvTitle!!.text = organizeByGroup.getName()
            }

            is SelectableItemViewHolder -> {
                // Use safe cast 'as?' because "None" item has null value
                val organizeByType = screenItem.value as? OrganizeByType
                val isSelected = controller.selectedType == organizeByType

                val nightMode = controller.isNightMode
                val activeColor = ColorUtilities.getActiveColor(app, nightMode)

                val title: String
                val iconId: Int
                if (organizeByType != null) {
                    title = organizeByType.getName()
                    iconId = AndroidUtils.getDrawableId(
                        app,
                        organizeByType.iconResId, R.drawable.ic_action_info_outlined)
                } else {
                    title = app.getString(R.string.shared_string_none)
                    iconId = R.drawable.ic_action_list_flat
                }
                holder.tvTitle?.text = title
                holder.ivIcon?.setImageDrawable(
                    if (isSelected)
                        app.uiUtilities.getActiveIcon(iconId, nightMode)
                    else
                        app.uiUtilities.getThemedIcon(iconId)
                )

                holder.rbRadio?.isChecked = isSelected
                UiUtilities.setupCompoundButton(nightMode, activeColor, holder.rbRadio)

                holder.itemView.setOnClickListener {
                    controller.selectType(organizeByType)
                }
                setupSelectableBackground(holder.itemView, activeColor)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setScreenItems(screenItems: List<ScreenItem>) {
        this.screenItems = screenItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = screenItems.size

    override fun getItemViewType(position: Int): Int = screenItems[position].type

    private fun inflate(@LayoutRes layoutId: Int): View {
        return UiUtilities.inflate(context!!, controller.isNightMode, layoutId, parent, false)
    }

    private fun setupSelectableBackground(view: View, @ColorInt color: Int) {
        AndroidUtils.setBackground(
            view,
            UiUtilities.getColoredSelectableDrawable(view.context, color, 0.3f)
        )
    }

    private class SummaryViewHolder(itemView: View) : ViewHolder(itemView) {
        var tvSummary: TextView? = itemView.findViewById(R.id.description)

        init {
            itemView.findViewById<View>(R.id.divider).visibility = View.GONE
            itemView.findViewById<View>(R.id.card_bottom_divider).visibility = View.GONE
        }
    }

    private class HeaderViewHolder(itemView: View) : ViewHolder(itemView) {
        var tvTitle: TextView? = itemView.findViewById(R.id.title)
    }

    private class SelectableItemViewHolder(itemView: View) : ViewHolder(itemView) {
        var ivIcon: ImageView? = itemView.findViewById(R.id.icon)
        var tvTitle: TextView? = itemView.findViewById(R.id.title)
        var rbRadio: CompoundButton? = itemView.findViewById(R.id.compound_button)
    }

    private class DividerViewHolder(itemView: View) : ViewHolder(itemView)

    private class DividerWithPaddingViewHolder(itemView: View) : ViewHolder(itemView)

    private class SpaceViewHolder(itemView: View, hSpace: Int) : ViewHolder(itemView) {
        init {
            itemView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, hSpace)
        }
    }
}