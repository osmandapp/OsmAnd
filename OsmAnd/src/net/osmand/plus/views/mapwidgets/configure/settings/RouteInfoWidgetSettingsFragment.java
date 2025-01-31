package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode.ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode.DISTANCE;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode.TIME_TO_GO;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget.formatArrivalTime;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget.formatDistance;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget.formatDuration;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.alert.SelectionDialogItem;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RouteInfoWidgetSettingsFragment extends BaseResizableWidgetSettingFragment {

	private static final String KEY_PRIMARY_VALUE = "primary_display_value";

	private static final long HOUR_IN_MILLISECONDS = 60 * 60 * 1000;
	private static final long ONE_HUNDRED_KM_IN_METERS = 100_000;

	private RouteInfoWidget widget;

	private RouteInfoDisplayMode selectedDisplayMode;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.ROUTE_INFO;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			widget = ((RouteInfoWidget) widgetInfo.widget);
			String displayModeKey = bundle.getString(KEY_PRIMARY_VALUE);
			selectedDisplayMode = displayModeKey != null
					? RouteInfoDisplayMode.valueOf(displayModeKey)
					: widget.getDisplayMode(appMode);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.fragment_widget_settings_route_info, container);

		View defViewContainer = container.findViewById(R.id.default_view_container);
		defViewContainer.setOnClickListener(v -> showDefaultValueDialog(container));
		defViewContainer.setBackground(getPressedStateDrawable());
		TextView defViewDesc = container.findViewById(R.id.default_view_description);
		defViewDesc.setText(getString(selectedDisplayMode.getTitleId()));

		super.setupContent(themedInflater, container);
	}

	private void showDefaultValueDialog(@NonNull View container) {
		Map<RouteInfoDisplayMode, String> previewData = new HashMap<>();
		previewData.put(ARRIVAL_TIME, getFormattedPreviewArrivalTime());
		previewData.put(TIME_TO_GO, formatDuration(app, HOUR_IN_MILLISECONDS));
		previewData.put(DISTANCE, formatDistance(app, ONE_HUNDRED_KM_IN_METERS));

		int selected = 0;
		RouteInfoDisplayMode[] displayModes = RouteInfoDisplayMode.values();
		SelectionDialogItem[] items = new SelectionDialogItem[displayModes.length];
		for (int i = 0; i < displayModes.length; i++) {
			RouteInfoDisplayMode displayMode = displayModes[i];
			CharSequence title = getString(displayMode.getTitleId());
			CharSequence description = getDisplayModeSummary(displayMode, previewData);
			items[i] = new SelectionDialogItem(title, description);
			selected = selectedDisplayMode == displayMode ? i : selected;
		}

		AlertDialogData dialogData = new AlertDialogData(container.getContext(), nightMode)
				.setTitle(R.string.shared_string_default_view)
				.setItemsLayoutRes(R.layout.dialog_list_item_with_compound_button_and_summary);

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedDisplayMode = displayModes[which];
			TextView defValueDesc = container.findViewById(R.id.default_view_description);
			defValueDesc.setText(getString(selectedDisplayMode.getTitleId()));
		});
	}

	@NonNull
	private CharSequence getDisplayModeSummary(@NonNull RouteInfoDisplayMode displayMode,
	                                           @NonNull Map<RouteInfoDisplayMode, String> previewData) {
		String fullText = "";
		String pattern = getString(R.string.ltr_or_rtl_combine_via_bold_point);
		for (RouteInfoDisplayMode mode : RouteInfoDisplayMode.values(displayMode)) {
			String value = Objects.requireNonNull(previewData.get(mode));
			if (fullText.isEmpty()) {
				fullText = value;
			} else {
				fullText = String.format(pattern, fullText, value);
			}
		}

		SpannableString spannable = new SpannableString(fullText);
		String primaryValue = previewData.get(displayMode);
		if (primaryValue != null) {
			int startIndex = 0;
			int endIndex = primaryValue.length();
			spannable.setSpan(new StyleSpan(Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return spannable;
	}

	@NonNull
	private String getFormattedPreviewArrivalTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 13);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return formatArrivalTime(app, calendar.getTimeInMillis());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_PRIMARY_VALUE, selectedDisplayMode.name());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widget.setDisplayMode(appMode, selectedDisplayMode);
	}

}
