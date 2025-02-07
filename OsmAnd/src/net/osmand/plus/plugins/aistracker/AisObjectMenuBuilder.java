package net.osmand.plus.plugins.aistracker;


import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;
import net.osmand.plus.utils.FontCache;

public class AisObjectMenuBuilder extends MenuBuilder {

    public AisObjectMenuBuilder(@NonNull MapActivity mapActivity) {
        super(mapActivity);
    }

    public View buildRow(View view, Drawable icon, String buttonText, String textPrefix, String text,
                         int textColor, String secondaryText, boolean collapsable, CollapsableView collapsableView, boolean needLinks,
                         int textLinesLimit, boolean isUrl, boolean isNumber, boolean isEmail, View.OnClickListener onClickListener, boolean matchWidthDivider) {

        return buildAisRow(view,null, text, textColor, buttonText,null, textLinesLimit, matchWidthDivider);

        /*
        return super.buildRow(view, icon, buttonText, textPrefix, text, textColor, secondaryText, collapsable, collapsableView, needLinks,
                textLinesLimit, isUrl, isNumber, isEmail, onClickListener, matchWidthDivider);
         */
        /*
        return super.buildRow(view, icon, null, textPrefix, text, textColor, buttonText, collapsable, collapsableView, needLinks,
                textLinesLimit, isUrl, isNumber, isEmail, onClickListener, matchWidthDivider);
         */
    }

    private View buildAisRow(View view, String prefixText, String aisType, int aisTypeColor, String aisValue,
                            String suffixText, int textLinesLimit, boolean matchWidthDivider) {
        boolean light = isLightContent();

        if (!isFirstRow()) {
            buildRowDivider(view);
        }

        LinearLayout baseView = new LinearLayout(view.getContext());
        baseView.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        baseView.setLayoutParams(llBaseViewParams);

        LinearLayout ll = new LinearLayout(view.getContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(llParams);
        ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
        ll.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String textToCopy = Algorithms.isEmpty(prefixText) ? aisType : prefixText + ": " + aisType;
                copyToClipboard(textToCopy, view.getContext());
                return true;
            }
        });

        baseView.addView(ll);

        // prefixText
        LinearLayout llText = new LinearLayout(view.getContext());
        llText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        llTextViewParams.weight = 1f;
        AndroidUtils.setMargins(llTextViewParams, 0, 0, dpToPx(10f), 0);
        llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
        llText.setLayoutParams(llTextViewParams);
        ll.addView(llText);

        TextViewEx textPrefixView = null;
        if (!Algorithms.isEmpty(prefixText)) {
            textPrefixView = new TextViewEx(view.getContext());
            LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            AndroidUtils.setMargins(llTextParams, dpToPx(16f), dpToPx(8f), 0, 0);
            textPrefixView.setLayoutParams(llTextParams);
            textPrefixView.setTypeface(FontCache.getNormalFont());
            textPrefixView.setTextSize(12);
            textPrefixView.setTextColor(ColorUtilities.getSecondaryTextColor(app, !light));
            textPrefixView.setMinLines(1);
            textPrefixView.setMaxLines(1);
            textPrefixView.setText(prefixText);
            llText.addView(textPrefixView);
        }

        // aisType
        TextViewEx textView = new TextViewEx(view.getContext());
        LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        AndroidUtils.setMargins(llTextParams,
                dpToPx(16f), dpToPx(textPrefixView != null ? 2f : (suffixText != null ? 10f : 8f)), 0, dpToPx(suffixText != null ? 6f : 8f));
        textView.setLayoutParams(llTextParams);
        textView.setTypeface(FontCache.getNormalFont());
        textView.setTextSize(16);
        textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !light));
        textView.setText(aisType);

        if (textLinesLimit > 0) {
            textView.setMinLines(1);
            textView.setMaxLines(textLinesLimit);
            textView.setEllipsize(TextUtils.TruncateAt.END);
        }
        if (aisTypeColor > 0) {
            textView.setTextColor(getColor(aisTypeColor));
        }
        llText.addView(textView);

        // suffixText
        if (!TextUtils.isEmpty(suffixText)) {
            TextViewEx textViewSecondary = new TextViewEx(view.getContext());
            LinearLayout.LayoutParams llTextSecondaryParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            AndroidUtils.setMargins(llTextSecondaryParams, dpToPx(16f), 0, 0, dpToPx(6f));
            textViewSecondary.setLayoutParams(llTextSecondaryParams);
            textViewSecondary.setTypeface(FontCache.getNormalFont());
            textViewSecondary.setTextSize(14);
            textViewSecondary.setTextColor(ColorUtilities.getSecondaryTextColor(app, !light));
            textViewSecondary.setText(suffixText);
            llText.addView(textViewSecondary);
        }

        // aisValue
        if (!TextUtils.isEmpty(aisValue)) {
            TextViewEx buttonTextView = new TextViewEx(view.getContext());
            LinearLayout.LayoutParams buttonTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonTextViewParams.gravity = Gravity.CENTER_VERTICAL;
            AndroidUtils.setMargins(buttonTextViewParams, dpToPx(8), 0, dpToPx(8), 0);
            buttonTextView.setLayoutParams(buttonTextViewParams);
            buttonTextView.setTypeface(FontCache.getMediumFont());
            buttonTextView.setTextSize(16);
            buttonTextView.setTextColor(ContextCompat.getColor(view.getContext(), !light ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n));
            buttonTextView.setText(aisValue);
            ll.addView(buttonTextView);
        }

        ((LinearLayout) view).addView(baseView);

        rowBuilt();

        setDividerWidth(matchWidthDivider);

        return ll;
    }
}