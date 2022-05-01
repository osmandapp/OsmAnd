package net.osmand.plus.views.mapwidgets.configure.add;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.configure.add.WidgetDataHolder.KEY_EXTERNAL_PROVIDER_PACKAGE;
import static net.osmand.plus.views.mapwidgets.configure.add.WidgetDataHolder.KEY_GROUP_NAME;
import static net.osmand.plus.views.mapwidgets.configure.add.WidgetDataHolder.KEY_WIDGET_ID;

public class AddWidgetFragment extends BaseOsmAndFragment {

	public static final String TAG = AddWidgetFragment.class.getSimpleName();

	private static final String KEY_APP_MODE = "app_mode";
	private static final String KEY_SELECTED_WIDGETS_IDS = "selected_widgets_ids";

	private OsmandApplication app;
	private ApplicationMode appMode;
	private boolean nightMode;

	private WidgetDataHolder widgetsDataHolder;
	private Set<String> selectedWidgetsIds = new HashSet<>();

	private View view;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();

		Bundle args = getArguments();
		if (savedInstanceState != null) {
			restoreAppMode(savedInstanceState);
			widgetsDataHolder = new WidgetDataHolder(app, savedInstanceState);
			selectedWidgetsIds = (Set<String>) savedInstanceState.getSerializable(KEY_SELECTED_WIDGETS_IDS);
		} else if (args != null) {
			restoreAppMode(args);
			widgetsDataHolder = new WidgetDataHolder(app, args);
		}
	}

	private void restoreAppMode(@NonNull Bundle bundle) {
		String appModeKey = bundle.getString(KEY_APP_MODE);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, app.getSettings().getApplicationMode());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		view = themedInflater.inflate(R.layout.fragment_add_widget, container, false);

		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(app, view);
		}

		setupToolbar();
		setupContent();
		setupApplyButton();

		return view;
	}

	private void setupToolbar() {
		View closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss());

		View helpButton = view.findViewById(R.id.help_button);
		String docsUrl = widgetsDataHolder.getDocsUrl();
		if (!Algorithms.isEmpty(docsUrl)) {
			helpButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AndroidUtils.openUrl(activity, Uri.parse(docsUrl), nightMode);
				}
			});
		} else {
			AndroidUiHelper.updateVisibility(helpButton, false);
		}

		TextView title = view.findViewById(R.id.title);
		title.setText(widgetsDataHolder.getTitle());

		TextView subTitle = view.findViewById(R.id.sub_title);
		int widgetsCount = widgetsDataHolder.getWidgetsCount();
		if (widgetsCount > 1) {
			String widgetsString = getString(R.string.shared_string_widgets);
			String widgetsCountString = String.valueOf(widgetsCount);
			subTitle.setText(getString(R.string.ltr_or_rtl_combine_via_colon, widgetsString, widgetsCountString));
		} else {
			subTitle.setText(R.string.shared_string_widget);
		}

		ImageView icon = view.findViewById(R.id.icon);
		int iconId = widgetsDataHolder.getIconId(nightMode);
		if (iconId != 0) {
			WidgetIconsHelper iconsHelper = new WidgetIconsHelper(app, appMode.getProfileColor(nightMode), nightMode);
			boolean externalWidget = !Algorithms.isEmpty(widgetsDataHolder.getAidlWidgetId());
			iconsHelper.updateWidgetIcon(icon, 0, iconId, externalWidget);
		}
	}

	private void dismiss() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void setupContent() {
		TextView descriptionText = view.findViewById(R.id.description);
		String description = widgetsDataHolder.getDescription();
		if (Algorithms.isEmpty(description)) {
			AndroidUiHelper.updateVisibility(descriptionText, false);
		} else {
			descriptionText.setText(description);
		}


		List<WidgetParams> widgets = widgetsDataHolder.getWidgetsList();
		AidlMapWidgetWrapper aidlWidgetData = widgetsDataHolder.getAidlWidgetData();
		if (widgets != null) {
			inflateWidgetsList(widgets);
		} else if (aidlWidgetData != null) {
			inflateAidlWidget(aidlWidgetData);
		}

		int secondaryDescId = widgetsDataHolder.getSecondaryDescriptionId();
		int secondaryIconId = widgetsDataHolder.getSecondaryIconId();
		if (secondaryDescId != 0 && secondaryIconId != 0) {
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.secondary_info_container), true);
			((TextView) view.findViewById(R.id.secondary_description)).setText(secondaryDescId);
			((ImageView) view.findViewById(R.id.secondary_icon)).setImageDrawable(getIcon(secondaryIconId));
		}
	}

	private void inflateWidgetsList(@NonNull List<WidgetParams> widgets) {
		ViewGroup container = view.findViewById(R.id.widgets_container);
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);

		for (WidgetParams widget : widgets) {
			View view = inflater.inflate(R.layout.selectable_widget_item, container, false);
			String title = getString(widget.titleId);
			String desc = widget.descId != 0 ? getString(widget.descId) : null;
			Drawable icon = getIcon(widget.getIconId(nightMode));
			setupWidgetItemView(view, widget.id, title, desc, icon);
			container.addView(view);
		}
	}

	private void inflateAidlWidget(@NonNull AidlMapWidgetWrapper aidlWidgetData) {
		ViewGroup container = view.findViewById(R.id.widgets_container);
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.selectable_widget_item, container, false);
		String widgetId = aidlWidgetData.getId();
		String title = aidlWidgetData.getMenuTitle();
		String iconName = aidlWidgetData.getMenuIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		Drawable icon = iconId != 0 ? getPaintedContentIcon(iconId, appMode.getProfileColor(nightMode)) : null;
		setupWidgetItemView(view, widgetId, title, null, icon);

		container.addView(view);
	}

	private void setupWidgetItemView(@NonNull View view,
	                                 @NonNull String widgetId,
	                                 @NonNull String title,
	                                 @Nullable String description,
	                                 @Nullable Drawable icon) {
		((ImageView) view.findViewById(R.id.icon)).setImageDrawable(icon);
		((TextView) view.findViewById(R.id.title)).setText(title);

		if (widgetsDataHolder.getWidgetGroup() != null && !Algorithms.isEmpty(description)) {
			TextView descriptionText = view.findViewById(R.id.description);
			descriptionText.setText(description);
			AndroidUiHelper.updateVisibility(descriptionText, true);
		}

		CheckBox checkBox = view.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(checkBox, nightMode, CompoundButtonType.GLOBAL);

		boolean alreadyEnabled = isWidgetEnabled(widgetId);
		if (alreadyEnabled) {
			checkBox.setChecked(true);
			view.setSelected(true);
			view.setOnClickListener(v -> app.showShortToastMessage(R.string.import_duplicates_title));
		} else {
			if (selectedWidgetsIds.contains(widgetId)) {
				view.setSelected(true);
				checkBox.setChecked(true);
			}

			view.setOnClickListener(v -> {
				boolean selected = !view.isSelected();
				view.setSelected(selected);
				checkBox.setChecked(selected);
				if (selected) {
					selectedWidgetsIds.add(widgetId);
				} else {
					selectedWidgetsIds.remove(widgetId);
				}
			});
		}
	}

	private boolean isWidgetEnabled(@NonNull String widgetId) {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(appMode, ENABLED_MODE, WidgetsPanel.values());

		for (MapWidgetInfo widgetInfo : enabledWidgets) {
			if (widgetId.equals(widgetInfo.key)) {
				return true;
			}
		}

		return false;
	}

	private void setupApplyButton() {
		View applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> app.showToastMessage("No action yet"));
		UiUtilities.setupDialogButton(nightMode, applyButton, DialogButtonType.PRIMARY, R.string.shared_string_apply);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_APP_MODE, appMode.getStringKey());
		outState.putSerializable(KEY_SELECTED_WIDGETS_IDS, (Serializable) selectedWidgetsIds);
		widgetsDataHolder.saveState(outState);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_color_dark : R.color.activity_background_color_light;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull WidgetGroup widgetGroup) {
		Bundle args = new Bundle();
		args.putString(KEY_APP_MODE, appMode.getStringKey());
		args.putString(KEY_GROUP_NAME, widgetGroup.name());
		showFragment(fragmentManager, args);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull WidgetParams widgetParams) {
		Bundle args = new Bundle();
		args.putString(KEY_APP_MODE, appMode.getStringKey());
		args.putString(KEY_WIDGET_ID, widgetParams.id);
		showFragment(fragmentManager, args);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull String widgetId,
	                                @NonNull String externalProviderPackage) {
		Bundle args = new Bundle();
		args.putString(KEY_APP_MODE, appMode.getStringKey());
		args.putString(KEY_WIDGET_ID, widgetId);
		args.putString(KEY_EXTERNAL_PROVIDER_PACKAGE, externalProviderPackage);
		showFragment(fragmentManager, args);
	}

	private static void showFragment(@NonNull FragmentManager fragmentManager, @NonNull Bundle args) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Fragment fragment = new AddWidgetFragment();
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}