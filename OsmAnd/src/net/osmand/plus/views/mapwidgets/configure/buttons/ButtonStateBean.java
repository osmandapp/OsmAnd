package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.ORIGINAL_VALUE;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ButtonStateBean {

	public String id;
	public String name = null;
	public String icon;
	public int size = ORIGINAL_VALUE;
	public int cornerRadius = ORIGINAL_VALUE;
	public float opacity = ORIGINAL_VALUE;
	public boolean enabled;
	public List<QuickAction> quickActions = new ArrayList<>();

	public ButtonStateBean(@NonNull String id) {
		this.id = id;
	}

	@NonNull
	public String getName(@NonNull Context app) {
		return Algorithms.isEmpty(name) ? app.getString(R.string.shared_string_quick_actions) : name;
	}

	@DrawableRes
	public int getIconId(@NonNull Context app) {
		if (Algorithms.isEmpty(icon) && quickActions.size() == 1) {
			int iconId = quickActions.get(0).getIconRes(app);
			if (iconId > 0) {
				return iconId;
			}
		}
		return AndroidUtils.getDrawableId(app, icon, R.drawable.ic_quick_action);
	}

	public void setupButtonState(@NonNull OsmandApplication app, @NonNull QuickActionButtonState buttonState) {
		app.getSettings().executePreservingPrefTimestamp(() -> {
			buttonState.setName(name);
			buttonState.setEnabled(enabled);

			if (!Algorithms.isEmpty(icon)) {
				buttonState.getIconPref().set(icon);
			}
			if (size > 0) {
				buttonState.getSizePref().set(size);
			}
			if (cornerRadius >= 0) {
				buttonState.getCornerRadiusPref().set(cornerRadius);
			}
			if (opacity >= 0) {
				buttonState.getOpacityPref().set(opacity);
			}
			MapButtonsHelper buttonsHelper = app.getMapButtonsHelper();
			buttonsHelper.updateQuickActions(buttonState, quickActions);
			buttonsHelper.updateActiveActions();
		});
	}

	@NonNull
	public static ButtonStateBean toStateBean(@NonNull QuickActionButtonState state) {
		ButtonStateBean bean = new ButtonStateBean(state.getId());
		bean.enabled = state.isEnabled();
		if (state.hasCustomName()) {
			bean.name = state.getName();
		}
		if (state.getIconPref().isSet()) {
			bean.icon = state.getIconPref().get();
		}
		if (state.getSizePref().isSet()) {
			bean.size = state.getSizePref().get();
		}
		if (state.getCornerRadiusPref().isSet()) {
			bean.cornerRadius = state.getCornerRadiusPref().get();
		}
		if (state.getOpacityPref().isSet()) {
			bean.opacity = state.getOpacityPref().get();
		}
		bean.quickActions = state.getQuickActions();
		return bean;
	}
}
