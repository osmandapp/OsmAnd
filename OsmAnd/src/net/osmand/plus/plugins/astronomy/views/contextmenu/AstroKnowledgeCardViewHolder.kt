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

	fun bind(card: AstroKnowledgeCardModel, nightMode: Boolean) {
		icon.setImageResource(card.getIconResId(nightMode))
		title.setText(card.getTitleId())
		description.setText(card.getDescriptionId())
		actionButton.setButtonType(DialogButtonType.SECONDARY)
		actionButton.setTitle(card.buttonTitle)
		actionButton.isEnabled = card.actionEnabled
		actionButton.setOnClickListener { onActionClick() }
	}
}
