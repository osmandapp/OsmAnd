package net.osmand.plus.download.ui;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.util.Objects;

public abstract class SuggestMapToolbarController extends TopToolbarController {

	protected final MapActivity mapActivity;
	protected final OsmandApplication app;
	protected final boolean nightMode;

	protected final View mainView;
	protected final TextView tvPrimaryText;
	protected final TextView tvSecondaryText;
	protected final ImageView ivIcon;
	protected final DialogButton btnClose;
	protected final DialogButton btnApply;

	private static String regionName;
	private static String lastProcessedRegionName;

	public SuggestMapToolbarController(@NonNull MapActivity mapActivity, @NonNull String regionName) {
		super(TopToolbarControllerType.SUGGEST_MAP);
		SuggestMapToolbarController.regionName = regionName;
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);

		this.mainView = UiUtilities.inflate(mapActivity, nightMode, R.layout.banner_suggest_map);
		if (!AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			mainView.setBackgroundResource(getLandscapeBottomSidesBgResId());
		} else {
			mainView.setBackgroundResource(getPortraitBgResId());
		}
		tvPrimaryText = mainView.findViewById(R.id.primary_text);
		tvSecondaryText = mainView.findViewById(R.id.secondary_text);
		ivIcon = mainView.findViewById(R.id.icon);
		btnClose = mainView.findViewById(R.id.btnClose);
		btnApply = mainView.findViewById(R.id.btnApply);
	}

	protected void initializeUI() {
		btnClose.setOnClickListener(v -> dismiss());
		btnApply.setOnClickListener(v -> onApply());

		refreshView();
		setBottomView(mainView);
		setTopViewVisible(false);
		setShadowViewVisible(false);
	}

	protected void refreshView() {
		if (!Algorithms.isEmpty(regionName)) {
			String summary = String.format(app.getString(getPrimaryTextPattern()), regionName);
			int startIndex = summary.indexOf(regionName);
			int endIndex = startIndex + regionName.length();
			SpannableStringBuilder description = new SpannableStringBuilder(summary);
			if (startIndex != -1 && endIndex != -1) {
				description.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			tvPrimaryText.setText(description);
		}
		tvSecondaryText.setText(getSecondaryText());
		ivIcon.setImageResource(getIconId());
		ViewGroup.LayoutParams layoutParams = ivIcon.getLayoutParams();
		layoutParams.height = getPreferredIconHeight();
		layoutParams.width = getPreferredIconWidth();
		ivIcon.setLayoutParams(layoutParams);
		btnApply.setTitle(getApplyButtonTitle());
	}

	@StringRes
	protected abstract int getPrimaryTextPattern();

	@NonNull
	protected abstract String getSecondaryText();

	@DrawableRes
	protected abstract int getIconId();

	protected abstract int getPreferredIconHeight();

	protected abstract int getPreferredIconWidth();

	@NonNull
	protected abstract String getApplyButtonTitle();

	protected abstract void onApply();

	protected void dismiss() {
		lastProcessedRegionName = regionName;
		SuggestMapToolbarController.regionName = null;
		mapActivity.hideTopToolbar(this);
	}

	public String getRegionName() {
		return regionName;
	}

	@DrawableRes
	private int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_top_menu_dark : R.drawable.bg_top_menu_light;
	}

	@DrawableRes
	private int getLandscapeBottomSidesBgResId() {
		return nightMode
				? R.drawable.bg_top_sheet_bottom_sides_landscape_dark
				: R.drawable.bg_top_sheet_bottom_sides_landscape_light;
	}

	public static boolean isLastProcessedRegionName(@NonNull String regionName) {
		return Objects.equals(regionName, lastProcessedRegionName);
	}
}