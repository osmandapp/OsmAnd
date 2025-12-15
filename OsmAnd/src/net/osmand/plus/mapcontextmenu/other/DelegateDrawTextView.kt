package net.osmand.plus.mapcontextmenu.other

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import net.osmand.plus.mapcontextmenu.controllers.NetworkRouteDrawable

class DelegateDrawTextView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


	override fun draw(canvas: Canvas) {
		super.draw(canvas)
	}
	override fun onDraw(canvas: Canvas) {
		val parentView = parent as? ViewGroup
		if (parentView == null) {
			super.onDraw(canvas)
			return
		}

		val routeDrawable = parentView.children
			.filterIsInstance<ImageView>()
			.mapNotNull { it.drawable as? NetworkRouteDrawable }
			.firstOrNull()

		if (routeDrawable != null) {
			routeDrawable.drawText(canvas)
		} else {
			super.onDraw(canvas)
		}
	}
}