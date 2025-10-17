package net.osmand.plus.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isNotEmpty

class ObservableFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onChildChanged: ((hasChild: Boolean) -> Unit)? = null

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        onChildChanged?.invoke(isNotEmpty())
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        onChildChanged?.invoke(isNotEmpty())
    }
}