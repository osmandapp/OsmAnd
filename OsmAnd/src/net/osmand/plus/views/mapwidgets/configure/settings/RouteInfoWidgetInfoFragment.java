package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayValue.ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayValue.DISTANCE;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayValue.TIME_TO_GO;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget.formatArrivalTime;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget.formatDistance;
import static net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget.formatDuration;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayValue;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.DisplayPriority;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.alert.SelectionDialogItem;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RouteInfoWidgetInfoFragment extends BaseResizableWidgetSettingFragment {

	private static final String KEY_DEFAULT_VIEW = "default_view";
	private static final String KEY_PRIORITY_VALUE = "display_priority";

	private static final long HOUR_IN_MILLISECONDS = 60 * 60 * 1000;
	private static final long ONE_HUNDRED_KM_IN_METERS = 100_000;

	private RouteInfoWidget widget;

	private DisplayValue selectedDefaultView;
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
			String defaultViewKey = bundle.getString(KEY_DEFAULT_VIEW);
			selectedDefaultView = defaultViewKey != null
					? DisplayValue.valueOf(defaultViewKey)
					: widget.getDefaultView(appMode);
			String displayPriorityKey = bundle.getString(KEY_PRIORITY_VALUE);
			selectedDisplayPriority = displayPriorityKey != null
					? DisplayPriority.valueOf(displayPriorityKey)
					: widget.getDisplayPriority(appMode);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_route_info, container);

		View defaultValueButton = container.findViewById(R.id.default_view_container);
		defaultValueButton.setOnClickListener(v -> showDefaultViewDialog(container));
		defaultValueButton.setBackground(getPressedStateDrawable());
		updateDefaultViewButton(defaultValueButton);

		View displayPriorityButton = container.findViewById(R.id.display_priority_container);
		displayPriorityButton.setOnClickListener(v -> showDisplayPriorityDialog(container));
		displayPriorityButton.setBackground(getPressedStateDrawable());
		updateDisplayPriorityButton(displayPriorityButton);
	}

	private void showDefaultViewDialog(@NonNull View container) {
		Map<DisplayValue, String> previewData = new HashMap<>();
		previewData.put(ARRIVAL_TIME, getFormattedPreviewArrivalTime());
		previewData.put(TIME_TO_GO, formatDuration(app, HOUR_IN_MILLISECONDS));
		previewData.put(DISTANCE, formatDistance(app, ONE_HUNDRED_KM_IN_METERS));

		int selected = 0;
		DisplayValue[] displayValues = DisplayValue.values();
		SelectionDialogItem[] items = new SelectionDialogItem[displayValues.length];
		for (int i = 0; i < displayValues.length; i++) {
			DisplayValue displayValue = displayValues[i];
			CharSequence title = getString(displayValue.getTitleId());
			CharSequence description = getDefaultViewSummary(displayValue, previewData);
			items[i] = new SelectionDialogItem(title, description);
			selected = selectedDefaultView == displayValue ? i : selected;
		}

		AlertDialogData dialogData = new AlertDialogData(container.getContext(), nightMode)
				.setTitle(R.string.shared_string_default_view)
				.setItemsLayoutRes(R.layout.dialog_list_item_with_compound_button_and_summary);

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedDefaultView = displayValues[which];
			updateDefaultViewButton(container);
		});
	}

	private void updateDefaultViewButton(@NonNull View container) {
		ImageView ivIcon = container.findViewById(R.id.default_view_icon);
		ivIcon.setImageResource(selectedDefaultView.getIconId());
		TextView tvDescription = container.findViewById(R.id.default_view_description);
		tvDescription.setText(getString(selectedDefaultView.getTitleId()));
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
	private CharSequence getDefaultViewSummary(@NonNull DisplayValue defaultView,
	                                           @NonNull Map<DisplayValue, String> previewData) {
		String fullText = "";
		String pattern = getString(R.string.ltr_or_rtl_combine_via_bold_point);
		for (DisplayValue displayValue : DisplayValue.values(defaultView)) {
			String value = Objects.requireNonNull(previewData.get(displayValue));
			if (fullText.isEmpty()) {
				fullText = value;
			} else {
				fullText = String.format(pattern, fullText, value);
			}
		}

		SpannableString spannable = new SpannableString(fullText);
		String primaryValue = previewData.get(defaultView);
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
		outState.putString(KEY_DEFAULT_VIEW, selectedDefaultView.name());
		outState.putString(KEY_PRIORITY_VALUE, selectedDisplayPriority.name());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widget.setDefaultView(appMode, selectedDefaultView);
		widget.setDisplayPriority(appMode, selectedDisplayPriority);
	}
}
