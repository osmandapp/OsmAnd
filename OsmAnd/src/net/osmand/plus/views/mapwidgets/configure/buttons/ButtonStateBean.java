package net.osmand.plus.views.mapwidgets.configure.buttons;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

public class ButtonStateBean {

	public String id;
	public String name = "";
	public String icon;
	public int size = -1;
	public int cornerRadius = -1;
	public float opacity = -1;
	public boolean enabled;

	public ButtonStateBean(@NonNull String id) {
		this.id = id;
	}

	public void setupButtonState(@NonNull QuickActionButtonState buttonState) {
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
		return bean;
	}
}
