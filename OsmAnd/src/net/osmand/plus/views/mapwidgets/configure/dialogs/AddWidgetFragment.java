package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_EXTERNAL_PROVIDER_PACKAGE;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_EXTERNAL_WIDGET_ID;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_GROUP_NAME;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_WIDGETS_PANEL_ID;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_WIDGET_TYPE;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.banner.WidgetPromoBanner;
import net.osmand.plus.views.mapwidgets.banner.WidgetPromoBanner.WidgetData;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AddWidgetFragment extends BaseWidgetFragment {

	public static final String TAG = AddWidgetFragment.class.getSimpleName();

	private static final String KEY_APP_MODE = "app_mode";
	private static final String KEY_SELECTED_WIDGETS_IDS = "selected_widgets_ids";
	private static final String KEY_ALREADY_SELECTED_WIDGETS_IDS = "already_selected_widgets_ids";

	private WidgetDataHolder widgetsDataHolder;
	private Map<Integer, String> selectedWidgetsIds = new TreeMap<>();
	private List<String> alreadySelectedWidgetsIds;

	private View view;
	private DialogButton applyButton;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			initFromBundle(savedInstanceState);
		} else if (args != null) {
			initFromBundle(args);
			selectWidgetByDefault();
		}

		view = themedInflater.inflate(R.layout.base_widget_fragment_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		ViewGroup mainContent = view.findViewById(R.id.main_content);
		themedInflater.inflate(R.layout.add_widget_fragment_content, mainContent);

		setupToolbar();
		setupContent();
		setupApplyButton();

		return view;
	}

	private void initFromBundle(@NonNull Bundle bundle) {
		String appModeKey = bundle.getString(KEY_APP_MODE);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, app.getSettings().getApplicationMode());

		widgetsDataHolder = new WidgetDataHolder(app, bundle);

		if (bundle.containsKey(KEY_ALREADY_SELECTED_WIDGETS_IDS)) {
			alreadySelectedWidgetsIds = (List<String>) AndroidUtils.getSerializable(bundle, KEY_ALREADY_SELECTED_WIDGETS_IDS, ArrayList.class);
		}

		if (bundle.containsKey(KEY_SELECTED_WIDGETS_IDS)) {
			selectedWidgetsIds = (Map<Integer, String>) AndroidUtils.getSerializable(bundle, KEY_SELECTED_WIDGETS_IDS, TreeMap.class);
		}
	}

	private void setupToolbar() {
		View closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss());

		View helpButton = view.findViewById(R.id.help_button);
		int docsUrlId = widgetsDataHolder.getDocsUrl();
		if (docsUrlId != 0) {
			String docsUrl = getString(docsUrlId);
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

	private void setupContent() {
		TextView descriptionText = view.findViewById(R.id.description);
		String description = widgetsDataHolder.getDescription();
		if (Algorithms.isEmpty(description)) {
			AndroidUiHelper.updateVisibility(descriptionText, false);
		} else {
			descriptionText.setText(description);
		}

		List<WidgetType> widgets = widgetsDataHolder.getWidgetsList();
		AidlMapWidgetWrapper aidlWidgetData = widgetsDataHolder.getAidlWidgetData();
		if (widgets != null) {
			inflateWidgetsList(widgets);
		} else if (aidlWidgetData != null) {
			inflateAidlWidget(aidlWidgetData);
		}

		String secondaryDesc = widgetsDataHolder.getSecondaryDescription();
		int secondaryIconId = widgetsDataHolder.getSecondaryIconId();
		if (secondaryDesc != null && secondaryIconId != 0) {
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.secondary_info_container), true);
			((TextView) view.findViewById(R.id.secondary_description)).setText(secondaryDesc);
			((ImageView) view.findViewById(R.id.secondary_icon)).setImageDrawable(getIcon(secondaryIconId));
		}
	}

	private void inflateWidgetsList(@NonNull List<WidgetType> widgets) {
		ViewGroup container = view.findViewById(R.id.widgets_container);
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
		WidgetGroup group = widgetsDataHolder.getWidgetGroup();
		MapActivity activity = requireMapActivity();

		for (WidgetType widget : widgets) {
			if (widget.isPurchased(app)) {
				int layoutId = widget.descId != 0 ? R.layout.selectable_widget_item : R.layout.selectable_widget_item_no_description;
				View view = inflater.inflate(layoutId, container, false);
				String title = getString(widget.titleId);
				int descId = group != null ? widget.getGroupDescriptionId() : widget.descId;
				String desc = descId == 0 ? null : getString(descId);
				Drawable icon = getIcon(widget.getIconId(nightMode));
				setupWidgetItemView(view, widget.id, title, desc, icon, widget.getDefaultOrder());
				container.addView(view);
			} else {
				WidgetData widgetData = new WidgetData(widget.titleId, widget.dayIconId, widget.nightIconId);
				WidgetPromoBanner banner = new WidgetPromoBanner(activity, widgetData, false);
				container.addView(banner.build(activity));
			}
		}
	}

	private void inflateAidlWidget(@NonNull AidlMapWidgetWrapper aidlWidgetData) {
		ViewGroup container = view.findViewById(R.id.widgets_container);
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.selectable_widget_item_no_description, container, false);
		String widgetId = getAidlWidgetId(aidlWidgetData);
		String title = aidlWidgetData.getMenuTitle();
		String iconName = aidlWidgetData.getMenuIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		Drawable icon = iconId != 0 ? getPaintedContentIcon(iconId, appMode.getProfileColor(nightMode)) : null;
		setupWidgetItemView(view, widgetId, title, null, icon, 0);

		container.addView(view);
	}

	private void setupWidgetItemView(@NonNull View view,
									 @NonNull String widgetId,
									 @NonNull String title,
									 @Nullable String description,
									 @Nullable Drawable icon,
									 int order) {
		((ImageView) view.findViewById(R.id.icon)).setImageDrawable(icon);
		((TextView) view.findViewById(R.id.title)).setText(title);

		if (widgetsDataHolder.getWidgetGroup() != null && !Algorithms.isEmpty(description)) {
			TextView descriptionText = view.findViewById(R.id.description);
			descriptionText.setText(description);
			AndroidUiHelper.updateVisibility(descriptionText, true);
		}

		CheckBox checkBox = view.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(checkBox, nightMode, CompoundButtonType.GLOBAL);

		if (selectedWidgetsIds.containsValue(widgetId)) {
			view.setSelected(true);
			checkBox.setChecked(true);
		}

		view.setOnClickListener(v -> {
			boolean selected = !view.isSelected();
			view.setSelected(selected);
			checkBox.setChecked(selected);
			updateWidgetSelection(order, widgetId, selected);
			enableDisableApplyButton();
		});

	}

	private boolean isWidgetEnabled(@NonNull MapActivity mapActivity, @NonNull String widgetId) {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(mapActivity, appMode,
				ENABLED_MODE | MATCHING_PANELS_MODE, Arrays.asList(WidgetsPanel.values()));

		for (MapWidgetInfo widgetInfo : enabledWidgets) {
			if (widgetId.equals(widgetInfo.key)) {
				return true;
			}
		}

		return false;
	}

	private String getAidlWidgetId(AidlMapWidgetWrapper aidlWidgetData) {
		return OsmandAidlApi.WIDGET_ID_PREFIX + aidlWidgetData.getId();
	}

	private void selectWidgetByDefault() {
		WidgetType widget = widgetsDataHolder.getMainWidget();
		AidlMapWidgetWrapper aidlWidgetData = widgetsDataHolder.getAidlWidgetData();
		if (widget != null) {
			updateWidgetSelection(widget.getDefaultOrder(), widget.id, widget.isPurchased(app));
		} else if (aidlWidgetData != null) {
			updateWidgetSelection(0, getAidlWidgetId(aidlWidgetData), true);
		}
	}

	private void updateWidgetSelection(int order, @NonNull String widgetId, boolean selected) {
		if (selected) {
			selectedWidgetsIds.put(order, widgetId);
		} else {
			selectedWidgetsIds.remove(order);
		}
	}

	private void setupApplyButton() {
		applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> {
			Fragment target = getTargetFragment();
			if (target instanceof AddWidgetListener) {
				List<String> widgetsIds = new ArrayList<>(selectedWidgetsIds.values());
				WidgetsPanel widgetsPanel = widgetsDataHolder.getWidgetsPanel();
				((AddWidgetListener) target).onWidgetsSelectedToAdd(widgetsIds, widgetsPanel, true);
			}
			dismiss();
		});
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_add);
		enableDisableApplyButton();
	}

	private void enableDisableApplyButton() {
		applyButton.setEnabled(!selectedWidgetsIds.isEmpty());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_APP_MODE, appMode.getStringKey());
		outState.putSerializable(KEY_SELECTED_WIDGETS_IDS, (Serializable) selectedWidgetsIds);
		outState.putSerializable(KEY_ALREADY_SELECTED_WIDGETS_IDS, (Serializable) alreadySelectedWidgetsIds);
		widgetsDataHolder.saveState(outState);
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		recreateFragment();
	}

	@Override
	protected String getFragmentTag() {
		return TAG;
	}

	/**
	 * @param alreadySelectedWidgetsIds If in edit mode ({@link ReorderWidgetsFragment}), non-null list
	 *                                  of added widgets ids of this group; null if in view mode
	 *                                  ({@link WidgetsListFragment})
	 */
	public static void showGroupDialog(@NonNull FragmentManager manager,
									   @Nullable Fragment target,
									   @NonNull ApplicationMode appMode,
									   @NonNull WidgetsPanel widgetsPanel,
									   @NonNull WidgetGroup widgetGroup,
									   @Nullable List<String> alreadySelectedWidgetsIds) {
		Bundle args = new Bundle();
		args.putString(KEY_APP_MODE, appMode.getStringKey());
		args.putString(KEY_WIDGETS_PANEL_ID, widgetsPanel.name());
		args.putString(KEY_GROUP_NAME, widgetGroup.name());
		args.putSerializable(KEY_ALREADY_SELECTED_WIDGETS_IDS, (Serializable) alreadySelectedWidgetsIds);
		AddWidgetFragment fragment = new AddWidgetFragment();
		fragment.setArguments(args);
		fragment.setTargetFragment(target, 0);
		showFragment(manager, fragment);
	}

	/**
	 * @see AddWidgetListener#showGroupDialog
	 */
	public static void showWidgetDialog(@NonNull FragmentManager manager,
										@Nullable Fragment target,
										@NonNull ApplicationMode appMode,
										@NonNull WidgetsPanel widgetsPanel,
										@NonNull WidgetType widgetType,
										@Nullable List<String> alreadySelectedWidgetsIds) {
		Bundle args = new Bundle();
		args.putString(KEY_APP_MODE, appMode.getStringKey());
		args.putString(KEY_WIDGETS_PANEL_ID, widgetsPanel.name());
		args.putString(KEY_WIDGET_TYPE, widgetType.name());
		args.putSerializable(KEY_ALREADY_SELECTED_WIDGETS_IDS, (Serializable) alreadySelectedWidgetsIds);
		AddWidgetFragment fragment = new AddWidgetFragment();
		fragment.setArguments(args);
		fragment.setTargetFragment(target, 0);
		showFragment(manager, fragment);
	}

	/**
	 * @see AddWidgetListener#showGroupDialog
	 */
	public static void showExternalWidgetDialog(@NonNull FragmentManager manager,
												@Nullable Fragment target,
												@NonNull ApplicationMode appMode,
												@NonNull WidgetsPanel widgetsPanel,
												@NonNull String widgetId,
												@NonNull String externalProviderPackage,
												@Nullable List<String> alreadySelectedWidgetsIds) {
		Bundle args = new Bundle();
		args.putString(KEY_APP_MODE, appMode.getStringKey());
		args.putString(KEY_WIDGETS_PANEL_ID, widgetsPanel.name());
		args.putString(KEY_EXTERNAL_WIDGET_ID, widgetId);
		args.putString(KEY_EXTERNAL_PROVIDER_PACKAGE, externalProviderPackage);
		args.putSerializable(KEY_ALREADY_SELECTED_WIDGETS_IDS, (Serializable) alreadySelectedWidgetsIds);
		AddWidgetFragment fragment = new AddWidgetFragment();
		fragment.setArguments(args);
		fragment.setTargetFragment(target, 0);
		showFragment(manager, fragment);
	}

	private static void showFragment(@NonNull FragmentManager manager, @NonNull AddWidgetFragment fragment) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	public interface AddWidgetListener {
		void onWidgetsSelectedToAdd(@NonNull List<String> widgetsIds, @NonNull WidgetsPanel widgetsPanel, boolean recreateControls);
	}
}