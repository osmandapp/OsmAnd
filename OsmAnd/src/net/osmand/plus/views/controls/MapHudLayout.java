package net.osmand.plus.views.controls;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static net.osmand.plus.OsmAndConstants.UI_HANDLER_MAP_HUD;
import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.DEF_MARGIN_DP;

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
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

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

	private final float dpToPx;
	private final int statusBarHeight;

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
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		addPosition(findViewById(R.id.widget_top_bar));
		addPosition(findViewById(R.id.map_left_widgets_panel));
		addPosition(findViewById(R.id.map_right_widgets_panel));
		addPosition(findViewById(R.id.top_widgets_panel));
		addPosition(findViewById(R.id.map_bottom_widgets_panel));

		updateRulerWidget(findViewById(R.id.map_ruler_container));
	}

	private void addPosition(@Nullable View view) {
		if (view != null) {
			addChangeListeners(view);
			widgetPositions.put(view, createWidgetPosition(view));
		}
	}

	private void addChangeListeners(@NonNull View view) {
		if (view instanceof ViewChangeProvider provider) {
			provider.setSizeListener((v, width, height, oldWidth, oldHeight) -> {
				if (width != oldWidth || height != oldHeight) {
					refresh();
				}
			});
			provider.setVisibilityListener((v, visibility) -> refresh());
		}
	}

	private void refresh() {
		app.runMessageInUIThreadAndCancelPrevious(REFRESH_UI_ID, this::updateButtons, UI_REFRESH_INTERVAL_MILLIS);
	}

	public void addMapButton(@NonNull MapButton button) {
		LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		ButtonPositionSize position = button.getDefaultPositionSize();
		if (position != null) {
			updateButtonParams(params, position);
		}
		addView(button, params);
		mapButtons.add(button);
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
			if (view instanceof MapButton button) {
				updateButtonPosition(button, entry.getValue());
			}
		}
	}

	@NonNull
	private Map<View, ButtonPositionSize> getButtonPositionSizes() {
		Map<View, ButtonPositionSize> positions = collectPositions();
		int width = (int) AndroidUtils.pxToDpF(getContext(), getWidth()) / 8;
		int height = (int) AndroidUtils.pxToDpF(getContext(), getHeight()) / 8;

		ButtonPositionSize.computeNonOverlap(1, width, height, new ArrayList<>(positions.values()));

		return positions;
	}

	@NonNull
	private Map<View, ButtonPositionSize> collectPositions() {
		Map<View, ButtonPositionSize> map = new LinkedHashMap<>();

		for (Map.Entry<View, ButtonPositionSize> entry : widgetPositions.entrySet()) {
			View view = entry.getKey();
			if (view.getVisibility() == VISIBLE) {
				ButtonPositionSize position = updateWidgetPosition(view, entry.getValue());
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
		return map;
	}

	@NonNull
	private ButtonPositionSize createWidgetPosition(@NonNull View view) {
		String name = getViewName(view);
		ButtonPositionSize position = new ButtonPositionSize(name);
		if (view instanceof VerticalWidgetPanel panel) {
			position.top = panel.isTopPanel();
		} else if (view instanceof SideWidgetsPanel panel) {
			position.top = true;
			position.left = !panel.rightSide;
		} else if (view.getId() == R.id.widget_top_bar) {
			position.top = true;
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
		int width = (int) AndroidUtils.pxToDpF(getContext(), view.getWidth()) / 8;
		int height = (int) AndroidUtils.pxToDpF(getContext(), view.getHeight()) / 8;
		position.setSize(width, height);

		if (view instanceof SideWidgetsPanel) {
			int parentWidth = getWidth();
			int parentHeight = getAdjustedHeight();
			int[] margins = AndroidUtils.getRelativeMargins(this, view);
			if (margins[0] >= 0 && margins[1] >= 0 && margins[2] >= 0 && margins[3] >= 0) {
				position.calcGridPositionFromPixel(dpToPx, parentWidth, parentHeight,
						position.left, position.left ? margins[0] : margins[2],
						position.top, position.top ? margins[1] - statusBarHeight : margins[3] - statusBarHeight);
			}
		}
		return position;
	}

	public void updateButtonPosition(@NonNull MapButton button, @NonNull ButtonPositionSize position) {
		LayoutParams params = (LayoutParams) button.getLayoutParams();
		updateButtonParams(params, position);
		button.setLayoutParams(params);
	}

	public void updateButtonParams(@NonNull LayoutParams params, @NonNull ButtonPositionSize position) {
		int gravity;
		int marginX = position.getXStartPix(dpToPx);
		int marginY = position.getYStartPix(dpToPx);

		if (position.left) {
			gravity = Gravity.START;
			params.rightMargin = 0;
			params.leftMargin = marginX;
		} else {
			gravity = Gravity.END;
			params.leftMargin = 0;
			params.rightMargin = marginX;
		}
		if (position.top) {
			gravity |= Gravity.TOP;
			params.bottomMargin = 0;
			params.topMargin = marginY;
		} else {
			gravity |= Gravity.BOTTOM;
			params.topMargin = 0;
			params.bottomMargin = marginY;
		}
		params.gravity = gravity;
	}

	public void updateButton(@NonNull MapButton button, boolean save) {
		MapButtonState buttonState = button.getButtonState();
		ButtonPositionSize positionSize = buttonState != null ? buttonState.getPositionSize() : null;
		if (buttonState != null) {
			int width = getWidth();
			int height = getAdjustedHeight();
			LayoutParams params = (LayoutParams) button.getLayoutParams();

			positionSize.calcGridPositionFromPixel(dpToPx, width, height,
					positionSize.left, positionSize.left ? params.leftMargin : params.rightMargin,
					positionSize.top, positionSize.top ? params.topMargin : params.bottomMargin);
		}
		if (save) {
			button.savePosition();
		}
		updateButtons(); // relayout to avoid overlap
	}

	private int getAdjustedHeight() {
		return getHeight() - statusBarHeight;
	}

	private void updateRulerWidget(@NonNull View widget) {
		MapButtonsHelper helper = app.getMapButtonsHelper();
		ButtonPositionSize drawer = helper.getDrawerMenuButtonState().getPositionSize();
		ButtonPositionSize navigation = helper.getNavigationMenuButtonState().getPositionSize();

		MarginLayoutParams params = (MarginLayoutParams) widget.getLayoutParams();
		params.setMarginStart((int) (drawer.getWidthPix(dpToPx) + navigation.getWidthPix(dpToPx) + DEF_MARGIN_DP * dpToPx));
	}

	public interface VisibilityChangeListener {
		void onVisibilityChanged(@NonNull View view, int visibility);
	}

	public interface SizeChangeListener {
		void onSizeChanged(@NonNull View view, int w, int h, int oldw, int oldh);
	}

	public interface ViewChangeProvider {

		void setSizeListener(@Nullable SizeChangeListener listener);

		void setVisibilityListener(@Nullable VisibilityChangeListener listener);

	}
}
