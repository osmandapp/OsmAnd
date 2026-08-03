package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class MapMarkersBarWidgetSettingFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_DISPLAYED_MARKERS = "displayed_markers";

	private boolean oneMarkerDisplayed;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.MARKERS_TOP_BAR;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		oneMarkerDisplayed = bundle.containsKey(KEY_DISPLAYED_MARKERS)
				? bundle.getBoolean(KEY_DISPLAYED_MARKERS)
				: settings.DISPLAYED_MARKERS_WIDGETS_COUNT.getModeValue(appMode) == 1;
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.map_markers_bar_widget_settings, container);

		LinearLayout radioButtonsView = view.findViewById(R.id.custom_radio_buttons);
		TextToggleButton toggle = new TextToggleButton(app, radioButtonsView, nightMode);
		toggle.setItems(listRadioItems());
		toggle.setSelectedItem(oneMarkerDisplayed ? 0 : 1);
	}

	@NonNull
	private List<TextRadioItem> listRadioItems() {
		TextRadioItem oneMarkerItem = new TextRadioItem(getString(R.string.shared_string_one));
		oneMarkerItem.setOnClickListener((radioItem, view1) -> {
			oneMarkerDisplayed = true;
			return true;
		});

		TextRadioItem twoMarkersItem = new TextRadioItem(getString(R.string.shared_string_two));
		twoMarkersItem.setOnClickListener((radioItem, view1) -> {
			oneMarkerDisplayed = false;
			return true;
		});

		return Arrays.asList(oneMarkerItem, twoMarkersItem);
	}

	@Override
	protected void applySettings() {
		settings.DISPLAYED_MARKERS_WIDGETS_COUNT.setModeValue(appMode, oneMarkerDisplayed ? 1 : 2);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_DISPLAYED_MARKERS, oneMarkerDisplayed);
	}
}