package net.osmand.plus.gallery.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.widgets.popup.OsmAndDropdownMenu
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuColors
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuOption
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuSelectionStyle

class GallerySortBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

	private var sortMode by mutableStateOf(GallerySortMode.NAME_A_Z)
	private var sortModes by mutableStateOf(GallerySortMode.entries.toList())
	private var horizontalContentPaddingPx by mutableIntStateOf(0)
	private var menuExpanded by mutableStateOf(false)
	private var onSortModeSelected: ((GallerySortMode) -> Unit)? = null

	fun bind(
		sortMode: GallerySortMode,
		sortModes: List<GallerySortMode>,
		horizontalContentPaddingPx: Int,
		onSortModeSelected: (GallerySortMode) -> Unit
	) {
		this.sortMode = sortMode
		this.sortModes = sortModes.toList()
		this.horizontalContentPaddingPx = horizontalContentPaddingPx
		this.onSortModeSelected = onSortModeSelected
	}

	@Composable
	override fun Content() {
		val listBackground = colorAttr(R.attr.list_background_color)
		val dividerColor = colorAttr(R.attr.divider_color_basic)
		val activeColor = colorAttr(R.attr.active_color_primary)
		val textColor = colorAttr(android.R.attr.textColorPrimary)
		val secondaryTextColor = colorAttr(android.R.attr.textColorSecondary)
		val iconColor = colorAttr(R.attr.default_icon_color)
		val horizontalPadding = with(LocalDensity.current) { horizontalContentPaddingPx.toDp() }
		val menuOptions = sortModes.mapIndexed { index, mode ->
			val nextMode = sortModes.getOrNull(index + 1)
			OsmAndDropdownMenuOption(
				value = mode,
				title = stringResource(mode.titleId),
				iconId = mode.iconId,
				selected = mode == sortMode,
				showDividerAfter = nextMode != null && nextMode.group != mode.group
			)
		}

		MaterialTheme(
			colorScheme = lightColorScheme(
				primary = activeColor,
				surface = listBackground,
				background = listBackground,
				onSurface = textColor
			)
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(listBackground)
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.height(dimensionResource(R.dimen.card_row_min_height))
						.clickable { menuExpanded = true }
						.padding(horizontal = horizontalPadding),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						painter = painterResource(sortMode.iconId),
						contentDescription = null,
						tint = activeColor,
						modifier = Modifier.size(dimensionResource(R.dimen.gallery_sort_icon_size))
					)
					Spacer(modifier = Modifier.width(dimensionResource(R.dimen.content_padding_half)))
					Text(
						text = stringResource(sortMode.titleId),
						color = activeColor,
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}

				OsmAndDropdownMenu(
					expanded = menuExpanded,
					onDismissRequest = { menuExpanded = false },
					options = menuOptions,
					onOptionSelected = {
						menuExpanded = false
						onSortModeSelected?.invoke(it)
					},
					colors = OsmAndDropdownMenuColors(
						background = listBackground,
						divider = dividerColor,
						text = textColor,
						secondaryText = secondaryTextColor,
						icon = iconColor,
						selected = iconColor
					),
					selectionStyle = OsmAndDropdownMenuSelectionStyle.CHECKMARK
				)
			}
		}
	}
}

@Composable
private fun colorAttr(attrId: Int): Color {
	val context = LocalContext.current
	val typedValue = TypedValue()
	context.theme.resolveAttribute(attrId, typedValue, true)
	return Color(
		if (typedValue.resourceId != 0) {
			ContextCompat.getColor(context, typedValue.resourceId)
		} else {
			typedValue.data
		}
	)
}