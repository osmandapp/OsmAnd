package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.plus.widgets.dialogbutton.DialogButtonType

class AstroKnowledgeCardViewHolder(
	itemView: View,
	private val onActionClick: () -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val icon: ImageView = itemView.findViewById(R.id.icon)
	private val title: TextView = itemView.findViewById(R.id.title)
	private val description: TextView = itemView.findViewById(R.id.description)
	private val actionButton: DialogButton = itemView.findViewById(R.id.action_button)

	fun bind(item: AstroKnowledgeCardItem) {
		icon.setImageResource(item.getIconResId())
		title.setText(item.getTitleId())
		description.setText(item.getDescriptionId())
		actionButton.setButtonType(DialogButtonType.SECONDARY_ACTIVE)
		actionButton.setTitle(item.buttonTitle)
		actionButton.isEnabled = item.actionEnabled
		actionButton.setOnClickListener { onActionClick() }
	}
}
