package net.osmand.plus.widgets.popup

import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import net.osmand.PlatformUtil
import net.osmand.plus.R

private val LOG = PlatformUtil.getLog("OsmAndDropdownMenu")

private val MENU_HORIZONTAL_PADDING = 12.dp
private val MENU_ICON_SIZE = 24.dp

data class OsmAndDropdownMenuOption<T>(
	val value: T,
	val title: String,
	@DrawableRes val iconId: Int? = null,
	val iconDrawable: Drawable? = null,
	val description: String? = null,
	val selected: Boolean = false,
	val selectedColor: Color? = null,
	val isCheckbox: Boolean = false,
	val enabled: Boolean = true,
	val showDividerAfter: Boolean = false,
	val titleBold: Boolean = false,
	val titleColor: Color? = null,
	val trailingBadgeTitle: String? = null,
	val trailingBadgeIcon: Drawable? = null,
	val trailingBadgeColor: Color? = null
)

data class OsmAndDropdownMenuColors(
	val background: Color,
	val divider: Color,
	val text: Color,
	val secondaryText: Color,
	val icon: Color,
	val selected: Color,
	val control: Color
)

object OsmAndDropdownMenuDefaults {
	val Offset = DpOffset(x = 0.dp, y = 4.dp)

	@Composable
	fun colors(
		background: Color = colorAttr(R.attr.list_background_color),
		divider: Color = colorAttr(R.attr.divider_color_basic),
		text: Color = colorAttr(android.R.attr.textColorPrimary),
		secondaryText: Color = colorAttr(android.R.attr.textColorSecondary),
		icon: Color = colorAttr(R.attr.default_icon_color),
		selected: Color = colorAttr(R.attr.default_icon_color),
		control: Color = colorAttr(R.attr.secondary_icon_color)
	): OsmAndDropdownMenuColors {
		return OsmAndDropdownMenuColors(
			background = background,
			divider = divider,
			text = text,
			secondaryText = secondaryText,
			icon = icon,
			selected = selected,
			control = control
		)
	}
}

@Composable
fun OsmAndDropdownMenuTheme(
	content: @Composable () -> Unit
) {
	val background = colorAttr(R.attr.list_background_color)
	val onSurface = colorAttr(android.R.attr.textColorPrimary)
	val onSurfaceVariant = colorAttr(R.attr.default_icon_color)
	val outlineVariant = colorAttr(R.attr.divider_color_basic)
	val primary = colorAttr(R.attr.active_color_primary)

	MaterialTheme(
		colorScheme = MaterialTheme.colorScheme.copy(
			primary = primary,
			surface = background,
			surfaceContainer = background,
			surfaceContainerHigh = background,
			background = background,
			onSurface = onSurface,
			onSurfaceVariant = onSurfaceVariant,
			outlineVariant = outlineVariant
		),
		content = content
	)
}

@Composable
fun <T> OsmAndDropdownMenuContent(
	options: List<OsmAndDropdownMenuOption<T>>,
	onOptionSelected: (T) -> Unit,
	modifier: Modifier = Modifier,
	colors: OsmAndDropdownMenuColors? = null,
	title: String? = null
) {
	val hasSelection = options.any { it.selected && !it.isCheckbox }

	Column(modifier = modifier.width(IntrinsicSize.Max)) {
		if (title != null) {
			Text(
				text = title,
				color = colors?.secondaryText ?: MaterialTheme.colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = FontWeight.Medium,
				modifier = Modifier.padding(
					start = MENU_HORIZONTAL_PADDING,
					top = 8.dp,
					end = MENU_HORIZONTAL_PADDING,
					bottom = 4.dp
				)
			)
		}

		options.forEach { option ->
			val leadingIcon: (@Composable () -> Unit)? = when {
				option.iconId != null -> {
					{
						val baseColor = colors?.icon ?: MaterialTheme.colorScheme.onSurfaceVariant
						Icon(
							painter = painterResource(option.iconId),
							contentDescription = null,
							tint = if (option.enabled) baseColor else baseColor.copy(alpha = 0.38f),
							modifier = Modifier.size(MENU_ICON_SIZE)
						)
					}
				}

				option.iconDrawable != null -> {
					{
						AndroidDrawableIcon(
							drawable = option.iconDrawable,
							modifier = Modifier.size(MENU_ICON_SIZE)
						)
					}
				}

				else -> null
			}

			val trailingIcon: (@Composable () -> Unit)? = when {
				option.trailingBadgeTitle != null -> {
					{
						Row(
							modifier = Modifier
								.background(
									color = (colors?.control ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.12f),
									shape = RoundedCornerShape(4.dp)
								)
								.padding(horizontal = 6.dp, vertical = 2.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							if (option.trailingBadgeIcon != null) {
								AndroidDrawableIcon(
									drawable = option.trailingBadgeIcon,
									modifier = Modifier.size(16.dp)
								)
								Spacer(modifier = Modifier.width(4.dp))
							}
							Text(
								text = option.trailingBadgeTitle,
								color = option.trailingBadgeColor ?: colors?.selected ?: MaterialTheme.colorScheme.onSurfaceVariant,
								fontSize = 12.sp,
								fontWeight = FontWeight.Medium
							)
						}
					}
				}

				option.isCheckbox -> {
					{
						Checkbox(
							checked = option.selected,
							onCheckedChange = null,
							enabled = option.enabled,
							colors = CheckboxDefaults.colors(
								checkedColor = option.selectedColor ?: colors?.selected ?: MaterialTheme.colorScheme.primary,
								uncheckedColor = colors?.control ?: MaterialTheme.colorScheme.onSurfaceVariant,
								checkmarkColor = Color.White
							)
						)
					}
				}

				option.selected -> {
					{
						Icon(
							painter = painterResource(R.drawable.ic_action_done),
							contentDescription = null,
							tint = option.selectedColor ?: colors?.selected ?: MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(MENU_ICON_SIZE)
						)
					}
				}

				hasSelection -> {
					{
						Spacer(modifier = Modifier.size(MENU_ICON_SIZE))
					}
				}

				else -> null
			}

			DropdownMenuItem(
				text = {
					if (option.description != null) {
						Column {
							Text(
								text = option.title,
								color = option.titleColor ?: colors?.text?.copy(alpha = if (option.enabled) 1f else 0.5f)
								?: if (option.enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
								fontWeight = if (option.titleBold) FontWeight.Bold else FontWeight.Normal,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis
							)
							Text(
								text = option.description,
								color = colors?.secondaryText?.copy(alpha = if (option.enabled) 1f else 0.5f)
									?: if (option.enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
								fontSize = 12.sp,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis
							)
						}
					} else {
						Text(
							text = option.title,
							color = option.titleColor ?: colors?.text?.copy(alpha = if (option.enabled) 1f else 0.5f)
							?: if (option.enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
							fontWeight = if (option.titleBold) FontWeight.Bold else FontWeight.Normal,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
					}
				},
				onClick = { onOptionSelected(option.value) },
				leadingIcon = leadingIcon,
				trailingIcon = trailingIcon,
				enabled = option.enabled
			)

			if (option.showDividerAfter) {
				HorizontalDivider(color = colors?.divider ?: MaterialTheme.colorScheme.outlineVariant)
			}
		}
	}
}

@Composable
fun <T> OsmAndDropdownMenu(
	expanded: Boolean,
	onDismissRequest: () -> Unit,
	options: List<OsmAndDropdownMenuOption<T>>,
	onOptionSelected: (T) -> Unit,
	modifier: Modifier = Modifier,
	offset: DpOffset = OsmAndDropdownMenuDefaults.Offset,
	scrollState: ScrollState = rememberScrollState(),
	properties: PopupProperties = PopupProperties(focusable = true),
	shape: Shape = MenuDefaults.shape,
	containerColor: Color = MenuDefaults.containerColor,
	tonalElevation: Dp = 6.dp,
	shadowElevation: Dp = 8.dp,
	border: BorderStroke? = null,
	colors: OsmAndDropdownMenuColors? = null,
	title: String? = null
) {
	OsmAndDropdownMenuTheme {
		DropdownMenu(
			expanded = expanded,
			onDismissRequest = onDismissRequest,
			modifier = modifier,
			offset = offset,
			scrollState = scrollState,
			properties = properties,
			shape = shape,
			containerColor = colors?.background ?: containerColor,
			tonalElevation = tonalElevation,
			shadowElevation = shadowElevation,
			border = border
		) {
			OsmAndDropdownMenuContent(
				options = options,
				onOptionSelected = onOptionSelected,
				colors = colors,
				title = title
			)
		}
	}
}

fun showComposeDropdownMenu(displayData: PopUpMenuDisplayData): PopupWindow? {
	val anchorView = displayData.anchorView ?: return null
	val context = anchorView.context

	var lifecycleContext = context
	while (lifecycleContext is ContextWrapper && lifecycleContext !is LifecycleOwner) {
		lifecycleContext = lifecycleContext.baseContext
	}
	val lifecycleOwner = anchorView.findViewTreeLifecycleOwner() ?: (lifecycleContext as? LifecycleOwner)
	val savedStateOwner = anchorView.findViewTreeSavedStateRegistryOwner() ?: (lifecycleContext as? SavedStateRegistryOwner)
	val viewModelStoreOwner = anchorView.findViewTreeViewModelStoreOwner() ?: (lifecycleContext as? ViewModelStoreOwner)

	val composeView = ComposeView(context).apply {
		lifecycleOwner?.let { setViewTreeLifecycleOwner(it) }
		savedStateOwner?.let { setViewTreeSavedStateRegistryOwner(it) }
		viewModelStoreOwner?.let { setViewTreeViewModelStoreOwner(it) }
	}

	val shadowPadding = 8.dp
	val shadowPaddingPx = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		8f,
		context.resources.displayMetrics
	).toInt()
	val screenMarginPx = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		16f,
		context.resources.displayMetrics
	).toInt()
	val verticalSpacingPx = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		4f,
		context.resources.displayMetrics
	).toInt()

	val popupWindow = PopupWindow(
		composeView,
		ViewGroup.LayoutParams.WRAP_CONTENT,
		ViewGroup.LayoutParams.WRAP_CONTENT,
		true
	).apply {
		isOutsideTouchable = true
		isFocusable = true
		setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
	}

	composeView.setContent {
		val colors = if (displayData.bgColor != 0) {
			OsmAndDropdownMenuDefaults.colors(background = Color(displayData.bgColor))
		} else {
			null
		}
		OsmAndDropdownMenuTheme {
			Box(modifier = Modifier.padding(shadowPadding)) {
				Surface(
					shape = MenuDefaults.shape,
					color = colors?.background ?: MenuDefaults.containerColor,
					tonalElevation = 6.dp,
					shadowElevation = 8.dp
				) {
					OsmAndDropdownMenuContent(
						options = displayData.menuItems?.toDropdownOptions(displayData) ?: emptyList(),
						modifier = Modifier.padding(vertical = 8.dp),
						colors = colors,
						onOptionSelected = { item ->
							val listener = item.onClickListener ?: displayData.onItemClickListener
							listener?.onPopUpItemClicked(item)
							if (item.shouldDismissOnClick()) {
								popupWindow.dismiss()
							}
						}
					)
				}
			}
		}
	}

	val anchorLocation = IntArray(2)
	anchorView.getLocationOnScreen(anchorLocation)
	val screenWidth = context.resources.displayMetrics.widthPixels
	val anchorCenterX = anchorLocation[0] + anchorView.width / 2
	val isRtl = anchorView.layoutDirection == View.LAYOUT_DIRECTION_RTL
	val isAnchorOnRight = anchorCenterX > screenWidth / 2

	val gravity = displayData.dropDownGravity ?: run {
		if (isAnchorOnRight) {
			if (isRtl) Gravity.START or Gravity.TOP else Gravity.END or Gravity.TOP
		} else {
			if (isRtl) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP
		}
	}

	val defaultHOffset = if (isAnchorOnRight) -screenMarginPx else screenMarginPx
	var hOffset = (displayData.horizontalOffset ?: 0) + defaultHOffset
	if (isAnchorOnRight) {
		val maxHOffset = screenWidth - screenMarginPx - (anchorLocation[0] + anchorView.width)
		if (hOffset > maxHOffset) {
			hOffset = maxHOffset
		}
	} else {
		val minHOffset = screenMarginPx - anchorLocation[0]
		if (hOffset < minHOffset) {
			hOffset = minHOffset
		}
	}

	val screenHeight = context.resources.displayMetrics.heightPixels
	val menuItems = displayData.menuItems
	var totalHeightDp = 16f + 16f
	if (menuItems != null) {
		for (item in menuItems) {
			totalHeightDp += 48f
			if (item.shouldShowTopDivider()) {
				totalHeightDp += 8f
			}
		}
	}
	val approxMenuHeightPx = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		totalHeightDp,
		context.resources.displayMetrics
	).toInt()

	val spaceBelow = screenHeight - (anchorLocation[1] + anchorView.height)
	val spaceAbove = anchorLocation[1]
	val shouldShowAbove = displayData.customDropDown == PopUpMenuDisplayData.CustomDropDown.TOP_DROPDOWN ||
		(displayData.customDropDown != PopUpMenuDisplayData.CustomDropDown.BOTTOM_DROPDOWN &&
			spaceBelow < approxMenuHeightPx && spaceAbove > spaceBelow)

	val vOffset = if (shouldShowAbove) {
		-anchorView.height - approxMenuHeightPx + shadowPaddingPx - verticalSpacingPx + (displayData.verticalOffset ?: 0)
	} else {
		(displayData.verticalOffset ?: 0) - shadowPaddingPx + verticalSpacingPx
	}

	try {
		popupWindow.showAsDropDown(anchorView, hOffset, vOffset, gravity)
	} catch (e: Exception) {
		LOG.warn("Failed to show dropdown menu", e)
		return null
	}

	val popupDecorView = composeView.rootView
	lifecycleOwner?.let { popupDecorView.setViewTreeLifecycleOwner(it) }
	savedStateOwner?.let { popupDecorView.setViewTreeSavedStateRegistryOwner(it) }
	viewModelStoreOwner?.let { popupDecorView.setViewTreeViewModelStoreOwner(it) }

	return popupWindow
}


fun PopUpMenuItem.toDropdownOption(displayData: PopUpMenuDisplayData? = null): OsmAndDropdownMenuOption<PopUpMenuItem> {
	val isCheckbox = (isShowCompoundBtn && compoundButtonType == PopUpMenuItem.CompoundButtonType.CHECKBOX)
			|| displayData?.layoutId == R.layout.popup_menu_item_full_divider_check_box
			|| displayData?.layoutId == R.layout.popup_menu_item_checkbox
	val hasClickListener = onClickListener != null || displayData?.onItemClickListener != null
	return OsmAndDropdownMenuOption(
		value = this,
		title = title?.toString() ?: "",
		iconDrawable = icon,
		selected = isSelected,
		selectedColor = compoundBtnColor?.takeIf { it != 0 }?.let { Color(it) },
		isCheckbox = isCheckbox,
		enabled = hasClickListener || isShowCompoundBtn,
		showDividerAfter = false,
		titleBold = isTitleBold,
		titleColor = titleColor?.let { Color(it) },
		trailingBadgeTitle = trailingBadge?.title?.toString(),
		trailingBadgeIcon = trailingBadge?.icon,
		trailingBadgeColor = trailingBadge?.titleColor?.let { Color(it) }
	)
}

fun List<PopUpMenuItem>.toDropdownOptions(displayData: PopUpMenuDisplayData? = null): List<OsmAndDropdownMenuOption<PopUpMenuItem>> {
	return mapIndexed { index, item ->
		val nextItem = getOrNull(index + 1)
		item.toDropdownOption(displayData).copy(
			showDividerAfter = nextItem?.shouldShowTopDivider() == true
		)
	}
}


@Composable
fun AndroidDrawableIcon(
	drawable: Drawable,
	modifier: Modifier = Modifier,
	tint: Color = Color.Unspecified
) {
	val mutatedDrawable = remember(drawable, tint) {
		drawable.mutate().apply {
			if (tint != Color.Unspecified) {
				setTint(tint.toArgb())
			}
		}
	}
	Canvas(modifier = modifier) {
		drawIntoCanvas { canvas ->
			val width = size.width.toInt()
			val height = size.height.toInt()
			val intrinsicWidth = mutatedDrawable.intrinsicWidth
			val intrinsicHeight = mutatedDrawable.intrinsicHeight
			if (intrinsicWidth > 0 && intrinsicHeight > 0) {
				val scale = minOf(size.width / intrinsicWidth, size.height / intrinsicHeight)
				val scaledWidth = (intrinsicWidth * scale).toInt()
				val scaledHeight = (intrinsicHeight * scale).toInt()
				val left = (width - scaledWidth) / 2
				val top = (height - scaledHeight) / 2
				mutatedDrawable.setBounds(left, top, left + scaledWidth, top + scaledHeight)
			} else {
				mutatedDrawable.setBounds(0, 0, width, height)
			}
			mutatedDrawable.draw(canvas.nativeCanvas)
		}
	}
}

@Composable
fun colorAttr(attrId: Int): Color {
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
