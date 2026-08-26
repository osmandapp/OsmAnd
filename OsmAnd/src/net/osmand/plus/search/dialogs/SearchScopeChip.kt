package net.osmand.plus.search.dialogs

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.osmand.plus.R
import net.osmand.util.Algorithms

class SearchScopeChip @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

	private var scopeName by mutableStateOf<String?>(null)
	private var nightMode by mutableStateOf(false)

	init {
		isClickable = false
		isFocusable = false
	}

	override fun onDetachedFromWindow() {
		disposeComposition()
		super.onDetachedFromWindow()
	}

	fun setScopeName(scopeName: CharSequence?, nightMode: Boolean) {
		val name = scopeName?.toString()
		this.scopeName = name
		this.nightMode = nightMode
		visibility = if (Algorithms.isEmpty(name)) GONE else VISIBLE
	}

	@Composable
	override fun Content() {
		val name = scopeName ?: return
		val backgroundColor = colorResource(
			if (nightMode) R.color.chip_search_scope_bg_dark else R.color.chip_search_scope_bg_light
		)
		val activeColor = colorAttr(R.attr.active_color_primary)

		MaterialTheme(
			colorScheme = lightColorScheme(
				primary = activeColor,
				surface = backgroundColor,
				background = backgroundColor,
				onSurface = activeColor
			)
		) {
			Row(
				modifier = Modifier
					.wrapContentHeight()
					.wrapContentWidth()
					.background(backgroundColor, RoundedCornerShape(percent = 50))
					.padding(start = 6.dp, end = 9.dp, top = 2.dp, bottom = 2.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					painter = painterResource(R.drawable.ic_action_location_marker_outlined),
					contentDescription = null,
					tint = activeColor,
					modifier = Modifier.size(18.dp)
				)
				Spacer(modifier = Modifier.width(3.dp))
				Text(
					text = name,
					color = colorAttr(android.R.attr.textColorPrimary),
					fontSize = dimensionResource(id = R.dimen.default_desc_text_size).value.sp,
					fontWeight = FontWeight.Medium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
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
