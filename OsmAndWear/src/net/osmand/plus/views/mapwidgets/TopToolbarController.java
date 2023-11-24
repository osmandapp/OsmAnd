package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

public class TopToolbarController {

	public enum TopToolbarControllerType {
		QUICK_SEARCH,
		CONTEXT_MENU,
		TRACK_DETAILS,
		DISCOUNT,
		MEASUREMENT_TOOL,
		WEATHER,
		POI_FILTER,
		DOWNLOAD_MAP
	}

	public static final int NO_COLOR = -1;

	private final TopToolbarControllerType type;

	@ColorRes
	int bgLightId = R.color.list_background_color_light;
	@ColorRes
	int bgDarkId = R.color.list_background_color_dark;
	@DrawableRes
	int bgLightLandId = R.drawable.btn_round;
	@DrawableRes
	int bgDarkLandId = R.drawable.btn_round_night;
	@Nullable
	Drawable bgLight;
	@Nullable
	Drawable bgDark;
	@Nullable
	Drawable bgLightLand;
	@Nullable
	Drawable bgDarkLand;

	@DrawableRes
	int backBtnIconLightId = R.drawable.ic_arrow_back;
	@DrawableRes
	int backBtnIconDarkId = R.drawable.ic_arrow_back;
	@ColorRes
	int backBtnIconClrLightId = R.color.icon_color_default_light;
	@ColorRes
	int backBtnIconClrDarkId;
	@ColorInt
	int backBtnIconClrLight = -1;
	@ColorInt
	int backBtnIconClrDark = -1;

	@DrawableRes
	int closeBtnIconLightId = R.drawable.ic_action_remove_dark;
	@DrawableRes
	int closeBtnIconDarkId = R.drawable.ic_action_remove_dark;
	@ColorRes
	int closeBtnIconClrLightId = R.color.icon_color_default_light;
	@ColorRes
	int closeBtnIconClrDarkId;
	boolean closeBtnVisible = true;

	@DrawableRes
	int refreshBtnIconLightId = R.drawable.ic_action_refresh_dark;
	@DrawableRes
	int refreshBtnIconDarkId = R.drawable.ic_action_refresh_dark;
	@ColorRes
	int refreshBtnIconClrLightId = R.color.icon_color_default_light;
	@ColorRes
	int refreshBtnIconClrDarkId;

	boolean refreshBtnVisible;
	boolean saveViewVisible;
	boolean textBtnVisible;
	protected boolean topBarSwitchVisible;
	protected boolean topBarSwitchChecked;

	@ColorRes
	int titleTextClrLightId = R.color.text_color_primary_light;
	@ColorRes
	int titleTextClrDarkId = R.color.text_color_primary_dark;
	@ColorRes
	int descrTextClrLightId = R.color.text_color_primary_light;
	@ColorRes
	int descrTextClrDarkId = R.color.text_color_primary_dark;
	@ColorInt
	int titleTextClrLight = -1;
	@ColorInt
	int titleTextClrDark = -1;
	@ColorInt
	int descrTextClrLight = -1;
	@ColorInt
	int descrTextClrDark = -1;
	@ColorInt
	int textBtnTitleClrLight = -1;
	@ColorInt
	int textBtnTitleClrDark = -1;

	boolean singleLineTitle = true;

	boolean nightMode;

	String title = "";
	String description;
	String textBtnTitle;

	int saveViewTextId = -1;

	OnClickListener onBackButtonClickListener;
	OnClickListener onTitleClickListener;
	OnClickListener onCloseButtonClickListener;
	OnClickListener onRefreshButtonClickListener;
	OnClickListener onSaveViewClickListener;
	OnClickListener onTextBtnClickListener;
	OnCheckedChangeListener onSwitchCheckedChangeListener;

	Runnable onCloseToolbarListener;

	View bottomView;
	boolean topViewVisible = true;
	boolean shadowViewVisible = true;

	private boolean bottomViewAdded;

	public TopToolbarController(TopToolbarControllerType type) {
		this.type = type;
	}

	public TopToolbarControllerType getType() {
		return type;
	}

	@ColorInt
	public int getStatusBarColor(Context context, boolean nightMode) {
		return ContextCompat.getColor(context, nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setBottomView(View bottomView) {
		this.bottomView = bottomView;
	}

	public boolean isTopViewVisible() {
		return topViewVisible;
	}

	public void setTopViewVisible(boolean topViewVisible) {
		this.topViewVisible = topViewVisible;
	}

	public boolean isShadowViewVisible() {
		return shadowViewVisible;
	}

	public void setShadowViewVisible(boolean shadowViewVisible) {
		this.shadowViewVisible = shadowViewVisible;
	}

	public void setSingleLineTitle(boolean singleLineTitle) {
		this.singleLineTitle = singleLineTitle;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setBgIds(int bgLightId, int bgDarkId, int bgLightLandId, int bgDarkLandId) {
		this.bgLightId = bgLightId;
		this.bgDarkId = bgDarkId;
		this.bgLightLandId = bgLightLandId;
		this.bgDarkLandId = bgDarkLandId;
	}

	public void setBgs(Drawable bgLight, Drawable bgDark, Drawable bgLightLand, Drawable bgDarkLand) {
		this.bgLight = bgLight;
		this.bgDark = bgDark;
		this.bgLightLand = bgLightLand;
		this.bgDarkLand = bgDarkLand;
	}

	public void setBackBtnIconIds(int backBtnIconLightId, int backBtnIconDarkId) {
		this.backBtnIconLightId = backBtnIconLightId;
		this.backBtnIconDarkId = backBtnIconDarkId;
	}

	public void setBackBtnIconClrIds(int backBtnIconClrLightId, int backBtnIconClrDarkId) {
		this.backBtnIconClrLightId = backBtnIconClrLightId;
		this.backBtnIconClrDarkId = backBtnIconClrDarkId;
	}

	public void setBackBtnIconClrs(int backBtnIconClrLight, int backBtnIconClrDark) {
		this.backBtnIconClrLight = backBtnIconClrLight;
		this.backBtnIconClrDark = backBtnIconClrDark;
	}

	public void setCloseBtnIconIds(int closeBtnIconLightId, int closeBtnIconDarkId) {
		this.closeBtnIconLightId = closeBtnIconLightId;
		this.closeBtnIconDarkId = closeBtnIconDarkId;
	}

	public void setCloseBtnIconClrIds(int closeBtnIconClrLightId, int closeBtnIconClrDarkId) {
		this.closeBtnIconClrLightId = closeBtnIconClrLightId;
		this.closeBtnIconClrDarkId = closeBtnIconClrDarkId;
	}

	public void setRefreshBtnIconIds(int refreshBtnIconLightId, int refreshBtnIconDarkId) {
		this.refreshBtnIconLightId = refreshBtnIconLightId;
		this.refreshBtnIconDarkId = refreshBtnIconDarkId;
	}

	public void setRefreshBtnIconClrIds(int refreshBtnIconClrLightId, int refreshBtnIconClrDarkId) {
		this.refreshBtnIconClrLightId = refreshBtnIconClrLightId;
		this.refreshBtnIconClrDarkId = refreshBtnIconClrDarkId;
	}

	public void setCloseBtnVisible(boolean closeBtnVisible) {
		this.closeBtnVisible = closeBtnVisible;
	}

	public void setRefreshBtnVisible(boolean visible) {
		this.refreshBtnVisible = visible;
	}

	public void setSaveViewVisible(boolean visible) {
		this.saveViewVisible = visible;
	}

	public void setSaveViewTextId(int id) {
		this.saveViewTextId = id;
	}

	public void setTextBtnVisible(boolean visible) {
		this.textBtnVisible = visible;
	}

	public void setTextBtnTitle(String title) {
		this.textBtnTitle = title;
	}

	public void setTopBarSwitchVisible(boolean visible) {
		this.topBarSwitchVisible = visible;
	}

	public void setTopBarSwitchChecked(boolean checked) {
		this.topBarSwitchChecked = checked;
	}

	public void setTitleTextClrIds(int titleTextClrLightId, int titleTextClrDarkId) {
		this.titleTextClrLightId = titleTextClrLightId;
		this.titleTextClrDarkId = titleTextClrDarkId;
	}

	public void setTitleTextClrs(int titleTextClrLight, int titleTextClrDark) {
		this.titleTextClrLight = titleTextClrLight;
		this.titleTextClrDark = titleTextClrDark;
	}

	public void setDescrTextClrIds(int descrTextClrLightId, int descrTextClrDarkId) {
		this.descrTextClrLightId = descrTextClrLightId;
		this.descrTextClrDarkId = descrTextClrDarkId;
	}

	public void setDescrTextClrs(int descrTextClrLight, int descrTextClrDark) {
		this.descrTextClrLight = descrTextClrLight;
		this.descrTextClrDark = descrTextClrDark;
	}

	public void setTextBtnTitleClrs(int textBtnTitleClrLight, int textBtnTitleClrDark) {
		this.textBtnTitleClrLight = textBtnTitleClrLight;
		this.textBtnTitleClrDark = textBtnTitleClrDark;
	}

	public void setOnBackButtonClickListener(OnClickListener onBackButtonClickListener) {
		this.onBackButtonClickListener = onBackButtonClickListener;
	}

	public void setOnTitleClickListener(OnClickListener onTitleClickListener) {
		this.onTitleClickListener = onTitleClickListener;
	}

	public void setOnCloseButtonClickListener(OnClickListener onCloseButtonClickListener) {
		this.onCloseButtonClickListener = onCloseButtonClickListener;
	}

	public void setOnSaveViewClickListener(OnClickListener onSaveViewClickListener) {
		this.onSaveViewClickListener = onSaveViewClickListener;
	}

	public void setOnTextBtnClickListener(OnClickListener onTextBtnClickListener) {
		this.onTextBtnClickListener = onTextBtnClickListener;
	}

	public void setOnSwitchCheckedChangeListener(OnCheckedChangeListener onSwitchCheckedChangeListener) {
		this.onSwitchCheckedChangeListener = onSwitchCheckedChangeListener;
	}

	public void setOnRefreshButtonClickListener(OnClickListener onRefreshButtonClickListener) {
		this.onRefreshButtonClickListener = onRefreshButtonClickListener;
	}

	public void setOnCloseToolbarListener(Runnable onCloseToolbarListener) {
		this.onCloseToolbarListener = onCloseToolbarListener;
	}

	public void updateToolbar(@NonNull TopToolbarView toolbarView) {
		TextView titleView = toolbarView.getTitleView();
		TextView descrView = toolbarView.getDescrView();
		LinearLayout bottomViewLayout = toolbarView.getBottomViewLayout();
		SwitchCompat switchCompat = toolbarView.getTopBarSwitch();
		if (title != null) {
			titleView.setText(title);
			AndroidUiHelper.updateVisibility(titleView, true);
		} else {
			AndroidUiHelper.updateVisibility(titleView, false);
		}
		if (description != null) {
			descrView.setText(description);
			AndroidUiHelper.updateVisibility(descrView, true);
		} else {
			AndroidUiHelper.updateVisibility(descrView, false);
		}
		if (bottomView != null) {
			if (!bottomViewAdded) {
				bottomViewLayout.removeAllViews();
				bottomViewLayout.addView(bottomView);
				bottomViewLayout.setVisibility(View.VISIBLE);
				bottomViewAdded = true;
			}
		} else {
			bottomViewLayout.setVisibility(View.GONE);
		}
		AndroidUiHelper.updateVisibility(switchCompat, topBarSwitchVisible);
		if (topBarSwitchVisible) {
			switchCompat.setChecked(topBarSwitchChecked);
			if (topBarSwitchChecked) {
				DrawableCompat.setTint(switchCompat.getTrackDrawable(), ContextCompat.getColor(switchCompat.getContext(), R.color.map_toolbar_switch_track_color));
			}
		}
		View shadowView = toolbarView.getShadowView();
		if (shadowView != null) {
			AndroidUiHelper.updateVisibility(shadowView, isShadowViewVisible());
		}
	}
}