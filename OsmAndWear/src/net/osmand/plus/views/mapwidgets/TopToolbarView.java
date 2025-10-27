package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType.CONTEXT_MENU;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.MapHudLayout.SizeChangeListener;
import net.osmand.plus.views.controls.MapHudLayout.ViewChangeProvider;
import net.osmand.plus.views.controls.MapHudLayout.VisibilityChangeListener;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;

import java.util.Iterator;
import java.util.LinkedList;

public class TopToolbarView extends FrameLayout implements ViewChangeProvider {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	private final LinkedList<TopToolbarController> controllers = new LinkedList<>();
	private final TopToolbarController defaultController = new TopToolbarController(CONTEXT_MENU);

	private MapActivity mapActivity;

	private View topBarLayout;
	private View topBarBottomView;
	private View topBarTitleLayout;
	private ImageButton backButton;
	private TextView titleView;
	private TextView descrView;
	private ImageButton refreshButton;
	private ImageButton closeButton;
	private TextView saveView;
	private TextView textBtn;
	private SwitchCompat topBarSwitch;
	private View shadowView;

	private SizeChangeListener sizeListener;
	private VisibilityChangeListener visibilityListener;

	private boolean nightMode;

	public TopToolbarView(@NonNull Context context) {
		this(context, null);
	}

	public TopToolbarView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TopToolbarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TopToolbarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.app = (OsmandApplication) context.getApplicationContext();
		this.uiUtilities = app.getUIUtilities();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		topBarLayout = findViewById(R.id.widget_top_bar_layout);
		topBarBottomView = findViewById(R.id.widget_top_bar_bottom_view);
		topBarTitleLayout = findViewById(R.id.widget_top_bar_title_layout);
		backButton = findViewById(R.id.widget_top_bar_back_button);
		refreshButton = findViewById(R.id.widget_top_bar_refresh_button);
		closeButton = findViewById(R.id.widget_top_bar_close_button);
		titleView = findViewById(R.id.widget_top_bar_title);
		saveView = findViewById(R.id.widget_top_bar_save);
		textBtn = findViewById(R.id.widget_top_bar_text_btn);
		descrView = findViewById(R.id.widget_top_bar_description);
		topBarSwitch = findViewById(R.id.widget_top_bar_switch);
		shadowView = findViewById(R.id.widget_top_bar_shadow);
	}

	@NonNull
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public boolean isTopToolbarViewVisible() {
		return getVisibility() == View.VISIBLE;
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
		app.getOsmandMap().getMapLayers().getMapMarkersLayer().getMarkersWidgetsHelper().setCustomLatLon(null);
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
		boolean visible = controller != null && !MapRouteInfoMenu.chooseRoutesVisible
				&& !MapRouteInfoMenu.waypointsVisible && (!mapActivity.getContextMenu().isVisible() || controller.getType() == CONTEXT_MENU);

		boolean updated = AndroidUiHelper.updateVisibility(this, visible);
		if (updated) {
			mapActivity.updateStatusBarColor();
		}
	}

	public void updateColors(TopToolbarController controller) {
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
				backButton.setImageDrawable(uiUtilities.getPaintedIcon(backBtnIconId, backBtnIconClr));
			} else {
				backButton.setImageDrawable(uiUtilities.getIcon(backBtnIconId, backBtnIconClrId));
			}
		}
		if (closeBtnIconId == 0) {
			closeButton.setImageDrawable(null);
		} else {
			closeButton.setImageDrawable(uiUtilities.getIcon(closeBtnIconId, closeBtnIconClrId));
		}
		if (refreshBtnIconId == 0) {
			refreshButton.setImageDrawable(null);
		} else {
			refreshButton.setImageDrawable(uiUtilities.getIcon(refreshBtnIconId, refreshBtnIconClrId));
		}
		int titleColor = titleTextClr != -1 ? titleTextClr : getContext().getColor(titleTextClrId);
		int descrColor = descrTextClr != -1 ? descrTextClr : getContext().getColor(descrTextClrId);
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
				saveView.setText(getContext().getString(controller.saveViewTextId));
				saveView.setContentDescription(getContext().getString(controller.saveViewTextId));
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

	@Override
	public void setSizeListener(@Nullable SizeChangeListener listener) {
		this.sizeListener = listener;
	}

	@Override
	public void setVisibilityListener(@Nullable VisibilityChangeListener listener) {
		this.visibilityListener = listener;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (sizeListener != null) {
			sizeListener.onSizeChanged(this, w, h, oldw, oldh);
		}
	}

	@Override
	protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (visibilityListener != null) {
			visibilityListener.onVisibilityChanged(changedView, visibility);
		}
	}
}