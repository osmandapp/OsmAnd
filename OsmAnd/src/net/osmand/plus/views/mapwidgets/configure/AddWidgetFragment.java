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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class AddWidgetFragment extends BaseOsmAndFragment {

	public static final String TAG = AddWidgetFragment.class.getSimpleName();

	private static final String KEY_GROUP_NAME = "group_name";
	private static final String KEY_WIDGET_ID = "widget_id";
	private static final String KEY_SELECTED_WIDGETS_IDS = "selected_widgets_ids";

	private OsmandApplication app;
	private boolean nightMode;

	private WidgetGroup widgetGroup;
	private WidgetParams widgetParams;

	private View view;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(KEY_GROUP_NAME)) {
				widgetGroup = WidgetGroup.valueOf(savedInstanceState.getString(KEY_GROUP_NAME));
			} else {
				widgetParams = WidgetParams.getById(savedInstanceState.getString(KEY_WIDGET_ID));
			}
		}
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
		String docsUrl = widgetGroup != null ? widgetGroup.docsUrl : widgetParams.getDocsUrl();
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
		title.setText(widgetGroup != null ? widgetGroup.titleId : widgetParams.titleId);

		TextView subTitle = view.findViewById(R.id.sub_title);
		if (widgetGroup != null) {
			String widgetsString = getString(R.string.shared_string_widgets);
			String widgetsCount = String.valueOf(widgetGroup.getWidgets().size());
			subTitle.setText(getString(R.string.ltr_or_rtl_combine_via_colon, widgetsString, widgetsCount));
		} else {
			subTitle.setText(R.string.shared_string_widget);
		}

		ImageView icon = view.findViewById(R.id.icon);
		int iconId = widgetGroup != null ? widgetGroup.getIconId(nightMode) : widgetParams.getIconId(nightMode);
		icon.setImageDrawable(getIcon(iconId));
	}

	private void dismiss() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void setupContent() {
		TextView description = view.findViewById(R.id.description);
		int descriptionId = widgetGroup != null ? widgetGroup.descId : widgetParams.descId;
		if (descriptionId != 0) {
			description.setText(descriptionId);
		} else {
			AndroidUiHelper.updateVisibility(description, false);
		}

		List<WidgetParams> widgets = widgetGroup != null
				? widgetGroup.getWidgets()
				: Collections.singletonList(widgetParams);
		inflateWidgetsList(widgets);

		int secondaryDescId = widgetGroup != null
				? widgetGroup.getSecondaryDescriptionId()
				: widgetParams.getSecondaryDescriptionId();
		int secondaryIconId = widgetGroup != null
				? widgetGroup.getSecondaryIconId()
				: widgetParams.getSecondaryIconId();
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

			if (widgetGroup != null && widget.descId != 0) {
				TextView description = view.findViewById(R.id.description);
				description.setText(widget.descId);
				AndroidUiHelper.updateVisibility(description, true);
			}

			container.addView(view);
		}
	}

	private void setupApplyButton() {
		View applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> app.showToastMessage("No action yet"));
		UiUtilities.setupDialogButton(nightMode, applyButton, DialogButtonType.PRIMARY, R.string.shared_string_apply);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (widgetGroup != null) {
			outState.putString(KEY_GROUP_NAME, widgetGroup.name());
		} else {
			outState.putString(KEY_WIDGET_ID, widgetParams.id);
		}
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_color_dark : R.color.activity_background_color_light;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull WidgetGroup widgetGroup) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AddWidgetFragment fragment = new AddWidgetFragment();
			fragment.widgetGroup = widgetGroup;
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull WidgetParams widgetParams) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AddWidgetFragment fragment = new AddWidgetFragment();
			fragment.widgetParams = widgetParams;
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}