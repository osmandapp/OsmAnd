package net.osmand.plus.plugins.streetimagery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

public class StreetImageryFirstDialogFragment extends BottomSheetDialogFragment {

	private static final String TAG = StreetImageryFirstDialogFragment.class.getSimpleName();

	private static final String KEY_SOURCE = "key_source";
	private static final String KEY_SHOW_WIDGET = "key_show_widget";

	private StreetImagerySource source;
	private boolean showWidget;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		source = readSource();
		MapActivity activity = getMapActivity();
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SHOW_WIDGET)) {
			showWidget = savedInstanceState.getBoolean(KEY_SHOW_WIDGET);
		} else {
			showWidget = activity != null && isWidgetVisible(activity);
		}

		View view = inflate(R.layout.street_imagery_first_dialog, container, false);
		applySourceContent(view);

		SwitchCompat widgetSwitch = view.findViewById(R.id.widget_switch);
		widgetSwitch.setChecked(showWidget);
		widgetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> showWidget = isChecked);
		view.findViewById(R.id.actionButton).setOnClickListener(v -> {
			applyWidgetVisibility();
			dismiss();
		});
		return view;
	}

	@NonNull
	private StreetImagerySource readSource() {
		Bundle args = getArguments();
		String name = args != null ? args.getString(KEY_SOURCE) : null;
		if (name == null) {
			throw new IllegalStateException("Missing required argument " + KEY_SOURCE);
		}
		return StreetImagerySource.valueOf(name);
	}

	private void applySourceContent(@NonNull View view) {
		ImageView icon = view.findViewById(R.id.titleIconImageView);
		icon.setImageResource(source.getIconId());
		icon.setColorFilter(ContextCompat.getColor(icon.getContext(), source.getColorId()));

		((TextView) view.findViewById(R.id.title)).setText(source.getTitleId());
		((TextView) view.findViewById(R.id.description)).setText(source.getDescriptionId());
		((TextView) view.findViewById(R.id.widget_title)).setText(source.getWidgetTitleId());
		((TextView) view.findViewById(R.id.widget_description)).setText(source.getWidgetDescriptionId());
	}

	@Nullable
	private MapWidgetInfo getDefaultWidgetInfo(@NonNull MapActivity activity) {
		// Only the default widget of this source, never a custom duplicate.
		return activity.getMapLayers().getMapWidgetRegistry()
				.getWidgetInfoById(source.getWidgetType().id);
	}

	private boolean isWidgetVisible(@NonNull MapActivity activity) {
		MapWidgetInfo widgetInfo = getDefaultWidgetInfo(activity);
		return widgetInfo != null && widgetInfo.isEnabledForAppMode(
				settings.getApplicationMode(), ScreenLayoutMode.getDefault(activity));
	}

	private void applyWidgetVisibility() {
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		MapWidgetInfo widgetInfo = getDefaultWidgetInfo(activity);
		if (widgetInfo == null) {
			return;
		}
		ApplicationMode appMode = settings.getApplicationMode();
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
		if (widgetInfo.isEnabledForAppMode(appMode, layoutMode) != showWidget) {
			MapWidgetRegistry widgetRegistry = activity.getMapLayers().getMapWidgetRegistry();
			widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, showWidget, layoutMode, true);
			activity.refreshMap();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_SHOW_WIDGET, showWidget);
	}

	public static boolean showInstance(@NonNull FragmentActivity activity, @NonNull StreetImagerySource source) {
		FragmentManager manager = activity.getSupportFragmentManager();
		String tag = getTag(source);
		if (!AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
			return false;
		}
		Bundle args = new Bundle();
		args.putString(KEY_SOURCE, source.name());

		StreetImageryFirstDialogFragment fragment = new StreetImageryFirstDialogFragment();
		fragment.setArguments(args);
		fragment.show(manager, tag);
		return true;
	}

	// One dialog class serves both providers, so the tag has to keep their identities apart.
	@NonNull
	private static String getTag(@NonNull StreetImagerySource source) {
		return TAG + "_" + source.name();
	}
}
