package net.osmand.plus.views.controls;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static net.osmand.plus.OsmAndConstants.UI_HANDLER_MAP_HUD;
import static net.osmand.shared.grid.ButtonPositionSize.CELL_SIZE_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_FULL_WIDTH;
import static net.osmand.shared.grid.ButtonPositionSize.POS_LEFT;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;
import static net.osmand.shared.grid.ButtonPositionSize.POS_TOP;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.ViewChangeProvider.ViewChangeListener;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.shared.grid.ButtonPositionSize;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapHudLayout extends FrameLayout {

	private static final int REFRESH_UI_ID = UI_HANDLER_MAP_HUD + 1;
	private static final int REFRESH_VERTICAL_PANELS_ID = UI_HANDLER_MAP_HUD + 2;
	private static final int REFRESH_ALARMS_CONTAINER_ID = UI_HANDLER_MAP_HUD + 3;
	private static final int UI_REFRESH_INTERVAL_MILLIS = 100;
	private static final float TOP_BAR_MAX_WIDTH_PERCENTAGE = 0.6f;

	private static final Log LOG = PlatformUtil.getLog(MapHudLayout.class);

	protected final OsmandApplication app;

	private final List<MapButton> mapButtons = new ArrayList<>();
	private final Map<View, ButtonPositionSize> widgetPositions = new LinkedHashMap<>();
	private final Map<View, ButtonPositionSize> additionalWidgetPositions = new LinkedHashMap<>();

	private View alarmsContainer;
	private View topBarPanelContainer;
	private SideWidgetsPanel leftWidgetsPanel;
	private SideWidgetsPanel rightWidgetsPanel;
	private VerticalWidgetPanel bottomWidgetsPanel;

	private final float dpToPx;
	private final int panelsMargin;
	private final int statusBarHeight;

	private final boolean tablet;
	private final boolean portrait;

	public MapHudLayout(@NonNull Context context) {
		this(context, null);
	}

	public MapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.app = (OsmandApplication) context.getApplicationContext();
		this.dpToPx = AndroidUtils.dpToPxF(context, 1);
		this.panelsMargin = AndroidUtils.dpToPx(context, 16);
		this.statusBarHeight = AndroidUtils.getStatusBarHeight(context);
		this.tablet = AndroidUiHelper.isTablet(context);
		this.portrait = AndroidUiHelper.isOrientationPortrait(context);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		alarmsContainer = findViewById(R.id.alarms_container);
		leftWidgetsPanel = findViewById(R.id.map_left_widgets_panel);
		rightWidgetsPanel = findViewById(R.id.map_right_widgets_panel);
		topBarPanelContainer = findViewById(R.id.top_bar_panel_container);
		bottomWidgetsPanel = findViewById(R.id.map_bottom_widgets_panel);

		if (shouldCenterVerticalPanels()) {
			addPosition(leftWidgetsPanel, this::updateVerticalPanels);
			addPosition(rightWidgetsPanel, this::updateVerticalPanels);

			addPosition(findViewById(R.id.widget_top_bar));
			addPosition(findViewById(R.id.top_widgets_panel));
			addPosition(bottomWidgetsPanel, this::updateAlarmsContainer);
		} else {
			addPosition(findViewById(R.id.widget_top_bar));
			addPosition(findViewById(R.id.top_widgets_panel));
			addPosition(bottomWidgetsPanel);

			addPosition(leftWidgetsPanel);
			addPosition(rightWidgetsPanel);
		}
		addPosition(findViewById(R.id.measurement_buttons));
		addPosition(findViewById(R.id.recording_note_layout));
		addPosition(findViewById(R.id.add_gpx_point_bottom_sheet));
	}

	private void addPosition(@Nullable View view) {
		addPosition(view, null);
	}

	private void addPosition(@Nullable View view, @Nullable Runnable callback) {
		if (view != null) {
			addViewChangeListener(view, callback);
			widgetPositions.put(view, createWidgetPosition(view));
		}
	}

	private void addViewChangeListener(@NonNull View view, @Nullable Runnable callback) {
		if (view instanceof ViewChangeProvider provider) {
			provider.addViewChangeListener(new ViewChangeListener() {
				@Override
				public void onSizeChanged(@NonNull View view, int width, int height, int oldWidth,
						int oldHeight) {
					if (width != oldWidth || height != oldHeight) {
						if (callback != null) {
							callback.run();
						}
						refresh();
					}
				}

				@Override
				public void onVisibilityChanged(@NonNull View view, int visibility) {
					if (callback != null) {
						callback.run();
					}
					refresh();
				}
			});
		}
	}

	private void refresh() {
		app.runInUIThreadAndCancelPrevious(REFRESH_UI_ID, this::updateButtons, UI_REFRESH_INTERVAL_MILLIS);
	}

	public void addMapButton(@NonNull MapButton button) {
		LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		ButtonPositionSize position = button.getDefaultPositionSize();
		if (position != null) {
			updateButtonParams(params, position);
		}
		addViewChangeListener(button, null);

		addView(button, params);
		mapButtons.add(button);
	}

	public void addWidget(@NonNull View view) {
		ButtonPositionSize position = createWidgetPosition(view);
		LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);

		updateButtonParams(params, position);
		addViewChangeListener(view, null);

		addView(view, params);
		additionalWidgetPositions.put(view, position);
	}

	public void removeWidget(@NonNull View view) {
		additionalWidgetPositions.remove(view);
	}

	public void removeMapButton(@NonNull MapButton button) {
		removeView(button);
		mapButtons.remove(button);
	}

	public void updateButtons() {
		if (getWidth() <= 0 && getHeight() <= 0 && getVisibility() != VISIBLE) {
			return;
		}
		Map<View, ButtonPositionSize> map = getButtonPositionSizes();
		for (Map.Entry<View, ButtonPositionSize> entry : map.entrySet()) {
			View view = entry.getKey();
			if (view instanceof MapButton || view instanceof RulerWidget) {
				updatePositionParams(view, entry.getValue());
			}
		}
	}

	@NonNull
	private Map<View, ButtonPositionSize> getButtonPositionSizes() {
		Map<View, ButtonPositionSize> map = collectPositions();
//		LOG.info("--------START--------");
//		for (ButtonPositionSize b : map.values()) {
//			LOG.info(b + " value = " + b.toLongValue());
//		}
//		LOG.info("--------");

		int width = Math.round(getWidth() / dpToPx / CELL_SIZE_DP);
		int height = Math.round(getAdjustedHeight() / dpToPx / CELL_SIZE_DP);
		ButtonPositionSize.Companion.computeNonOverlap(1, new ArrayList<>(map.values()), width, height);

//		for (ButtonPositionSize b : map.values()) {
//			LOG.info(b + " value = " + b.toLongValue());
//		}
//		LOG.info("--------END--------");

		return map;
	}

	@NonNull
	public Map<View, ButtonPositionSize> collectPositions() {
		Map<View, ButtonPositionSize> map = new LinkedHashMap<>();

		for (Map.Entry<View, ButtonPositionSize> entry : widgetPositions.entrySet()) {
			View view = entry.getKey();
			if (view.getVisibility() == VISIBLE) {
				ButtonPositionSize position = createWidgetPosition(view);
				if (position.getHeight() > 0 && position.getWidth() > 0) {
					map.put(view, position);
				}
			}
		}
		for (MapButton button : mapButtons) {
			if (button.getVisibility() == VISIBLE) {
				ButtonPositionSize position = button.getDefaultPositionSize();
				if (position != null && position.getHeight() > 0 && position.getWidth() > 0) {
					map.put(button, position);
				}
			}
		}
		for (Map.Entry<View, ButtonPositionSize> entry : additionalWidgetPositions.entrySet()) {
			View view = entry.getKey();
			if (view.getVisibility() == VISIBLE) {
				ButtonPositionSize position = updateWidgetPosition(view, entry.getValue());
				if (position.getHeight() > 0 && position.getWidth() > 0) {
					map.put(view, position);
				}
			}
		}
		return map;
	}

	@NonNull
	private ButtonPositionSize createWidgetPosition(@NonNull View view) {
		int id = view.getId();
		String name = getViewName(view);
		ButtonPositionSize position = new ButtonPositionSize(name);
		if (view instanceof VerticalWidgetPanel panel) {
			position.setMoveDescendantsVertical();
			position.setPositionVertical(panel.isTopPanel() ? POS_TOP : POS_BOTTOM);
			position.setPositionHorizontal(shouldCenterVerticalPanels() ? POS_LEFT : POS_FULL_WIDTH);
		} else if (view instanceof SideWidgetsPanel panel) {
			position.setMoveDescendantsVertical();
			position.setPositionVertical(POS_TOP);
			position.setPositionHorizontal(panel.rightSide ? POS_RIGHT : POS_LEFT);
		} else if (id == R.id.widget_top_bar) {
			position.setMoveDescendantsVertical();
			position.setPositionVertical(POS_TOP);
			position.setPositionHorizontal(shouldCenterVerticalPanels() ? POS_LEFT : POS_FULL_WIDTH);
		} else if (id == R.id.measurement_buttons) {
			position.setMoveDescendantsHorizontal();
			position.setPositionVertical(POS_BOTTOM);
			position.setPositionHorizontal(POS_LEFT);
		} else if (id == R.id.add_gpx_point_bottom_sheet || id == R.id.recording_note_layout) {
			position.setMoveDescendantsVertical();
			position.setPositionVertical(POS_BOTTOM);
			position.setPositionHorizontal(POS_FULL_WIDTH);
		} else if (view instanceof RulerWidget) {
			position.setMoveHorizontal();
			position.setPositionVertical(POS_BOTTOM);
			position.setPositionHorizontal(POS_LEFT);
		}
		return updateWidgetPosition(view, position);
	}

	@NonNull
	private String getViewName(@NonNull View view) {
		try {
			return getResources().getResourceEntryName(view.getId());
		} catch (Resources.NotFoundException e) {
			return view.toString();
		}
	}

	@NonNull
	private ButtonPositionSize updateWidgetPosition(@NonNull View view,
			@NonNull ButtonPositionSize position) {
		int id = view.getId();
		int width = (int) AndroidUtils.pxToDpF(getContext(), view.getWidth()) / 8;
		int height = (int) AndroidUtils.pxToDpF(getContext(), view.getHeight()) / 8;
		position.setSize(width, height);

		if (view instanceof SideWidgetsPanel || view instanceof VerticalWidgetPanel && shouldCenterVerticalPanels()
				|| id == R.id.measurement_buttons) {
			int parentWidth = getWidth();
			int parentHeight = getAdjustedHeight();
			int[] margins = AndroidUtils.getRelativeMargins(this, view);
			if (margins[0] >= 0 && margins[1] >= 0 && margins[2] >= 0 && margins[3] >= 0) {
				boolean top = position.isTop();
				boolean left = position.isLeft();
				int x = left ? margins[0] : margins[2];
				int y = top ? margins[1] - statusBarHeight : margins[3] - statusBarHeight;
				position.calcGridPositionFromPixel(dpToPx, parentWidth, parentHeight, left, x, top, y);
			}
		} else if (view instanceof RulerWidget) {
			position.setMarginX(0);
			position.setMarginY(0);
		}
		return position;
	}

	public void updatePositionParams(@NonNull View view, @NonNull ButtonPositionSize position) {
		LayoutParams params = (LayoutParams) view.getLayoutParams();

		boolean changed = updateButtonParams(params, position);
		if (changed) {
			view.setLayoutParams(params);
		}
	}

	public boolean updateButtonParams(@NonNull LayoutParams params, @NonNull ButtonPositionSize position) {
		boolean changed = false;

		int gravity;
		int startMargin;
		int topMargin;
		int endMargin;
		int bottomMargin;

		int marginX = position.getXStartPix(dpToPx);
		int marginY = position.getYStartPix(dpToPx);

		if (position.isLeft()) {
			gravity = Gravity.START;
			endMargin = 0;
			startMargin = marginX;
		} else {
			gravity = Gravity.END;
			startMargin = 0;
			endMargin = marginX;
		}
		if (position.isTop()) {
			gravity |= Gravity.TOP;
			bottomMargin = 0;
			topMargin = marginY;
		} else {
			gravity |= Gravity.BOTTOM;
			topMargin = 0;
			bottomMargin = marginY;
		}
		if (startMargin != params.getMarginStart() || topMargin != params.topMargin
				|| endMargin != params.getMarginEnd() || bottomMargin != params.bottomMargin) {
			changed = true;

			params.topMargin = topMargin;
			params.bottomMargin = bottomMargin;
			params.setMarginStart(startMargin);
			params.setMarginEnd(endMargin);
		}
		if (params.gravity != gravity) {
			changed = true;
			params.gravity = gravity;
		}
		return changed;
	}

	public void updateButton(@NonNull MapButton button, boolean save) {
		MapButtonState buttonState = button.getButtonState();
		ButtonPositionSize positionSize = buttonState != null ? buttonState.getPositionSize() : null;
		if (buttonState != null) {
			int width = getWidth();
			int height = getAdjustedHeight();
			LayoutParams params = (LayoutParams) button.getLayoutParams();

			positionSize.calcGridPositionFromPixel(dpToPx, width, height,
					positionSize.isLeft(), positionSize.isLeft() ? params.getMarginStart() : params.getMarginEnd(),
					positionSize.isTop(), positionSize.isTop() ? params.topMargin : params.bottomMargin);
		}
		if (save) {
			button.savePosition();
		}
		updateButtons(); // relayout to avoid overlap
	}

	public int getAdjustedHeight() {
		return getHeight() - statusBarHeight;
	}

	private boolean shouldCenterVerticalPanels() {
		return !portrait || tablet;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (w > 0 && w != oldw) {
			updateVerticalPanels();

			if (shouldCenterVerticalPanels()) {
				updateAlarmsContainer();
			}
		}
	}

	public void updateVerticalPanels() {
		app.runInUIThreadAndCancelPrevious(REFRESH_VERTICAL_PANELS_ID, () -> {
			updateHorizontalMargins(topBarPanelContainer);
			updateHorizontalMargins(bottomWidgetsPanel);
		}, UI_REFRESH_INTERVAL_MILLIS);
	}

	private void updateHorizontalMargins(@Nullable View view) {
		int totalWidth = getWidth();
		if (view == null || leftWidgetsPanel == null || rightWidgetsPanel == null || totalWidth <= 0) {
			return;
		}
		if (view.getLayoutParams() instanceof MarginLayoutParams params) {
			int leftMargin = 0;
			int rightMargin = 0;

			if (shouldCenterVerticalPanels()) {
				int defaultWidth = (int) (totalWidth * TOP_BAR_MAX_WIDTH_PERCENTAGE);
				int defaultMargin = (totalWidth - defaultWidth) / 2;

				int leftWidth = leftWidgetsPanel.getVisibility() == VISIBLE ? leftWidgetsPanel.getWidth() : 0;
				int rightWidth = rightWidgetsPanel.getVisibility() == VISIBLE ? rightWidgetsPanel.getWidth() : 0;

				leftMargin = Math.max(defaultMargin, leftWidth > 0 ? leftWidth + panelsMargin : 0);
				rightMargin = Math.max(defaultMargin, rightWidth > 0 ? rightWidth + panelsMargin : 0);
			}
			if (params.leftMargin != leftMargin || params.rightMargin != rightMargin) {
				params.leftMargin = leftMargin;
				params.rightMargin = rightMargin;
				view.setLayoutParams(params);
			}
		}
	}

	private void updateAlarmsContainer() {
		app.runInUIThreadAndCancelPrevious(REFRESH_ALARMS_CONTAINER_ID, () -> {
			if (alarmsContainer != null && alarmsContainer.getLayoutParams() instanceof MarginLayoutParams params) {
				int marginId = portrait ? R.dimen.map_alarm_bottom_margin : R.dimen.map_alarm_bottom_margin_land;
				int baseMargin = getResources().getDimensionPixelSize(marginId);

				int panelMargin = 0;
				if (shouldCenterVerticalPanels() && bottomWidgetsPanel != null && bottomWidgetsPanel.getVisibility() == VISIBLE) {
					panelMargin = bottomWidgetsPanel.getHeight();
				}
				int bottomMargin = Math.max(baseMargin, panelMargin);
				if (params.bottomMargin != bottomMargin) {
					params.bottomMargin = bottomMargin;
					alarmsContainer.setLayoutParams(params);
				}
			}
		}, UI_REFRESH_INTERVAL_MILLIS);
	}
}
