package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class MenuRowBuilder {

	public static final String WITHIN_POLYGONS_ROW_KEY = "within_polygons";
	public static final String NEAREST_WIKI_KEY = "nearest_wiki_key";
	public static final String NEAREST_POI_KEY = "nearest_poi_key";
	public static final String DIVIDER_ROW_KEY = "divider_row_key";
	public static final String NAMES_ROW_KEY = "names_row_key";
	public static final String ALT_NAMES_ROW_KEY = "alt_names_row_key";

	public static final String ROUTE_MEMBERS_ROW_KEY = "route_members_row_key";
	public static final String ROUTE_PART_OF_ROW_KEY = "route_part_of_row_key";
	public static final String ROUTE_RELATED_ROUTES_ROW_KEY = "route_related_routes_row_key";

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final UiUtilities iconsCache;
	private boolean isLightContent;

	public MenuRowBuilder(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.iconsCache = app.getUIUtilities();
	}

	public void setLightContent(boolean lightContent) {
		this.isLightContent = lightContent;
	}

	public void buildDetailsRow(@NonNull View view, @Nullable Drawable icon, @Nullable String text,
	                            @Nullable String textPrefix, @Nullable String textSuffix,
	                            @Nullable CollapsableView collapsableView, boolean firstRow,
	                            boolean parentRow, @Nullable OnClickListener onClickListener) {
		if (!firstRow && !parentRow) {
			View horizontalLine = new View(view.getContext());
			horizontalLine.setTag(DIVIDER_ROW_KEY);
			LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
			llHorLineParams.gravity = Gravity.BOTTOM;
			AndroidUtils.setMargins(llHorLineParams, icon != null ? dpToPx(64f) : 0, 0, 0, 0);

			horizontalLine.setLayoutParams(llHorLineParams);
			horizontalLine.setBackgroundColor(getColor(isLightContent() ? R.color.divider_color_light : R.color.divider_color_dark));
			((LinearLayout) view).addView(horizontalLine);
		}

		boolean collapsable = collapsableView != null;

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		baseView.setLayoutParams(llBaseViewParams);

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(v -> {
			copyToClipboard(text, view.getContext());
			return true;
		});

		baseView.addView(ll);

		if (icon != null) {
			LinearLayout llIcon = new LinearLayout(view.getContext());
			llIcon.setOrientation(LinearLayout.HORIZONTAL);
			llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(64f), dpToPx(48f)));
			llIcon.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIcon);

			ImageView iconView = new ImageView(view.getContext());
			LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconParams, dpToPx(16f), dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconParams.gravity = Gravity.CENTER_VERTICAL;
			iconView.setLayoutParams(llIconParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconView.setImageDrawable(icon);
			llIcon.addView(iconView);
		}

		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textPrefixView = null;
		if (!Algorithms.isEmpty(textPrefix)) {
			textPrefixView = new TextView(view.getContext());
			LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			int startMargin = parentRow ? (icon == null ? dpToPx(16f) : 0) : 0;
			AndroidUtils.setMargins(llTextParams, startMargin, dpToPx(8f), 0, 0);
			textPrefixView.setLayoutParams(llTextParams);
			textPrefixView.setTextSize(12);
			textPrefixView.setTextColor(getColor(R.color.text_color_secondary_light));
			textPrefixView.setEllipsize(TextUtils.TruncateAt.END);
			textPrefixView.setMinLines(1);
			textPrefixView.setMaxLines(1);
			textPrefixView.setText(textPrefix);
		}

		TextView textSuffixView = null;
		if (!Algorithms.isEmpty(textSuffix)) {
			textSuffixView = new TextView(view.getContext());
			LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			int startMargin = parentRow ? (icon == null ? dpToPx(16f) : 0) : 0;
			AndroidUtils.setMargins(llTextParams, startMargin, 0, 0, dpToPx(8f));
			textSuffixView.setLayoutParams(llTextParams);
			textSuffixView.setTextSize(12);
			textSuffixView.setTextColor(getColor(R.color.text_color_secondary_light));
			textSuffixView.setEllipsize(TextUtils.TruncateAt.END);
			textSuffixView.setMinLines(1);
			textSuffixView.setMaxLines(1);
			textSuffixView.setText(textSuffix);
		}

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int startMargin = parentRow ? (icon == null ? dpToPx(16f) : 0) : 0;
		int topMargin = dpToPx(2f);
		int bottomMargin = dpToPx(2f);
		if (textPrefixView == null) {
			topMargin = collapsable && textSuffix == null ? dpToPx(13f) : dpToPx(8f);
		}
		if (textSuffixView == null) {
			bottomMargin = collapsable && textPrefixView == null ? dpToPx(13f) : dpToPx(8f);
		}
		AndroidUtils.setMargins(llTextParams, startMargin, topMargin, 0, bottomMargin);
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !isLightContent()));
		textView.setText(text);
		textView.setEllipsize(TextUtils.TruncateAt.END);
		textView.setMinLines(1);
		textView.setMaxLines(10);

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		AndroidUtils.setMargins(llTextViewParams, 0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		if (textPrefixView != null) {
			llText.addView(textPrefixView);
		}
		llText.addView(textView);
		if (textSuffixView != null) {
			llText.addView(textSuffixView);
		}

		ImageView iconViewCollapse = new ImageView(view.getContext());
		if (collapsableView != null) {
			LinearLayout llIconCollapse = new LinearLayout(view.getContext());
			llIconCollapse.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40f), ViewGroup.LayoutParams.MATCH_PARENT));
			llIconCollapse.setOrientation(LinearLayout.HORIZONTAL);
			llIconCollapse.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIconCollapse);

			LinearLayout.LayoutParams llIconCollapseParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconCollapseParams, 0, dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(getCollapseIcon(collapsableView.getContentView().getVisibility() == View.GONE));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(v -> {
				if (collapsableView.getContentView().getVisibility() == View.VISIBLE) {
					collapsableView.getContentView().setVisibility(View.GONE);
					iconViewCollapse.setImageDrawable(getCollapseIcon(true));
					collapsableView.setCollapsed(true);
				} else {
					collapsableView.getContentView().setVisibility(View.VISIBLE);
					iconViewCollapse.setImageDrawable(getCollapseIcon(false));
					collapsableView.setCollapsed(false);
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContentView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			baseView.addView(collapsableView.getContentView());
		} else if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}
		((LinearLayout) view).addView(baseView);
	}

	@NonNull
	public Drawable getCollapseIcon(boolean collapsed) {
		return iconsCache.getIcon(
				collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up,
				isLightContent() ? R.color.icon_color_default_light : R.color.icon_color_default_dark
		);
	}

	public void copyToClipboard(String text, Context ctx) {
		ShareMenu.copyToClipboardWithToast(ctx, text, false);
	}

	@NonNull
	public Drawable getRowIcon(int iconId) {
		int colorId = isLightContent() ? R.color.icon_color_secondary_light : R.color.icon_color_secondary_dark;
		return iconsCache.getIcon(iconId, colorId);
	}

	@NonNull
	public Drawable getThemedIcon(int iconId) {
		return iconsCache.getThemedIcon(iconId);
	}

	@Nullable
	public Drawable getRowIcon(Context ctx, String fileName) {
		return app.getUIUtilities().getRenderingIcon(ctx, fileName, isLightContent());
	}

	@ColorInt
	public int getColor(@ColorRes int resId) {
		return ColorUtilities.getColor(mapActivity, resId);
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	public boolean isLightContent() {
		return isLightContent;
	}
}
