package net.osmand.plus.views.mapwidgets.configure;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static net.osmand.aidl.OsmandAidlApi.WIDGET_ID_PREFIX;

public class AddWidgetFragment extends BaseOsmAndFragment {

	public static final String TAG = AddWidgetFragment.class.getSimpleName();

	private static final String KEY_APP_MODE = "app_mode";
	private static final String KEY_GROUP_NAME = "group_name";
	private static final String KEY_WIDGET_ID = "widget_id";
	private static final String KEY_EXTERNAL_PROVIDER_PACKAGE = "external_provider_package";
	private static final String KEY_SELECTED_WIDGETS_IDS = "selected_widgets_ids";

	private OsmandApplication app;
	private ApplicationMode appMode;
	private boolean nightMode;

	private WidgetDataHolder widgetsDataHolder;

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
			boolean externalWidget = !Algorithms.isEmpty(widgetsDataHolder.aidlWidgetId);
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

			((ImageView) view.findViewById(R.id.icon)).setImageDrawable(getIcon(widget.getIconId(nightMode)));
			((TextView) view.findViewById(R.id.title)).setText(widget.titleId);

			if (widgetsDataHolder.widgetGroup != null && widget.descId != 0) {
				TextView description = view.findViewById(R.id.description);
				description.setText(widget.descId);
				AndroidUiHelper.updateVisibility(description, true);
			}

			container.addView(view);
		}
	}

	private void inflateAidlWidget(@NonNull AidlMapWidgetWrapper aidlWidgetData) {

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

	private static class WidgetDataHolder {

		private final OsmandApplication app;

		private WidgetGroup widgetGroup;
		private WidgetParams widgetParams;

		private ConnectedApp connectedApp;
		private String aidlWidgetId;
		private String externalProviderPackage;
		private AidlMapWidgetWrapper aidlWidgetData;

		public WidgetDataHolder(@NonNull OsmandApplication app, @NonNull Bundle bundle) {
			this.app = app;

			if (bundle.containsKey(KEY_GROUP_NAME)) {
				widgetGroup = WidgetGroup.valueOf(bundle.getString(KEY_GROUP_NAME));
			} else if (bundle.containsKey(KEY_EXTERNAL_PROVIDER_PACKAGE)) {
				aidlWidgetId = bundle.getString(KEY_WIDGET_ID);
				externalProviderPackage = bundle.getString(KEY_EXTERNAL_PROVIDER_PACKAGE);
				connectedApp = app.getAidlApi().getConnectedApp(externalProviderPackage);
				if (connectedApp != null) {
					String sourceId = aidlWidgetId.replaceFirst(WIDGET_ID_PREFIX, "");
					aidlWidgetData = connectedApp.getWidgets().get(sourceId);
				}
			} else {
				widgetParams = WidgetParams.getById(bundle.getString(KEY_WIDGET_ID));
			}
		}

		@NonNull
		public String getTitle() {
			if (widgetGroup != null) {
				return getString(widgetGroup.titleId);
			} else if (widgetParams != null) {
				return getString(widgetParams.titleId);
			} else if (aidlWidgetData != null) {
				return aidlWidgetData.getMenuTitle();
			}
			return "";
		}

		public int getWidgetsCount() {
			return widgetGroup != null ? widgetGroup.getWidgets().size() : 1;
		}

		/**
		 * @return existing icon id or 0
		 */
		@DrawableRes
		public int getIconId(boolean nightMode) {
			if (widgetGroup != null) {
				return widgetGroup.getIconId(nightMode);
			} else if (widgetParams != null) {
				return widgetParams.getIconId(nightMode);
			} else if (aidlWidgetData != null) {
				String iconName = aidlWidgetData.getMenuIconName();
				return AndroidUtils.getDrawableId(app, iconName);
			}
			return 0;
		}

		@Nullable
		public String getDescription() {
			if (widgetGroup != null && widgetGroup.descId != 0) {
				return getString(widgetGroup.descId);
			} else if (widgetParams != null && widgetParams.descId != 0) {
				return getString(widgetParams.descId);
			} else if (connectedApp != null) {
				return connectedApp.getName();
			}
			return null;
		}

		@Nullable
		public List<WidgetParams> getWidgetsList() {
			if (widgetGroup != null) {
				return widgetGroup.getWidgets();
			} else if (widgetParams != null) {
				return Collections.singletonList(widgetParams);
			}
			return null;
		}

		@Nullable
		public AidlMapWidgetWrapper getAidlWidgetData() {
			return aidlWidgetData;
		}

		@StringRes
		public int getSecondaryDescriptionId() {
			if (widgetGroup != null) {
				return widgetGroup.getSecondaryDescriptionId();
			} else if (widgetParams != null) {
				return widgetParams.getSecondaryDescriptionId();
			}
			return 0;
		}

		@DrawableRes
		public int getSecondaryIconId() {
			if (widgetGroup != null) {
				return widgetGroup.getSecondaryIconId();
			} else if (widgetParams != null) {
				return widgetParams.getSecondaryIconId();
			}
			return 0;
		}

		@Nullable
		public String getDocsUrl() {
			if (widgetGroup != null) {
				return widgetGroup.docsUrl;
			} else if (widgetParams != null) {
				return widgetParams.getDocsUrl();
			}
			return null;
		}

		public void saveState(@NonNull Bundle outState) {
			if (widgetGroup != null) {
				outState.putString(KEY_GROUP_NAME, widgetGroup.name());
			} else if (widgetParams != null) {
				outState.putString(KEY_WIDGET_ID, widgetParams.id);
			} else {
				outState.putString(KEY_WIDGET_ID, aidlWidgetId);
				outState.putString(KEY_EXTERNAL_PROVIDER_PACKAGE, externalProviderPackage);
			}
		}

		@NonNull
		private String getString(@StringRes int stringId) {
			return app.getString(stringId);
		}
	}
}