package net.osmand.plus.views.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapHudLayout extends FrameLayout {

	private static final Log LOG = PlatformUtil.getLog(MapHudLayout.class);

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
		this.dpToPx = AndroidUtils.dpToPxF(getContext(), 1);
		this.statusBarHeight = AndroidUtils.getStatusBarHeight(context);
	}

	public void updateButtons() {
		if (getWidth() <= 0 && getHeight() <= 0) {
			return;
		}
		Map<String, ButtonPositionSize> map = getButtonPositionSizes();
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child instanceof MapButton button && button.getVisibility() == VISIBLE) {
				String id = button.getButtonId();
				ButtonPositionSize positionSize = map.get(id);
				if (positionSize != null) {
					updateButtonPosition(button, positionSize);
				}
			}
		}
	}

	@NonNull
	private Map<String, ButtonPositionSize> getButtonPositionSizes() {
		Map<String, ButtonPositionSize> map = new LinkedHashMap<>();
		collectButtonPositions(this, map);

		int width = (int) AndroidUtils.pxToDpF(getContext(), getWidth()) / 8;
		int height = (int) AndroidUtils.pxToDpF(getContext(), getHeight()) / 8;
		ButtonPositionSize.computeNonOverlap(1, width, height, new ArrayList<>(map.values()));

		return map;
	}

	private void collectButtonPositions(@NonNull ViewGroup parent, @NonNull Map<String, ButtonPositionSize> map) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			if (child.getVisibility() == VISIBLE) {
				if (child instanceof MapButton || child instanceof SideWidgetsPanel || child instanceof VerticalWidgetPanel) {
					ButtonPositionSize position = getButtonPositionSize(child);
					if (position != null) {
						map.put(position.id, position);
					}
				} else if (child instanceof ViewGroup) {
					collectButtonPositions((ViewGroup) child, map);
				}
			}
		}
	}

	@Nullable
	private ButtonPositionSize getButtonPositionSize(@NonNull View view) {
		if (view instanceof MapButton button) {
			MapButtonState buttonState = button.getButtonState();
			return buttonState != null ? buttonState.getPositionSize() : null;
		} else {
			int width = (int) AndroidUtils.pxToDpF(getContext(), view.getWidth()) / 8;
			int height = (int) AndroidUtils.pxToDpF(getContext(), view.getHeight()) / 8;

			ButtonPositionSize position = new ButtonPositionSize(getResources().getResourceEntryName(view.getId()));
			position.setSize(width, height);

			if (view instanceof VerticalWidgetPanel panel) {
				position.top = panel.isTopPanel();
			} else if (view instanceof SideWidgetsPanel panel) {
				position.top = true;
				position.left = !panel.rightSide;
			}
			return position;
		}
	}

	public void updateButtonPosition(@NonNull MapButton button, @NonNull ButtonPositionSize position) {
		LayoutParams params = (LayoutParams) button.getLayoutParams();
		updateButtonParams(params, position);
		button.setLayoutParams(params);
	}

	public void updateButtonParams(@NonNull LayoutParams params, @NonNull ButtonPositionSize position) {
		int gravity = 0;
		int marginX = position.getXStartPix(dpToPx);
		int marginY = position.getYStartPix(dpToPx);

		if (position.left) {
			gravity |= Gravity.START;
			params.rightMargin = 0;
			params.leftMargin = marginX;
		} else {
			gravity |= Gravity.END;
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
			int height = getHeight(); // TODO this height incorrect cause it includes statusbar?
			LayoutParams params = (LayoutParams) button.getLayoutParams();

			positionSize.calcGridPositionFromPixel(dpToPx, width, height,
					positionSize.left, positionSize.left ? params.leftMargin : params.rightMargin,
					positionSize.top, positionSize.top ? params.topMargin : params.bottomMargin);
		}
		updateButtons(); // relayout to avoid overlap

		if (save) {
			button.savePosition();
		}
	}
}