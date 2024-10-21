package net.osmand.plus.views.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
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
	}

	public void updateButtons() {
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
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child instanceof MapButton button && button.getVisibility() == VISIBLE) {
				MapButtonState buttonState = button.getButtonState();
				if (buttonState != null) {
					ButtonPositionSize position = buttonState.getPositionSize();
					map.put(position.id, buttonState.getPositionSize());
				}
			}
		}
		// TODO delete log.info
		for (ButtonPositionSize b : map.values()) {
			LOG.info("BTNS " + b.toString());
		}
		ButtonPositionSize.computeNonOverlap(1, new ArrayList<>(map.values()));
		LOG.info("BTNS ----------");
		for (ButtonPositionSize b : map.values()) {
			LOG.info("BTNS " + b.toString());
		}
		return map;
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

		boolean top = (params.gravity & Gravity.TOP) == Gravity.TOP;
		boolean left = (params.gravity & Gravity.START) == Gravity.START;

		LOG.info("params " + position.id + (left ? " left " : " right ")
				+ (top ? "top " : "bott ") + "marginX " + marginX + " marginY " + marginY);
		LOG.info(position);
	}

	public void updateButton(@NonNull MapButton button, boolean save) {
		MapButtonState buttonState = button.getButtonState();
		if (buttonState != null) {
			ButtonPositionSize positionSize = buttonState.getPositionSize();
			int width = getWidth();
			// TODO this height incorrect cause it includes statusbar?
			int height = getHeight();
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
}