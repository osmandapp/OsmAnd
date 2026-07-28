package net.osmand.plus.widgets.popup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.osmand.plus.R

data class OsmAndDropdownMenuOption<T>(
	val value: T,
	val title: String,
	@DrawableRes val iconId: Int? = null,
	val description: String? = null,
	val selected: Boolean = false,
	val enabled: Boolean = true,
	val showDividerAfter: Boolean = false
)

data class OsmAndDropdownMenuColors(
	val background: Color,
	val divider: Color,
	val text: Color,
	val secondaryText: Color,
	val icon: Color,
	val selected: Color
)

enum class OsmAndDropdownMenuSelectionStyle {
	RADIO,
	CHECKMARK,
	NONE
}

@Composable
fun <T> OsmAndDropdownMenu(
	expanded: Boolean,
	onDismissRequest: () -> Unit,
	options: List<OsmAndDropdownMenuOption<T>>,
	onOptionSelected: (T) -> Unit,
	colors: OsmAndDropdownMenuColors,
	modifier: Modifier = Modifier,
	title: String? = null,
	selectionStyle: OsmAndDropdownMenuSelectionStyle = OsmAndDropdownMenuSelectionStyle.RADIO
) {
	DropdownMenu(
		expanded = expanded,
		onDismissRequest = onDismissRequest,
		modifier = modifier.background(colors.background),
		offset = DpOffset(x = 0.dp, y = 4.dp)
	) {
		if (title != null) {
			Text(
				text = title,
				color = colors.secondaryText,
				fontSize = 16.sp,
				modifier = Modifier.padding(
					start = MENU_HORIZONTAL_PADDING,
					top = 12.dp,
					end = MENU_HORIZONTAL_PADDING,
					bottom = 8.dp
				)
			)
		}

		options.forEach { option ->
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.defaultMinSize(minHeight = MENU_ITEM_HEIGHT)
					.clickable(enabled = option.enabled) { onOptionSelected(option.value) }
					.padding(horizontal = MENU_HORIZONTAL_PADDING),
				verticalAlignment = Alignment.CenterVertically
			) {
				when (selectionStyle) {
					OsmAndDropdownMenuSelectionStyle.RADIO -> {
						if (option.iconId != null) {
							Icon(
								painter = painterResource(option.iconId),
								contentDescription = null,
								tint = colors.icon.copy(alpha = if (option.enabled) 1f else .5f),
								modifier = Modifier.size(MENU_ICON_SIZE)
							)
						} else {
							RadioButton(
								selected = option.selected,
								onClick = null,
								enabled = option.enabled,
								colors = RadioButtonDefaults.colors(selectedColor = colors.selected)
							)
						}
						Spacer(modifier = Modifier.width(MENU_CONTENT_SPACING))
					}
					OsmAndDropdownMenuSelectionStyle.CHECKMARK,
					OsmAndDropdownMenuSelectionStyle.NONE -> {
						option.iconId?.let {
							Icon(
								painter = painterResource(it),
								contentDescription = null,
								tint = colors.icon.copy(alpha = if (option.enabled) 1f else .5f),
								modifier = Modifier.size(MENU_ICON_SIZE)
							)
							Spacer(modifier = Modifier.width(MENU_CONTENT_SPACING))
						}
					}
				}

				Column(
					modifier = Modifier.weight(1f)
				) {
					Text(
						text = option.title,
						color = colors.text.copy(alpha = if (option.enabled) 1f else .5f),
						fontSize = 18.sp,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
					if (option.description != null) {
						Text(
							text = option.description,
							color = colors.secondaryText.copy(alpha = if (option.enabled) 1f else .5f),
							fontSize = 14.sp,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
					}
				}

				if (selectionStyle == OsmAndDropdownMenuSelectionStyle.CHECKMARK) {
					Spacer(modifier = Modifier.width(MENU_CONTENT_SPACING))
					if (option.selected) {
						Icon(
							painter = painterResource(R.drawable.ic_action_done),
							contentDescription = null,
							tint = colors.selected,
							modifier = Modifier.size(MENU_ICON_SIZE)
						)
					} else {
						Spacer(modifier = Modifier.size(MENU_ICON_SIZE))
					}
				}
			}

			if (option.showDividerAfter) {
				Spacer(
					modifier = Modifier
						.fillMaxWidth()
						.height(1.dp)
						.background(colors.divider)
				)
			}
		}
	}
}

private val MENU_ITEM_HEIGHT = 56.dp
private val MENU_HORIZONTAL_PADDING = 24.dp
private val MENU_CONTENT_SPACING = 24.dp
private val MENU_ICON_SIZE = 24.dp
