package net.osmand.plus.settings.backend.preferences;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

public class FabMarginPreference {
	private static final String X_PORTRAIT_MARGIN = "_x_portrait_margin";
	private static final String Y_PORTRAIT_MARGIN = "_y_portrait_margin";
	private static final String X_LANDSCAPE_MARGIN = "_x_landscape_margin";
	private static final String Y_LANDSCAPE_MARGIN = "_y_landscape_margin";

	private final CommonPreference<Integer> FAB_MARGIN_X_PORTRAIT;
	private final CommonPreference<Integer> FAB_MARGIN_Y_PORTRAIT;
	private final CommonPreference<Integer> FAB_MARGIN_X_LANDSCAPE;
	private final CommonPreference<Integer> FAB_MARGIN_Y_LANDSCAPE;

	public FabMarginPreference(OsmandSettings settings, String prefix) {
		FAB_MARGIN_X_PORTRAIT = new IntPreference(settings, prefix + X_PORTRAIT_MARGIN, 0).makeProfile();
		FAB_MARGIN_Y_PORTRAIT = new IntPreference(settings, prefix + Y_PORTRAIT_MARGIN, 0).makeProfile();
		FAB_MARGIN_X_LANDSCAPE = new IntPreference(settings, prefix + X_LANDSCAPE_MARGIN, 0).makeProfile();
		FAB_MARGIN_Y_LANDSCAPE = new IntPreference(settings, prefix + Y_LANDSCAPE_MARGIN, 0).makeProfile();
	}

	public void setPortraitFabMargin(int x, int y) {
		FAB_MARGIN_X_PORTRAIT.set(x);
		FAB_MARGIN_Y_PORTRAIT.set(y);
	}

	public void setLandscapeFabMargin(int x, int y) {
		FAB_MARGIN_X_LANDSCAPE.set(x);
		FAB_MARGIN_Y_LANDSCAPE.set(y);
	}

	@Nullable
	public Pair<Integer, Integer> getPortraitFabMargin() {
		return new Pair<>(FAB_MARGIN_X_PORTRAIT.get(), FAB_MARGIN_Y_PORTRAIT.get());
	}

	@Nullable
	public Pair<Integer, Integer> getLandscapeFabMargin() {
		return new Pair<>(FAB_MARGIN_X_LANDSCAPE.get(), FAB_MARGIN_Y_LANDSCAPE.get());

	}

	public static void setFabButtonMargin(@Nullable MapActivity mapActivity, @NonNull ImageView fabButton, FrameLayout.LayoutParams params,
	                                      Pair<Integer, Integer> fabMargin,
	                                      int defRightMargin, int defBottomMargin) {
		if (mapActivity == null) {
			return;
		}
		int screenHeight = AndroidUtils.getScreenHeight(mapActivity);
		int screenWidth = AndroidUtils.getScreenWidth(mapActivity);
		int btnHeight = fabButton.getHeight();
		int btnWidth = fabButton.getWidth();
		int maxRightMargin = screenWidth - btnWidth;
		int maxBottomMargin = screenHeight - btnHeight;

		int rightMargin = fabMargin != null ? fabMargin.first : defRightMargin;
		int bottomMargin = fabMargin != null ? fabMargin.second : defBottomMargin;
		// check limits
		if (rightMargin <= 0) {
			rightMargin = defRightMargin;
		} else if (rightMargin > maxRightMargin) {
			rightMargin = maxRightMargin;
		}
		if (bottomMargin <= 0) {
			bottomMargin = defBottomMargin;
		} else if (bottomMargin > maxBottomMargin) {
			bottomMargin = maxBottomMargin;
		}

		params.rightMargin = rightMargin;
		params.bottomMargin = bottomMargin;
		fabButton.setLayoutParams(params);
	}

	public View.OnTouchListener getMoveFabOnTouchListener(@NonNull OsmandApplication app, @Nullable MapActivity mapActivity, @NonNull ImageView fabButton) {
		return new View.OnTouchListener() {
			private int initialMarginX = 0;
			private int initialMarginY = 0;
			private float initialTouchX = 0;
			private float initialTouchY = 0;

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mapActivity == null) {
					return false;
				}
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						setUpInitialValues(v, event);
						return true;
					case MotionEvent.ACTION_UP:
						fabButton.setOnTouchListener(null);
						fabButton.setPressed(false);
						fabButton.setScaleX(1);
						fabButton.setScaleY(1);
						fabButton.setAlpha(1f);
						FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
						if (AndroidUiHelper.isOrientationPortrait(mapActivity))
							setPortraitFabMargin(params.rightMargin, params.bottomMargin);
						else
							setLandscapeFabMargin(params.rightMargin, params.bottomMargin);
						return true;
					case MotionEvent.ACTION_MOVE:
						if (initialMarginX == 0 && initialMarginY == 0 && initialTouchX == 0 && initialTouchY == 0)
							setUpInitialValues(v, event);

						int padding = calculateTotalSizePx(app, R.dimen.map_button_margin);
						FrameLayout parent = (FrameLayout) v.getParent();
						FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) v.getLayoutParams();

						int deltaX = (int) (initialTouchX - event.getRawX());
						int deltaY = (int) (initialTouchY - event.getRawY());

						int newMarginX = interpolate(initialMarginX + deltaX, v.getWidth(), parent.getWidth() - padding * 2);
						int newMarginY = interpolate(initialMarginY + deltaY, v.getHeight(), parent.getHeight() - padding * 2);

						if (v.getHeight() + newMarginY <= parent.getHeight() - padding * 2 && newMarginY > 0)
							param.bottomMargin = newMarginY;

						if (v.getWidth() + newMarginX <= parent.getWidth() - padding * 2 && newMarginX > 0) {
							param.rightMargin = newMarginX;
						}

						v.setLayoutParams(param);

						return true;
				}
				return false;
			}

			private int interpolate(int value, int divider, int boundsSize) {
				if (value <= divider && value > 0)
					return value * value / divider;
				else {
					int leftMargin = boundsSize - value - divider;
					if (leftMargin <= divider && value < boundsSize - divider)
						return leftMargin - (leftMargin * leftMargin / divider) + value;
					else
						return value;
				}
			}

			private void setUpInitialValues(View v, MotionEvent event) {
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();

				initialMarginX = params.rightMargin;
				initialMarginY = params.bottomMargin;

				initialTouchX = event.getRawX();
				initialTouchY = event.getRawY();
			}
		};
	}

	public static int calculateTotalSizePx(OsmandApplication app, @DimenRes int... dimensId) {
		int result = 0;
		for (int id : dimensId) {
			result += app.getResources().getDimensionPixelSize(id);
		}
		return result;
	}
}
