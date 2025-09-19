package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_EXTERNAL_PROVIDER_PACKAGE;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_EXTERNAL_WIDGET_ID;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_GROUP_NAME;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_WIDGETS_PANEL_ID;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetDataHolder.KEY_WIDGET_TYPE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.banner.WidgetPromoBanner;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.ArrayList;
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

		view = inflate(R.layout.base_widget_fragment_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		ViewGroup mainContent = view.findViewById(R.id.main_content);
		inflate(R.layout.add_widget_fragment_content, mainContent);

		setupToolbar();
		setupContent();

		return view;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
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
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			closeFragment();
		});
		toolbar.setTitle(widgetsDataHolder.getTitle());
		toolbar.setTitleTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
	}

	private void setupContent() {
		TextView descriptionText = view.findViewById(R.id.description);
		String description = widgetsDataHolder.getDescription();
		if (Algorithms.isEmpty(description)) {
			AndroidUiHelper.updateVisibility(descriptionText, false);
		} else {
			descriptionText.setText(description);
		}

		List<WidgetType> widgets = widgetsDataHolder.getWidgetsList(appMode);
		AidlMapWidgetWrapper aidlWidgetData = widgetsDataHolder.getAidlWidgetData();
		if (widgets != null) {
			Collator collator = OsmAndCollator.primaryCollator();
			widgets.sort((indexItem, indexItem2) -> collator.compare(app.getString(indexItem.titleId), app.getString(indexItem2.titleId)));
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

		for (int i = 0; i < widgets.size(); i++) {
			WidgetType widget = widgets.get(i);
			if (widget.isPurchased(app)) {
				int layoutId = R.layout.add_widget_item;
				View view = inflater.inflate(layoutId, container, false);
				String title = getString(widget.titleId);
				int descId = group != null ? widget.getGroupDescriptionId() : widget.descId;
				String desc = descId == 0 ? null : getString(descId);
				Drawable icon = getIcon(widget.getIconId(nightMode));
				boolean showDivider = i + 1 < widgets.size() && widgets.get(i + 1).isPurchased(app);
				setupWidgetItemView(view, widget.id, title, desc, icon, showDivider);
				container.addView(view);
			} else if (widget.isOBDWidget()) {
				View view = inflater.inflate(R.layout.selectable_widget_item_pro_banner, container, false);
				((ImageView) view.findViewById(R.id.icon)).setImageResource(widget.getIconId(nightMode));
				((TextView) view.findViewById(R.id.title)).setText(widget.titleId);
				view.setOnClickListener(v -> ChoosePlanFragment.showInstance(activity, OsmAndFeature.VEHICLE_METRICS));
				container.addView(view);
			} else {
				container.addView(new WidgetPromoBanner(activity, widget, false).build());
				addVerticalSpace(container, getDimensionPixelSize(R.dimen.content_padding_small));
			}
		}
	}

	private void inflateAidlWidget(@NonNull AidlMapWidgetWrapper aidlWidgetData) {
		ViewGroup container = view.findViewById(R.id.widgets_container);
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.add_widget_item, container, false);
		String widgetId = getAidlWidgetId(aidlWidgetData);
		String title = aidlWidgetData.getMenuTitle();
		String iconName = aidlWidgetData.getMenuIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		Drawable icon = iconId != 0 ? getPaintedIcon(iconId, appMode.getProfileColor(nightMode)) : null;
		setupWidgetItemView(view, widgetId, title, null, icon, false);

		container.addView(view);
	}

	private void setupWidgetItemView(@NonNull View view,
	                                 @NonNull String widgetId,
	                                 @NonNull String title,
	                                 @Nullable String description,
	                                 @Nullable Drawable icon,
	                                 boolean showDivider) {
		((ImageView) view.findViewById(R.id.icon)).setImageDrawable(icon);
		((TextView) view.findViewById(R.id.title)).setText(title);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), showDivider);

		View selectableBackground = view.findViewById(R.id.container);
		int color = appMode.getProfileColor(nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(selectableBackground, drawable);

		view.setOnClickListener(v -> {
			Fragment target = getTargetFragment();
			if (target instanceof AddWidgetListener) {
				WidgetsPanel widgetsPanel = widgetsDataHolder.getWidgetsPanel();
				((AddWidgetListener) target).onWidgetSelectedToAdd(widgetId, widgetsPanel, true);
			}
		});
	}

	@NonNull
	private String getAidlWidgetId(@NonNull AidlMapWidgetWrapper aidlWidgetData) {
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

	private void closeFragment() {
		requireActivity().getSupportFragmentManager().popBackStack();
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
		showInstance(manager, fragment);
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
		showInstance(manager, fragment);
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
		showInstance(manager, fragment);
	}

	private static void showInstance(@NonNull FragmentManager manager,
	                                 @NonNull AddWidgetFragment fragment) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	public interface AddWidgetListener {
		void onWidgetSelectedToAdd(@NonNull String widgetsId, @NonNull WidgetsPanel widgetsPanel, boolean recreateControls);
	}
}