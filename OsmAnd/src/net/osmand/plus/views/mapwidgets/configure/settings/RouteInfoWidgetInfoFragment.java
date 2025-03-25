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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayPriority;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoDisplayMode;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.alert.SelectionDialogItem;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RouteInfoWidgetInfoFragment extends BaseResizableWidgetSettingFragment {

	private static final String KEY_PRIMARY_VALUE = "primary_display_value";
	private static final String KEY_PRIORITY_VALUE = "primary_display_priority";

	private static final long HOUR_IN_MILLISECONDS = 60 * 60 * 1000;
	private static final long ONE_HUNDRED_KM_IN_METERS = 100_000;

	private RouteInfoWidget widget;

	private RouteInfoDisplayMode selectedDisplayMode;
	private DisplayPriority selectedDisplayPriority;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.ROUTE_INFO;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			widget = ((RouteInfoWidget) widgetInfo.widget);
			String displayModeKey = bundle.getString(KEY_PRIMARY_VALUE);
			selectedDisplayMode = displayModeKey != null
					? RouteInfoDisplayMode.valueOf(displayModeKey)
					: widget.getDisplayMode(appMode);
			String displayPriorityKey = bundle.getString(KEY_PRIORITY_VALUE);
			selectedDisplayPriority = displayPriorityKey != null
					? DisplayPriority.valueOf(displayPriorityKey)
					: widget.getDisplayPriority(appMode);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.fragment_widget_settings_route_info, container);

		View defaultValueButton = container.findViewById(R.id.default_view_container);
		defaultValueButton.setOnClickListener(v -> showDefaultValueDialog(container));
		defaultValueButton.setBackground(getPressedStateDrawable());
		updateDefaultValueButton(defaultValueButton);

		View displayPriorityButton = container.findViewById(R.id.display_priority_container);
		displayPriorityButton.setOnClickListener(v -> showDisplayPriorityDialog(container));
		displayPriorityButton.setBackground(getPressedStateDrawable());
		updateDisplayPriorityButton(displayPriorityButton);
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
			updateDefaultValueButton(container);
		});
	}

	private void updateDefaultValueButton(@NonNull View container) {
		ImageView ivIcon = container.findViewById(R.id.default_view_icon);
		ivIcon.setImageResource(selectedDisplayMode.getIconId());
		TextView tvDescription = container.findViewById(R.id.default_view_description);
		tvDescription.setText(getString(selectedDisplayMode.getTitleId()));
	}

	private void showDisplayPriorityDialog(@NonNull View container) {
		DisplayPriority[] priorities = DisplayPriority.values();
		SelectionDialogItem[] items = new SelectionDialogItem[priorities.length];
		int selected = 0;
		for (int i = 0; i < priorities.length; i++) {
			DisplayPriority priority = priorities[i];
			CharSequence title = getString(priority.getTitleId());
			items[i] = new SelectionDialogItem(title, "");
			selected = selectedDisplayPriority == priority ? i : selected;
		}

		AlertDialogData dialogData = new AlertDialogData(container.getContext(), nightMode)
				.setTitle(R.string.display_priority)
				.setItemsLayoutRes(R.layout.dialog_list_item_with_compound_button);

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedDisplayPriority = priorities[which];
			updateDisplayPriorityButton(container);
		});
	}

	private void updateDisplayPriorityButton(@NonNull View container) {
		ImageView ivIcon = container.findViewById(R.id.display_priority_icon);
		ivIcon.setImageResource(selectedDisplayPriority.getIconId());
		TextView tvDescription = container.findViewById(R.id.display_priority_description);
		tvDescription.setText(getString(selectedDisplayPriority.getTitleId()));
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
		outState.putString(KEY_PRIORITY_VALUE, selectedDisplayPriority.name());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widget.setDisplayMode(appMode, selectedDisplayMode);
		widget.setDisplayPriority(appMode, selectedDisplayPriority);
	}
}
