package net.osmand.plus.search.dialogs

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.DayNightMode
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.popup.OsmAndDropdownMenu
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuColors
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuOption

class ChipsLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

	enum class TextColorStyle {
		PRIMARY,
		SECONDARY,
		TERTIARY,
		PRIMARY_INVERSE,
		SECONDARY_INVERSE,
		TERTIARY_INVERSE
	}

	enum class IconColorStyle {
		DEFAULT,
		SECONDARY,
		PRIMARY,
		OSMAND,
		ACTIVE,
		WARNING
	}

	open class ChipData @JvmOverloads constructor(
		@JvmField val id: String,
		@DrawableRes @JvmField var iconId: Int,
		@JvmField var title: String?,
		@JvmField var selected: Boolean,
		@JvmField var visible: Boolean,
		@JvmField var enabled: Boolean,
		@JvmField var hasDropDown: Boolean,
		@JvmField var titleColor: TextColorStyle,
		@JvmField var iconColor: IconColorStyle,
		@StringRes @JvmField var menuTitleId: Int = 0,
		@JvmField var dropdownItems: List<DropdownItem> = emptyList(),
		@JvmField var showDropDownIconWhenDisabled: Boolean = false,
		@JvmField var onClickListener: OnChipClickListener? = null,
		@JvmField var onDropdownItemClickListener: OnDropdownItemClickListener? = null,
		@JvmField var contentDescription: String? = null
	) {
		fun updateContent(chip: ChipData) {
			iconId = chip.iconId
			title = chip.title
			selected = chip.selected
			visible = chip.visible
			enabled = chip.enabled
			hasDropDown = chip.hasDropDown
			titleColor = chip.titleColor
			iconColor = chip.iconColor
			menuTitleId = chip.menuTitleId
			dropdownItems = chip.dropdownItems
			showDropDownIconWhenDisabled = chip.showDropDownIconWhenDisabled
			onClickListener = chip.onClickListener
			onDropdownItemClickListener = chip.onDropdownItemClickListener
			contentDescription = chip.contentDescription
		}
	}

	class DropDownChipData @JvmOverloads constructor(
		id: String,
		@DrawableRes iconId: Int,
		title: String?,
		selected: Boolean,
		visible: Boolean,
		enabled: Boolean,
		titleColor: TextColorStyle,
		iconColor: IconColorStyle,
		@StringRes menuTitleId: Int = 0,
		dropdownItems: List<DropdownItem> = emptyList(),
		showDropDownIconWhenDisabled: Boolean = false,
		onDropdownItemClickListener: OnDropdownItemClickListener? = null,
		contentDescription: String? = null
	) : ChipData(
		id = id,
		iconId = iconId,
		title = title,
		selected = selected,
		visible = visible,
		enabled = enabled,
		hasDropDown = true,
		titleColor = titleColor,
		iconColor = iconColor,
		menuTitleId = menuTitleId,
		dropdownItems = dropdownItems,
		showDropDownIconWhenDisabled = showDropDownIconWhenDisabled,
		onDropdownItemClickListener = onDropdownItemClickListener,
		contentDescription = contentDescription
	)

	class DropdownItem @JvmOverloads constructor(
		@JvmField val id: Int,
		@DrawableRes @JvmField val iconId: Int,
		@JvmField val title: String,
		@JvmField val description: String? = null,
		@JvmField val selected: Boolean = false,
		@JvmField val enabled: Boolean = true,
		@JvmField val showDropDownIconWhenDisabled: Boolean = false,
		@JvmField val showDividerBelow: Boolean = false
	)

	fun interface OnChipClickListener {
		fun onChipClick(chipId: String)
	}

	fun interface OnDropdownItemClickListener {
		fun onDropdownItemClick(chipId: String, itemId: Int)
	}

	private var items by mutableStateOf<List<ChipData>>(emptyList(), neverEqualPolicy())
	private var expandedChipId by mutableStateOf<String?>(null)
	private var appMode by mutableStateOf<ApplicationMode?>(null)
	private var themeUsageContext by mutableStateOf(ThemeUsageContext.APP)
	private var nightMode by mutableStateOf(resolveNightMode())
	private var chipClickListener: OnChipClickListener? = null
	private var dropdownItemClickListener: OnDropdownItemClickListener? = null

	fun setThemeContext(appMode: ApplicationMode, themeUsageContext: ThemeUsageContext) {
		this.appMode = appMode
		this.themeUsageContext = themeUsageContext
		updateNightMode()
	}

	fun updateContent(chips: List<ChipData>) {
		val currentChips = items.associateBy { it.id }
		val updatedChips = chips.map { chip ->
			currentChips[chip.id]?.also { it.updateContent(chip) } ?: chip
		}
		items = updatedChips
		if (updatedChips.none { it.id == expandedChipId && it.visible && it.enabled && it.hasDropDown }) {
			expandedChipId = null
		}
	}

	fun setOnChipClickListener(listener: OnChipClickListener?) {
		chipClickListener = listener
	}

	fun setOnDropdownItemClickListener(listener: OnDropdownItemClickListener?) {
		dropdownItemClickListener = listener
	}

	@Composable
	override fun Content() {
		ObserveThemeChanges()
		ChipsLayoutContent(
			items = items,
			nightMode = nightMode,
			expandedChipId = expandedChipId,
			onExpandedChipChanged = { expandedChipId = it },
			onChipClick = { chipClickListener?.onChipClick(it) },
			onDropdownItemClick = { chipId, itemId ->
				expandedChipId = null
				dropdownItemClickListener?.onDropdownItemClick(chipId, itemId)
			}
		)
	}

	@Composable
	private fun ObserveThemeChanges() {
		DisposableEffect(Unit) {
			val app = context.applicationContext as? OsmandApplication
			if (app == null) {
				onDispose { }
			} else {
				val dayNightListener = StateChangedListener<DayNightMode> {
					app.runInUIThread { updateNightMode() }
				}
				val appThemeListener = StateChangedListener<Int> {
					app.runInUIThread { updateNightMode() }
				}
				app.settings.DAYNIGHT_MODE.addListener(dayNightListener)
				app.settings.OSMAND_THEME.addListener(appThemeListener)
				onDispose {
					app.settings.DAYNIGHT_MODE.removeListener(dayNightListener)
					app.settings.OSMAND_THEME.removeListener(appThemeListener)
				}
			}
		}
	}

	private fun updateNightMode() {
		nightMode = resolveNightMode()
	}

	private fun resolveNightMode(): Boolean {
		val app = context.applicationContext as? OsmandApplication ?: return false
		val appMode = appMode ?: app.settings.applicationMode
		return app.daynightHelper.isNightMode(appMode, themeUsageContext)
	}
}

@Composable
private fun ChipsLayoutContent(
	items: List<ChipsLayout.ChipData>,
	nightMode: Boolean,
	expandedChipId: String?,
	onExpandedChipChanged: (String?) -> Unit,
	onChipClick: (String) -> Unit,
	onDropdownItemClick: (String, Int) -> Unit
) {
	val activityBackground = colorAttr(R.attr.activity_background_color)
	val listBackground = colorAttr(R.attr.list_background_color)
	val dividerColor = colorAttr(R.attr.divider_color_basic)
	val activeColor = colorAttr(R.attr.active_color_primary)
	val inActiveColor = colorAttr(R.attr.secondary_icon_color)
	val activeBackground = colorAttr(R.attr.active_color_secondary)
	val contentPadding = dimensionResource(R.dimen.content_padding)
	val halfPadding = dimensionResource(R.dimen.content_padding_half)
	val chips = items.filter { it.visible }

	MaterialTheme(
		colorScheme = lightColorScheme(
			primary = activeColor,
			surface = listBackground,
			background = activityBackground,
			onSurface = textColor(ChipsLayout.TextColorStyle.PRIMARY)
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.height(36.dp)
				.horizontalScroll(rememberScrollState()),
			horizontalArrangement = Arrangement.spacedBy(halfPadding),
			verticalAlignment = Alignment.CenterVertically
		) {
			Spacer(modifier = Modifier.width(contentPadding - halfPadding))
			chips.forEach { chip ->
				ChipAnchor(
					chip = chip,
					expanded = expandedChipId == chip.id,
					changeExpandedState = { expanded ->
						onExpandedChipChanged(if (expanded) chip.id else null)
					},
					onChipClick = onChipClick,
					onDropdownItemClick = onDropdownItemClick,
					listBackground = listBackground,
					dividerColor = dividerColor,
					activeColor = activeColor,
					inActiveColor = inActiveColor,
					activeBackground = activeBackground,
					nightMode = nightMode
				)
			}
			Spacer(modifier = Modifier.width(contentPadding - halfPadding))
		}
	}
}

@Composable
private fun ChipAnchor(
	chip: ChipsLayout.ChipData,
	expanded: Boolean,
	changeExpandedState: (Boolean) -> Unit,
	onChipClick: (String) -> Unit,
	onDropdownItemClick: (String, Int) -> Unit,
	listBackground: Color,
	dividerColor: Color,
	activeColor: Color,
	inActiveColor: Color,
	activeBackground: Color,
	nightMode: Boolean
) {
	Box {
		val chipId = chip.id
		OsmandFilterChip(
			chipData = chip,
			contentDescription = chip.contentDescription,
			selected = chip.selected || expanded,
			onClick = {
				if (chip.hasDropDown && chip.enabled) {
					changeExpandedState(true)
				} else if (chip.enabled) {
					val clickListener = chip.onClickListener
					if (clickListener != null) {
						clickListener.onChipClick(chipId)
					} else {
						onChipClick(chipId)
					}
				}
			},
			listBackground = listBackground,
			dividerColor = dividerColor,
			activeColor = activeColor,
			inActiveColor = inActiveColor,
			activeBackground = activeBackground,
			nightMode = nightMode
		)
		if (chip.hasDropDown) {
			val menuOptions = chip.dropdownItems.map { item ->
				OsmAndDropdownMenuOption(
					value = item.id,
					title = item.title,
					iconId = if (item.iconId != 0) item.iconId else null,
					description = item.description,
					selected = item.selected,
					enabled = item.enabled,
					showDividerAfter = item.showDividerBelow
				)
			}
			OsmAndDropdownMenu(
				expanded = expanded && chip.enabled,
				onDismissRequest = { changeExpandedState(false) },
				options = menuOptions,
				onOptionSelected = { itemId ->
					changeExpandedState(false)
					val dropdownItemClickListener = chip.onDropdownItemClickListener
					if (dropdownItemClickListener != null) {
						dropdownItemClickListener.onDropdownItemClick(chipId, itemId)
					} else {
						onDropdownItemClick(chipId, itemId)
					}
				},
				colors = OsmAndDropdownMenuColors(
					background = listBackground,
					divider = dividerColor,
					text = textColor(ChipsLayout.TextColorStyle.PRIMARY),
					secondaryText = textColor(ChipsLayout.TextColorStyle.SECONDARY),
					icon = iconColor(ChipsLayout.IconColorStyle.DEFAULT, nightMode),
					selected = activeColor
				),
				title = if (chip.menuTitleId != 0) stringResource(chip.menuTitleId) else null
			)
		}
	}
}

@Composable
private fun OsmandFilterChip(
	chipData: ChipsLayout.ChipData,
	contentDescription: String? = null,
	selected: Boolean,
	onClick: () -> Unit,
	listBackground: Color,
	dividerColor: Color,
	activeColor: Color,
	inActiveColor: Color,
	activeBackground: Color,
	nightMode: Boolean
) {
	val labelColor = textColor(chipData.titleColor)
	val leadingIconColor = iconColor(chipData.iconColor, nightMode)
	val trailingIconColor = if (chipData.enabled) {
		labelColor
	} else {
		iconColor(ChipsLayout.IconColorStyle.SECONDARY, nightMode)
	}
	val trailingIconVisible =
		chipData.hasDropDown && (chipData.enabled || chipData.showDropDownIconWhenDisabled)
	val title = chipData.title
	val iconOnly = title == null && chipData.iconId != 0
	FilterChip(
		selected = selected,
		onClick = onClick,
		enabled = chipData.enabled,
		label = {
			if (iconOnly) {
				Icon(
					painter = painterResource(chipData.iconId),
					contentDescription = contentDescription,
					tint = leadingIconColor.copy(alpha = if (chipData.enabled) 1f else .5f),
					modifier = Modifier.size(18.dp)
				)
			} else if (title != null) {
				Text(
					text = title,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					fontSize = 14.sp,
					fontWeight = FontWeight.Medium
				)
			}
		},
		modifier = Modifier.height(36.dp),
		leadingIcon = if (chipData.iconId != 0 && !iconOnly) {
			{
				Icon(
					painter = painterResource(chipData.iconId),
					contentDescription = contentDescription,
					tint = leadingIconColor,
					modifier = Modifier.size(18.dp)
				)
			}
		} else {
			null
		},
		trailingIcon = if (trailingIconVisible) {
			{
				Icon(
					painter = painterResource(R.drawable.ic_action_arrow_drop_down),
					contentDescription = null,
					tint = trailingIconColor,
					modifier = Modifier.size(24.dp)
				)
			}
		} else {
			null
		},
		shape = RoundedCornerShape(8.dp),
		colors = FilterChipDefaults.filterChipColors(
			containerColor = listBackground,
			labelColor = labelColor,
			iconColor = leadingIconColor,
			disabledContainerColor = listBackground,
			disabledLabelColor = labelColor.copy(alpha = .5f),
			disabledLeadingIconColor = leadingIconColor.copy(alpha = .5f),
			disabledTrailingIconColor = trailingIconColor,
			selectedContainerColor = activeBackground,
			selectedLabelColor = labelColor,
			selectedLeadingIconColor = leadingIconColor,
			selectedTrailingIconColor = labelColor
		),
		border = FilterChipDefaults.filterChipBorder(
			enabled = chipData.enabled,
			selected = selected,
			borderColor = dividerColor,
			selectedBorderColor = activeColor,
			disabledBorderColor = dividerColor,
			borderWidth = 1.dp,
			selectedBorderWidth = 1.dp
		)
	)
}

@Composable
private fun textColor(style: ChipsLayout.TextColorStyle): Color {
	return colorAttr(
		when (style) {
			ChipsLayout.TextColorStyle.PRIMARY -> android.R.attr.textColorPrimary
			ChipsLayout.TextColorStyle.SECONDARY -> android.R.attr.textColorSecondary
			ChipsLayout.TextColorStyle.TERTIARY -> android.R.attr.textColorTertiary
			ChipsLayout.TextColorStyle.PRIMARY_INVERSE -> android.R.attr.textColorPrimaryInverse
			ChipsLayout.TextColorStyle.SECONDARY_INVERSE -> android.R.attr.textColorSecondaryInverse
			ChipsLayout.TextColorStyle.TERTIARY_INVERSE -> android.R.attr.textColorTertiaryInverse
		}
	)
}

@Composable
private fun iconColor(style: ChipsLayout.IconColorStyle, nightMode: Boolean): Color {
	val context = LocalContext.current
	val colorId = when (style) {
		ChipsLayout.IconColorStyle.DEFAULT -> ColorUtilities.getDefaultIconColorId(nightMode)
		ChipsLayout.IconColorStyle.SECONDARY -> ColorUtilities.getSecondaryIconColorId(nightMode)
		ChipsLayout.IconColorStyle.PRIMARY -> ColorUtilities.getPrimaryIconColorId(nightMode)
		ChipsLayout.IconColorStyle.OSMAND -> ColorUtilities.getOsmandIconColorId(nightMode)
		ChipsLayout.IconColorStyle.ACTIVE -> ColorUtilities.getActiveIconColorId(nightMode)
		ChipsLayout.IconColorStyle.WARNING -> ColorUtilities.getWarningColorId(nightMode)
	}
	return Color(ContextCompat.getColor(context, colorId))
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
