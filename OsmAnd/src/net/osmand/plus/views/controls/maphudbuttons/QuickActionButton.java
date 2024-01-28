package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.settings.backend.preferences.FabMarginPreference.setFabButtonMargin;
import static net.osmand.plus.utils.AndroidUtils.calculateTotalSizePx;
import static net.osmand.plus.utils.AndroidUtils.getMoveFabOnTouchListener;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.layers.MapQuickActionLayer;

public class QuickActionButton extends MapButton {

	private final MapQuickActionLayer layer;

	public QuickActionButton(@NonNull MapActivity mapActivity, @NonNull ImageView fabButton, @NonNull String id) {
		super(mapActivity, fabButton, id, false);

		layer = mapActivity.getMapLayers().getMapQuickActionLayer();

		setOnClickListener(getOnCLickListener());
		setOnLongClickListener(getLongClickListener());

		updateButton(layer.getCurrentWidgetState());
		setBackground(R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setQuickActionButtonMargin();
	}

	@Override
	protected void updateState(boolean nightMode) {
		updateButton(layer.getCurrentWidgetState());
	}

	public void updateButton(boolean widgetVisible) {
		setIconId(!widgetVisible ? R.drawable.ic_quick_action : R.drawable.ic_action_close);
		setContentDesc(!widgetVisible ? R.string.configure_screen_quick_action : R.string.shared_string_cancel);
	}

	@Override
	public void refresh() {
		updateVisibility(shouldShow());
		setQuickActionButtonMargin();
	}

	@Override
	protected boolean shouldShow() {
		return mapActivity.getWidgetsVisibilityHelper().shouldShowQuickActionButton();
	}

	@NonNull
	private View.OnClickListener getOnCLickListener() {
		return v -> {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			if (!showTutorialIfNeeded()) {
				layer.updateWidgetVisibility(!layer.isWidgetVisible());
			}
		};
	}

	@NonNull
	private View.OnLongClickListener getLongClickListener() {
		return v -> {
			Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_SHORT);
			view.setScaleX(1.5f);
			view.setScaleY(1.5f);
			view.setAlpha(0.95f);
			view.setOnTouchListener(getMoveFabOnTouchListener(app, mapActivity, view, settings.QUICK_ACTION_FAB_MARGIN));
			return true;
		};
	}

	public void setQuickActionButtonMargin() {
		if (mapActivity != null) {
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
			if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
				Pair<Integer, Integer> fabMargin = settings.QUICK_ACTION_FAB_MARGIN.getPortraitFabMargin();
				int defBottomMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing) * 2;
				setFabButtonMargin(mapActivity, view, params, fabMargin, 0, defBottomMargin);
			} else {
				Pair<Integer, Integer> fabMargin = settings.QUICK_ACTION_FAB_MARGIN.getLandscapeFabMargin();
				int defRightMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing_land) * 2;
				setFabButtonMargin(mapActivity, view, params, fabMargin, defRightMargin, 0);
			}
		}
	}

	private boolean showTutorialIfNeeded() {
		if (mapActivity != null && layer.isLayerOn()
				&& !app.accessibilityEnabled() && !settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.get()) {
			TapTarget tapTarget = TapTarget.forView(view,
							app.getString(R.string.quick_action_btn_tutorial_title),
							app.getString(R.string.quick_action_btn_tutorial_descr))
					// All options below are optional
					.outerCircleColor(R.color.osmand_orange)
					.targetCircleColor(R.color.card_and_list_background_light)
					.titleTextSize(20).descriptionTextSize(16)
					.descriptionTextColor(R.color.card_and_list_background_light)
					.titleTextColor(R.color.card_and_list_background_light)
					.drawShadow(true)
					.cancelable(false)
					.tintTarget(false)
					.transparentTarget(false)
					.targetRadius(50);
			TapTargetView.showFor(mapActivity, tapTarget, new TapTargetView.Listener() {
				@Override
				public void onTargetClick(TapTargetView view) {
					super.onTargetClick(view);
					settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.set(true);
				}
			});
			return true;
		}
		return false;
	}
}