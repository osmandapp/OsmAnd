package net.osmand.plus.mapcontextmenu

import android.graphics.drawable.Drawable
import android.view.View.OnClickListener

class BuildRowAttrs private constructor(
    val icon: Drawable?,
    val iconId: Int,
    val buttonText: String?,
    val textPrefix: String?,
    val text: String,
    val secondaryText: String?,
    val textColor: Int,
    val isCollapsable: Boolean,
    val collapsableView: CollapsableView?,
    val isNeedLinks: Boolean,
    val textLinesLimit: Int,
    val isUrl: Boolean,
    val isNumber: Boolean,
    val isEmail: Boolean,
    val onClickListener: OnClickListener?,
    val isMatchWithDivider: Boolean
) {
    class Builder {
        private lateinit var text: String
        private var icon: Drawable? = null
        private var iconId: Int = 0
        private var buttonText: String? = null
        private var textPrefix: String? = null
        private var secondaryText: String? = null
        private var textColor: Int = 0
        private var isCollapsable: Boolean = false
        private var collapsableView: CollapsableView? = null
        private var isNeedLinks: Boolean = false
        private var textLinesLimit: Int = 0
        private var isUrl: Boolean = false
        private var isNumber: Boolean = false
        private var isEmail: Boolean = false
        private var onClickListener: OnClickListener? = null
        private var matchWithDivider: Boolean = false

        fun setText(v: String) = apply { text = v }
        fun setIcon(v: Drawable?) = apply { icon = v }
        fun setIconId(v: Int) = apply { iconId = v }
        fun setButtonText(v: String?) = apply { buttonText = v }
        fun setTextPrefix(v: String?) = apply { textPrefix = v }
        fun setSecondaryText(v: String?) = apply { secondaryText = v }
        fun setTextColor(v: Int) = apply { textColor = v }
        fun setCollapsable(v: Boolean) = apply { isCollapsable = v }
        fun setCollapsableView(v: CollapsableView?) = apply { collapsableView = v }
        fun setNeedLinks(v: Boolean) = apply { isNeedLinks = v }
        fun setTextLinesLimit(v: Int) = apply { textLinesLimit = v }
        fun setUrl(v: Boolean) = apply { isUrl = v }
        fun setNumber(v: Boolean) = apply { isNumber = v }
        fun setEmail(v: Boolean) = apply { isEmail = v }
        fun setOnClickListener(v: OnClickListener?) = apply { onClickListener = v }
        fun setMatchWithDivider(v: Boolean) = apply { matchWithDivider = v }
        fun markLabelUndefined() = apply {  }

        fun build() = BuildRowAttrs(
            icon, iconId, buttonText, textPrefix, text, secondaryText, textColor,
            isCollapsable, collapsableView, isNeedLinks, textLinesLimit,
            isUrl, isNumber, isEmail, onClickListener, matchWithDivider
        )
    }
}