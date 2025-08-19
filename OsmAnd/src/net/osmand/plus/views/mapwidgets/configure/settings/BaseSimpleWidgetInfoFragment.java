package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class BaseSimpleWidgetInfoFragment extends BaseResizableWidgetSettingFragment {
	private static final String SHOW_ICON_KEY = "show_icon_key";

	public CommonPreference<Boolean> shouldShowIconPref;

	private boolean showIcon;
	private View showIconContainer;

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof SimpleWidget simpleWidget) {
			shouldShowIconPref = simpleWidget.shouldShowIconPref();
			showIcon = bundle.containsKey(SHOW_ICON_KEY) ? bundle.getBoolean(SHOW_ICON_KEY) : shouldShowIconPref.get();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_ICON_KEY, showIcon);
	}

	@Override
	protected void setupTopContent(@NonNull ViewGroup container) {
		super.setupTopContent(container);
		inflate(R.layout.simple_widget_settings, container);

		SwitchCompat switchCompat = container.findViewById(R.id.show_icon_toggle);
		switchCompat.setChecked(showIcon);
		showIconContainer = container.findViewById(R.id.show_icon_container);
		showIconContainer.setOnClickListener(v -> updateShowIcon(!showIcon, switchCompat));
		showIconContainer.setBackground(getPressedStateDrawable());
		updateShowIconContainerVisibility();
	}

	private void updateShowIconContainerVisibility() {
		AndroidUiHelper.updateVisibility(showIconContainer, !isSmallHeight() || isVerticalPanel);
	}

	@Override
	protected void onWidgetSizeChanged() {
		super.onWidgetSizeChanged();
		updateShowIconContainerVisibility();
	}

	private void updateShowIcon(boolean shouldShowIcon, SwitchCompat switchCompat) {
		switchCompat.setChecked(shouldShowIcon);
		showIcon = shouldShowIcon;
	}

	@Override
	protected void applySettings() {
		shouldShowIconPref.set(showIcon);
		if (widgetInfo != null) {
			if (widgetInfo.widget instanceof SimpleWidget simpleWidget) {
				simpleWidget.updateWidgetView();
			}
		}
		super.applySettings();
	}
}
