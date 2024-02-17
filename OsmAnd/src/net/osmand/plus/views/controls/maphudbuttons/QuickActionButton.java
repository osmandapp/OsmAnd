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
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import java.util.List;

public class QuickActionButton extends MapButton {

	private final MapQuickActionLayer layer;
	private final QuickActionButtonState buttonState;

	public QuickActionButton(@NonNull MapActivity activity, @NonNull ImageView view,
	                         @NonNull QuickActionButtonState buttonState) {
		super(activity, view, buttonState.getId(), false);
		this.buttonState = buttonState;
		this.layer = app.getOsmandMap().getMapLayers().getMapQuickActionLayer();

		setOnClickListener(getOnCLickListener());
		setOnLongClickListener(getLongClickListener());

		setBackground(R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		update(app.getDaynightHelper().isNightMode(), isRouteDialogOpened(), isShowBottomButtons());
		setQuickActionButtonMargin();
	}

	@NonNull
	public QuickActionButtonState getButtonState() {
		return buttonState;
	}

	@Override
	public void update(boolean nightMode, boolean routeDialogOpened, boolean bottomButtonsAllowed) {
		super.update(nightMode, routeDialogOpened, bottomButtonsAllowed);

		boolean visible = layer.isWidgetVisibleForButton(this);
		if (visible) {
			setIconId(R.drawable.ic_action_close);
			setContentDesc(R.string.shared_string_cancel);
		} else if (buttonState.isSingleAction()) {
			setupSingleAction(nightMode);
		} else {
			setIconId(R.drawable.ic_quick_action);
			setContentDesc(R.string.configure_screen_quick_action);
		}
	}

	private void setupSingleAction(boolean nightMode) {
		List<QuickAction> actions = buttonState.getQuickActions();
		QuickAction action = MapButtonsHelper.produceAction(actions.get(0));

		setContentDesc(action.getActionText(app));
		setDrawable(buttonState.getIcon(nightMode, true, ColorUtilities.getColor(app, getIconColorId())));
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
			if (buttonState.isSingleAction()) {
				List<QuickAction> actions = buttonState.getQuickActions();
				layer.onActionSelected(buttonState, actions.get(0));
			} else if (!showTutorialIfNeeded()) {
				boolean visible = layer.isWidgetVisibleForButton(this);
				layer.setSelectedButton(visible ? null : this);
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
			view.setOnTouchListener(getMoveFabOnTouchListener(app, mapActivity, view, buttonState.getFabMarginPref()));
			return true;
		};
	}

	public void setQuickActionButtonMargin() {
		if (mapActivity != null) {
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
			if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
				Pair<Integer, Integer> fabMargin = buttonState.getFabMarginPref().getPortraitFabMargin();
				int defBottomMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing) * 2;
				setFabButtonMargin(mapActivity, view, params, fabMargin, 0, defBottomMargin);
			} else {
				Pair<Integer, Integer> fabMargin = buttonState.getFabMarginPref().getLandscapeFabMargin();
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