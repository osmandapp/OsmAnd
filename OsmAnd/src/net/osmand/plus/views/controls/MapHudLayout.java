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

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapHudLayout extends FrameLayout {

	private static final Log LOG = PlatformUtil.getLog(MapHudLayout.class);

<<<<<<< HEAD
	private final Paint gridPaint;
=======
>>>>>>> b9b2466d6e9e509d19d19c5934a97a63f2cd5dd3
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
<<<<<<< HEAD
		this.gridPaint = new Paint();
		gridPaint.setColor(Color.BLACK);
		gridPaint.setStrokeWidth(1f);
		setWillNotDraw(false);
=======
>>>>>>> b9b2466d6e9e509d19d19c5934a97a63f2cd5dd3
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
				ButtonPositionSize positionSize = button.getPositionSize();
				if (positionSize != null) {
					map.put(positionSize.id, positionSize);
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

	public void updateButtonPosition(@NonNull MapButton button, @NonNull ButtonPositionSize positionSize) {
		int gravity = 0;
		int marginX = positionSize.getXStartPix(dpToPx);
		int marginY = positionSize.getYStartPix(dpToPx);
		LayoutParams params = (LayoutParams) button.getLayoutParams();

		if (positionSize.left) {
			gravity |= Gravity.START;
			params.rightMargin = 0;
			params.leftMargin = marginX;
		} else {
			gravity |= Gravity.END;
			params.leftMargin = 0;
			params.rightMargin = marginX;
		}
		if (positionSize.top) {
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

		LOG.info("params " + button.getButtonId() + (left ? " left " : " right ")
				+ (top ? "top " : "bott ") + "marginX " + marginX + "marginY " + marginY);
		LOG.info(positionSize);
	}

	public void updateButton(@NonNull MapButton button, boolean save) {
		ButtonPositionSize positionSize = button.getPositionSize();
		if (positionSize != null) {
			int width = getWidth();
			int height = getHeight();
			LayoutParams params = (LayoutParams) button.getLayoutParams();

			positionSize.calcGridPositionFromPixel(dpToPx, width, height,
					positionSize.left, params.rightMargin, positionSize.top, params.bottomMargin);
		}
		updateButtons(); // relayout to avoid overlap

		if (save) {
			button.saveMargins();
		}
	}
}