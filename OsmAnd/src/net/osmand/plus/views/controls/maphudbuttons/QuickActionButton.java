package net.osmand.plus.views.controls.maphudbuttons;

import static android.widget.ImageView.ScaleType.CENTER;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QuickActionButton extends androidx.appcompat.widget.AppCompatImageButton {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final MapQuickActionLayer layer;

	private QuickActionButtonState buttonState;
	private ButtonAppearanceParams appearanceParams;
	private ButtonAppearanceParams customAppearanceParams;

	private boolean nightMode;
	private boolean widgetVisible;

	public QuickActionButton(@NonNull @NotNull Context context) {
		this(context, null);
	}

	public QuickActionButton(@NonNull @NotNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public QuickActionButton(@NonNull @NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		this.app = (OsmandApplication) context.getApplicationContext();
		this.uiUtilities = app.getUIUtilities();
		this.layer = app.getOsmandMap().getMapLayers().getMapQuickActionLayer();
		this.nightMode = app.getDaynightHelper().isNightMode();
	}

	@NonNull
	public String getButtonId() {
		return buttonState.getId();
	}

	@NonNull
	public QuickActionButtonState getButtonState() {
		return buttonState;
	}

	public void setButtonState(@NonNull QuickActionButtonState buttonState) {
		this.buttonState = buttonState;
	}

	public void setCustomAppearanceParams(@Nullable ButtonAppearanceParams customAppearanceParams) {
		this.customAppearanceParams = customAppearanceParams;
	}

	public void update(boolean nightMode, boolean forceUpdate) {
		boolean widgetVisible = layer.isWidgetVisibleForButton(this);
		ButtonAppearanceParams params = customAppearanceParams != null ? customAppearanceParams : buttonState.createAppearanceParams();

		if (this.nightMode != nightMode || this.widgetVisible != widgetVisible
				|| !Algorithms.objectEquals(appearanceParams, params) || forceUpdate) {
			this.nightMode = nightMode;
			this.widgetVisible = widgetVisible;
			this.appearanceParams = params;

			updateIcon();
			setBackground(AppCompatResources.getDrawable(app, nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle_trans));
		}
	}

	private void updateIcon() {
		int iconColor = ColorUtilities.getMapButtonIconColor(getContext(), nightMode);
		if (widgetVisible) {
			setContentDescription(app.getString(R.string.shared_string_cancel));
			OsmandMapLayer.setMapButtonIcon(this, uiUtilities.getPaintedIcon(R.drawable.ic_action_close, iconColor), CENTER);
		} else {
			int iconId = AndroidUtils.getDrawableId(app, appearanceParams.getIconName());
			if (iconId > 0) {
				setContentDescription(app.getString(R.string.configure_screen_quick_action));
				OsmandMapLayer.setMapButtonIcon(this, uiUtilities.getPaintedIcon(iconId, iconColor), CENTER);
			} else if (buttonState.isSingleAction()) {
				List<QuickAction> actions = buttonState.getQuickActions();
				QuickAction action = MapButtonsHelper.produceAction(actions.get(0));

				setContentDescription(action.getActionText(app));
				OsmandMapLayer.setMapButtonIcon(this, buttonState.getIcon(nightMode, true, iconColor), CENTER);
			} else {
				setContentDescription(app.getString(R.string.configure_screen_quick_action));
				OsmandMapLayer.setMapButtonIcon(this, uiUtilities.getPaintedIcon(R.drawable.ic_quick_action, iconColor), CENTER);
			}
		}
	}
}