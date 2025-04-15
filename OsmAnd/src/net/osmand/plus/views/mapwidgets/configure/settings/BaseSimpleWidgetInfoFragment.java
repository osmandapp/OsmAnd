package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class BaseSimpleWidgetInfoFragment extends BaseResizableWidgetSettingFragment {
	private static final String SHOW_ICON_KEY = "show_icon_key";

	public CommonPreference<Boolean> shouldShowIconPref;

	private boolean showIcon;

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
	protected void setupTopContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		super.setupTopContent(themedInflater, container);
		if (isVerticalPanel) {
			themedInflater.inflate(R.layout.simple_widget_settings, container);

			SwitchCompat switchCompat = container.findViewById(R.id.show_icon_toggle);
			switchCompat.setChecked(showIcon);
			View shoIconContainer = container.findViewById(R.id.show_icon_container);
			shoIconContainer.setOnClickListener(v -> updateShowIcon(!showIcon, switchCompat));
			shoIconContainer.setBackground(getPressedStateDrawable());
		}
	}

	private void updateShowIcon(boolean shouldShowIcon, SwitchCompat switchCompat) {
		switchCompat.setChecked(shouldShowIcon);
		showIcon = shouldShowIcon;
	}

	@Override
	protected void applySettings() {
		if (isVerticalPanel) {
			shouldShowIconPref.set(showIcon);
			if (widgetInfo != null) {
				if (widgetInfo.widget instanceof SimpleWidget simpleWidget) {
					simpleWidget.showIcon(shouldShowIconPref.get());
					//simpleWidget.recreateView();
				}
			}
		}
		super.applySettings();
	}
}
