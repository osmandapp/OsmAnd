package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import java.util.List;

public class QuickActionButton extends MapButton {

	private final MapQuickActionLayer layer;

	private QuickActionButtonState buttonState;

	private boolean widgetVisible;

	public QuickActionButton(@NonNull Context context) {
		this(context, null);
	}

	public QuickActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public QuickActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.layer = app.getOsmandMap().getMapLayers().getMapQuickActionLayer();

		setOnClickListener(v -> {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			if (!buttonState.isDefaultButton() && buttonState.isSingleAction()) {
				List<QuickAction> actions = buttonState.getQuickActions();
				layer.onActionSelected(buttonState, actions.get(0));
			} else if (!showTutorialIfNeeded()) {
				boolean visible = layer.isWidgetVisibleForButton(this);
				layer.setSelectedButton(visible ? null : this);
			}
			layer.invalidateRelatedButtons(QuickActionButton.this);
		});
	}

	@Nullable
	@Override
	public QuickActionButtonState getButtonState() {
		return buttonState;
	}

	public void setButtonState(@NonNull QuickActionButtonState buttonState) {
		this.buttonState = buttonState;
	}

	public void setWidgetVisible(boolean widgetVisible) {
		if (this.widgetVisible != widgetVisible) {
			this.widgetVisible = widgetVisible;
			this.invalidated = true;
		}
	}

	@Override
	public void update() {
		setWidgetVisible(layer.isWidgetVisibleForButton(this));
		super.update();

		if (widgetVisible) {
			setContentDescription(app.getString(R.string.shared_string_cancel));
		} else if (buttonState.isSingleAction()) {
			List<QuickAction> actions = buttonState.getQuickActions();
			setContentDescription(actions.get(0).getActionText(app));
		} else {
			setContentDescription(app.getString(R.string.configure_screen_quick_action));
		}
	}

	@Override
	protected boolean shouldShow() {
		return visibilityHelper.shouldShowQuickActionButton();
	}

	private boolean showTutorialIfNeeded() {
		if (mapActivity != null && layer.isLayerOn() && !app.accessibilityEnabled() && !settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.get()) {
			TapTarget tapTarget = TapTarget.forView(this, app.getString(R.string.quick_action_btn_tutorial_title), app.getString(R.string.quick_action_btn_tutorial_descr))
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