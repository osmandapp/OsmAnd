package net.osmand.plus.views.mapwidgets;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;

import java.util.Iterator;
import java.util.LinkedList;

public class TopToolbarView {

	private final MapActivity mapActivity;

	private final LinkedList<TopToolbarController> controllers = new LinkedList<>();
	private final TopToolbarController defaultController = new TopToolbarController(TopToolbarControllerType.CONTEXT_MENU);

	private final View topBar;
	private final View topBarLayout;
	private final View topBarBottomView;
	private final View topBarTitleLayout;
	private final ImageButton backButton;
	private final TextView titleView;
	private final TextView descrView;
	private final ImageButton refreshButton;
	private final ImageButton closeButton;
	private final TextView saveView;
	private final TextView textBtn;
	private final SwitchCompat topBarSwitch;
	private final View shadowView;
	private boolean nightMode;

	public TopToolbarView(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;

		topBar = mapActivity.findViewById(R.id.widget_top_bar);
		topBarLayout = mapActivity.findViewById(R.id.widget_top_bar_layout);
		topBarBottomView = mapActivity.findViewById(R.id.widget_top_bar_bottom_view);
		topBarTitleLayout = mapActivity.findViewById(R.id.widget_top_bar_title_layout);
		backButton = mapActivity.findViewById(R.id.widget_top_bar_back_button);
		refreshButton = mapActivity.findViewById(R.id.widget_top_bar_refresh_button);
		closeButton = mapActivity.findViewById(R.id.widget_top_bar_close_button);
		titleView = mapActivity.findViewById(R.id.widget_top_bar_title);
		saveView = mapActivity.findViewById(R.id.widget_top_bar_save);
		textBtn = mapActivity.findViewById(R.id.widget_top_bar_text_btn);
		descrView = mapActivity.findViewById(R.id.widget_top_bar_description);
		topBarSwitch = mapActivity.findViewById(R.id.widget_top_bar_switch);
		shadowView = mapActivity.findViewById(R.id.widget_top_bar_shadow);
		AndroidUiHelper.updateVisibility(topBar, false);
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public View getTopBar() {
		return topBar;
	}

	public boolean isTopToolbarViewVisible() {
		return topBar.getVisibility() == View.VISIBLE;
	}

	public View getTopBarLayout() {
		return topBarLayout;
	}

	public ImageButton getBackButton() {
		return backButton;
	}

	public TextView getTitleView() {
		return titleView;
	}

	public LinearLayout getBottomViewLayout() {
		return (LinearLayout) topBarBottomView;
	}

	public TextView getDescrView() {
		return descrView;
	}

	public ImageButton getCloseButton() {
		return closeButton;
	}

	public TextView getSaveView() {
		return saveView;
	}

	public SwitchCompat getTopBarSwitch() {
		return topBarSwitch;
	}

	public ImageButton getRefreshButton() {
		return refreshButton;
	}

	public View getShadowView() {
		return shadowView;
	}

	public TopToolbarController getTopController() {
		if (controllers.size() > 0) {
			return controllers.get(controllers.size() - 1);
		} else {
			return null;
		}
	}

	public TopToolbarController getController(TopToolbarControllerType type) {
		for (TopToolbarController controller : controllers) {
			if (controller.getType() == type) {
				return controller;
			}
		}
		return null;
	}

	public void addController(TopToolbarController controller) {
		for (Iterator<TopToolbarController> iterator = controllers.iterator(); iterator.hasNext(); ) {
			TopToolbarController ctrl = iterator.next();
			if (ctrl.getType() == controller.getType()) {
				if (controller.onCloseToolbarListener != null) {
					controller.onCloseToolbarListener.run();
				}
				iterator.remove();
			}
		}
		controllers.add(controller);
		mapActivity.getMapLayers().getMapMarkersLayer().getMarkersWidgetsHelper().setCustomLatLon(null);
		updateColors();
		updateInfo();
	}

	public void removeController(TopToolbarController controller) {
		if (controller.onCloseToolbarListener != null) {
			controller.onCloseToolbarListener.run();
		}
		controllers.remove(controller);
		updateColors();
		updateInfo();
	}

	private void initToolbar(TopToolbarController controller) {
		backButton.setOnClickListener(controller.onBackButtonClickListener);
		topBarTitleLayout.setOnClickListener(controller.onTitleClickListener);
		closeButton.setOnClickListener(controller.onCloseButtonClickListener);
		refreshButton.setOnClickListener(controller.onRefreshButtonClickListener);
		saveView.setOnClickListener(controller.onSaveViewClickListener);
		textBtn.setOnClickListener(controller.onTextBtnClickListener);
		topBarSwitch.setOnCheckedChangeListener(controller.onSwitchCheckedChangeListener);
	}

	public void updateInfo() {
		TopToolbarController controller = getTopController();
		if (controller != null) {
			initToolbar(controller);
			controller.updateToolbar(this);
		} else {
			initToolbar(defaultController);
			defaultController.updateToolbar(this);
		}
		boolean updated = AndroidUiHelper.updateVisibility(topBar, controller != null && !MapRouteInfoMenu.chooseRoutesVisible && !MapRouteInfoMenu.waypointsVisible &&
				(!mapActivity.getContextMenu().isVisible() || controller.getType() == TopToolbarController.TopToolbarControllerType.CONTEXT_MENU));
		if (updated) {
			mapActivity.updateStatusBarColor();
		}
	}

	public void updateColors(TopToolbarController controller) {
		UiUtilities uiUtils = mapActivity.getMyApplication().getUIUtilities();
		controller.nightMode = nightMode;

		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		int bgId = portrait
				? nightMode ? controller.bgDarkId : controller.bgLightId
				: nightMode ? controller.bgDarkLandId : controller.bgLightLandId;
		Drawable bg = portrait
				? nightMode ? controller.bgDark : controller.bgLight
				: nightMode ? controller.bgDarkLand : controller.bgLightLand;
		int backBtnIconId = nightMode ? controller.backBtnIconDarkId : controller.backBtnIconLightId;
		int backBtnIconClr = nightMode ? controller.backBtnIconClrDark : controller.backBtnIconClrLight;
		int backBtnIconClrId = nightMode ? controller.backBtnIconClrDarkId : controller.backBtnIconClrLightId;
		int closeBtnIconId = nightMode ? controller.closeBtnIconDarkId : controller.closeBtnIconLightId;
		int closeBtnIconClrId = nightMode ? controller.closeBtnIconClrDarkId : controller.closeBtnIconClrLightId;
		int refreshBtnIconId = nightMode ? controller.refreshBtnIconDarkId : controller.refreshBtnIconLightId;
		int refreshBtnIconClrId = nightMode ? controller.refreshBtnIconClrDarkId : controller.refreshBtnIconClrLightId;
		int titleTextClr = nightMode ? controller.titleTextClrDark : controller.titleTextClrLight;
		int titleTextClrId = nightMode ? controller.titleTextClrDarkId : controller.titleTextClrLightId;
		int descrTextClr = nightMode ? controller.descrTextClrDark : controller.descrTextClrLight;
		int descrTextClrId = nightMode ? controller.descrTextClrDarkId : controller.descrTextClrLightId;
		int textBtnTitleClr = nightMode ? controller.textBtnTitleClrDark : controller.textBtnTitleClrLight;

		if (controller.isTopViewVisible()) {
			if (bg != null) {
				topBarLayout.setBackground(bg);
			} else {
				topBarLayout.setBackgroundResource(bgId);
			}
			topBarLayout.setVisibility(View.VISIBLE);
		} else {
			topBarLayout.setVisibility(View.GONE);
		}

		if (backBtnIconId == 0) {
			backButton.setImageDrawable(null);
		} else {
			if (backBtnIconClr != -1) {
				backButton.setImageDrawable(uiUtils.getPaintedIcon(backBtnIconId, backBtnIconClr));
			} else {
				backButton.setImageDrawable(uiUtils.getIcon(backBtnIconId, backBtnIconClrId));
			}
		}
		if (closeBtnIconId == 0) {
			closeButton.setImageDrawable(null);
		} else {
			closeButton.setImageDrawable(uiUtils.getIcon(closeBtnIconId, closeBtnIconClrId));
		}
		if (refreshBtnIconId == 0) {
			refreshButton.setImageDrawable(null);
		} else {
			refreshButton.setImageDrawable(uiUtils.getIcon(refreshBtnIconId, refreshBtnIconClrId));
		}
		int titleColor = titleTextClr != -1 ? titleTextClr : mapActivity.getColor(titleTextClrId);
		int descrColor = descrTextClr != -1 ? descrTextClr : mapActivity.getColor(descrTextClrId);
		titleView.setTextColor(titleColor);
		descrView.setTextColor(descrColor);
		saveView.setTextColor(titleColor);
		if (textBtnTitleClr != -1) {
			textBtn.setTextColor(textBtnTitleClr);
		}

		titleView.setSingleLine(controller.singleLineTitle);

		if (controller.closeBtnVisible) {
			if (closeButton.getVisibility() == View.GONE) {
				closeButton.setVisibility(View.VISIBLE);
			}
		} else if (closeButton.getVisibility() == View.VISIBLE) {
			closeButton.setVisibility(View.GONE);
		}
		if (controller.refreshBtnVisible) {
			if (refreshButton.getVisibility() == View.GONE) {
				refreshButton.setVisibility(View.VISIBLE);
			}
		} else if (refreshButton.getVisibility() == View.VISIBLE) {
			refreshButton.setVisibility(View.GONE);
		}
		if (controller.saveViewVisible) {
			if (controller.saveViewTextId != -1) {
				saveView.setText(mapActivity.getString(controller.saveViewTextId));
				saveView.setContentDescription(mapActivity.getString(controller.saveViewTextId));
			}
			if (saveView.getVisibility() == View.GONE) {
				saveView.setVisibility(View.VISIBLE);
			}
		} else if (saveView.getVisibility() == View.VISIBLE) {
			saveView.setVisibility(View.GONE);
		}
		if (controller.textBtnVisible) {
			textBtn.setText(controller.textBtnTitle);
			textBtn.setContentDescription(controller.textBtnTitle);
			if (textBtn.getVisibility() == View.GONE) {
				textBtn.setVisibility(View.VISIBLE);
			}
		} else if (textBtn.getVisibility() == View.VISIBLE) {
			textBtn.setVisibility(View.GONE);
		}
	}

	public void updateColors() {
		TopToolbarController controller = getTopController();
		if (controller != null) {
			updateColors(controller);
		} else {
			updateColors(defaultController);
		}
	}

	public void updateColors(boolean nightMode) {
		this.nightMode = nightMode;
		for (TopToolbarController controller : controllers) {
			controller.nightMode = nightMode;
		}
		updateColors();
	}

	public boolean isNightMode() {
		return nightMode;
	}
}