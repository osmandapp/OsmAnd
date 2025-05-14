package net.osmand.plus.views.mapwidgets.configure.settings;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsConfigurationChangeListener;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public abstract class WidgetSettingsBaseFragment extends BaseOsmAndFragment {

	public static final String KEY_APP_MODE = "app_mode";
	public static final String KEY_WIDGET_ID = "widget_id";

	protected MapWidgetRegistry widgetRegistry;
	protected ApplicationMode appMode;
	protected String widgetId;

	protected View view;

	@NonNull
	public abstract WidgetType getWidget();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
	}

	protected void initParams(@NonNull Bundle bundle) {
		widgetId = bundle.getString(KEY_WIDGET_ID);
		appMode = ApplicationMode.valueOfStringKey(bundle.getString(KEY_APP_MODE), settings.getApplicationMode());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			initParams(savedInstanceState);
		} else if (args != null) {
			initParams(args);
		}

		updateNightMode();
		view = themedInflater.inflate(R.layout.base_widget_fragment_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar();
		setupContent(themedInflater, view.findViewById(R.id.main_content));
		setupApplyButton();

		return view;
	}

	private void setupToolbar() {
		WidgetType widget = getWidget();

		View closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss());

		View helpButton = view.findViewById(R.id.help_button);
		int docsUrlId = widget.docsUrlId;
		if (docsUrlId == 0) {
			AndroidUiHelper.updateVisibility(helpButton, false);
		} else {
			String docsUrl = getString(docsUrlId);
			helpButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AndroidUtils.openUrl(activity, Uri.parse(docsUrl), nightMode);
				}
			});
		}

		TextView title = view.findViewById(R.id.title);
		title.setText(widget.titleId);

		TextView subTitle = view.findViewById(R.id.sub_title);
		subTitle.setText(R.string.shared_string_settings);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getIcon(widget.getIconId(nightMode)));
	}

	protected abstract void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container);

	private void setupApplyButton() {
		DialogButton applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> {
			applySettings();
			MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateControls();
			}
			Fragment target = getTargetFragment();
			if (target instanceof WidgetsConfigurationChangeListener) {
				((WidgetsConfigurationChangeListener) target).onWidgetsConfigurationChanged();
			}
			dismiss();
		});
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);
	}

	protected abstract void applySettings();

	protected void dismiss() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_APP_MODE, appMode.getStringKey());
		outState.putString(KEY_WIDGET_ID, widgetId);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.activity_background_color_light;
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@NonNull
	protected Drawable getPressedStateDrawable() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		return UiUtilities.getColoredSelectableDrawable(app, activeColor);
	}

	@Nullable
	protected MapActivity getMapActivity() {
		Activity activity = getActivity();
		return activity != null ? ((MapActivity) activity) : null;
	}

	public static void showFragment(@NonNull FragmentManager manager,
	                                @NonNull Bundle args,
	                                @Nullable Fragment target,
	                                @NonNull WidgetSettingsBaseFragment fragment) {
		String tag = fragment.getClass().getSimpleName();
		if (AndroidUtils.isFragmentCanBeAdded(manager, tag) && manager.findFragmentByTag(tag) == null) {
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, tag)
					.addToBackStack(tag)
					.commitAllowingStateLoss();
		}
	}
}