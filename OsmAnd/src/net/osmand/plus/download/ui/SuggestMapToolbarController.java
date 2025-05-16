package net.osmand.plus.download.ui;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
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
	protected final TextView tvSummary;
	protected final DialogButton btnClose;
	protected final DialogButton btnApply;

	private static String regionName;
	private static String lastProcessedRegionName;

	public SuggestMapToolbarController(@NonNull MapActivity mapActivity,
	                                   @NonNull String regionName, @LayoutRes int layoutId) {
		super(TopToolbarControllerType.SUGGEST_MAP);
		SuggestMapToolbarController.regionName = regionName;
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.nightMode = app.getDaynightHelper().isNightModeForMapControls();

		this.mainView = UiUtilities.inflate(mapActivity, nightMode, layoutId);
		if (!AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			mainView.setBackgroundResource(getLandscapeBottomSidesBgResId());
		} else {
			mainView.setBackgroundResource(getPortraitBgResId());
		}
		tvSummary = mainView.findViewById(R.id.description);
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
			String summary = String.format(app.getString(getSummaryPattern()), regionName);
			int startIndex = summary.indexOf(regionName);
			int endIndex = startIndex + regionName.length();
			SpannableStringBuilder description = new SpannableStringBuilder(summary);
			if (startIndex != -1 && endIndex != -1) {
				description.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			tvSummary.setText(description);
		}
	}

	@StringRes
	protected abstract int getSummaryPattern();

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