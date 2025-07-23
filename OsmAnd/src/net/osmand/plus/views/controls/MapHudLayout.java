package net.osmand.plus.views.controls;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static net.osmand.plus.OsmAndConstants.UI_HANDLER_MAP_HUD;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_FULL_WIDTH;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_LEFT;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_RIGHT;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.POS_TOP;

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
import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapHudLayout extends FrameLayout {

	private static final int REFRESH_UI_ID = UI_HANDLER_MAP_HUD + 1;
	private static final int UI_REFRESH_INTERVAL_MILLIS = 100;

	private static final Log LOG = PlatformUtil.getLog(MapHudLayout.class);

	protected final OsmandApplication app;

	private final List<MapButton> mapButtons = new ArrayList<>();
	private final Map<View, ButtonPositionSize> widgetPositions = new LinkedHashMap<>();
	private final Map<View, ButtonPositionSize> additionalWidgetPositions = new LinkedHashMap<>();

	private final float dpToPx;
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
		this.statusBarHeight = AndroidUtils.getStatusBarHeight(context);
		this.tablet = AndroidUiHelper.isTablet(context);
		this.portrait = AndroidUiHelper.isOrientationPortrait(context);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		addPosition(findViewById(R.id.widget_top_bar));

		addPosition(findViewById(R.id.top_widgets_panel));

		addPosition(findViewById(R.id.map_left_widgets_panel));
		addPosition(findViewById(R.id.map_right_widgets_panel));

		addPosition(findViewById(R.id.measurement_buttons));
		addPosition(findViewById(R.id.recording_note_layout));
		addPosition(findViewById(R.id.add_gpx_point_bottom_sheet));
	}

	private void addPosition(@Nullable View view) {
		if (view != null) {
			addViewChangeListener(view);
			widgetPositions.put(view, createWidgetPosition(view));
		}
	}

	private void addViewChangeListener(@NonNull View view) {
		if (view instanceof ViewChangeProvider provider) {
			provider.addViewChangeListener(new ViewChangeListener() {
				@Override
				public void onSizeChanged(@NonNull View view, int width, int height, int oldWidth, int oldHeight) {
					if (width != oldWidth || height != oldHeight) {
						refresh();
					}
				}

				@Override
				public void onVisibilityChanged(@NonNull View view, int visibility) {
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
		addViewChangeListener(button);

		addView(button, params);
		mapButtons.add(button);
	}

	public void addWidget(@NonNull View view) {
		ButtonPositionSize position = createWidgetPosition(view);
		LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);

		updateButtonParams(params, position);
		addViewChangeListener(view);

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
		ButtonPositionSize.computeNonOverlap(1, new ArrayList<>(map.values()));
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
				map.put(view, position);
			}
		}
		for (MapButton button : mapButtons) {
			if (button.getVisibility() == VISIBLE) {
				ButtonPositionSize position = button.getDefaultPositionSize();
				if (position != null) {
					map.put(button, position);
				}
			}
		}
		for (Map.Entry<View, ButtonPositionSize> entry : additionalWidgetPositions.entrySet()) {
			View view = entry.getKey();
			if (view.getVisibility() == VISIBLE) {
				ButtonPositionSize position = updateWidgetPosition(view, entry.getValue());
				map.put(view, position);
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
			position.setPositionHorizontal(POS_FULL_WIDTH);
		} else if (view instanceof SideWidgetsPanel panel) {
			position.setPositionVertical(POS_TOP);
			position.setPositionHorizontal(panel.rightSide ? POS_RIGHT : POS_LEFT);

			if (portrait) {
				position.setMoveDescendantsVertical();
			} else {
				position.setMoveDescendantsHorizontal();
			}
		} else if (id == R.id.widget_top_bar) {
			position.setMoveDescendantsVertical();
			position.setPositionVertical(POS_TOP);
			position.setPositionHorizontal(POS_FULL_WIDTH);
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
	private ButtonPositionSize updateWidgetPosition(@NonNull View view, @NonNull ButtonPositionSize position) {
		int id = view.getId();
		int width = (int) AndroidUtils.pxToDpF(getContext(), view.getWidth()) / 8;
		int height = (int) AndroidUtils.pxToDpF(getContext(), view.getHeight()) / 8;
		position.setSize(width, height);

		if (view instanceof SideWidgetsPanel || id == R.id.measurement_buttons) {
			int parentWidth = getWidth();
			int parentHeight = getAdjustedHeight();
			int[] margins = AndroidUtils.getRelativeMargins(this, view);
			if (margins[0] >= 0 && margins[1] >= 0 && margins[2] >= 0 && margins[3] >= 0) {
				position.calcGridPositionFromPixel(dpToPx, parentWidth, parentHeight,
						position.isLeft(), position.isLeft() ? margins[0] : margins[2],
						position.isTop(), position.isTop() ? margins[1] - statusBarHeight : margins[3] - statusBarHeight);
			}
		} else if (view instanceof RulerWidget) {
			position.marginX = 0;
			position.marginY = 0;
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

	private int getAdjustedHeight() {
		return getHeight() - statusBarHeight;
	}
}
