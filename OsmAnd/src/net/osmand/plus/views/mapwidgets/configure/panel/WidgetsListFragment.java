package net.osmand.plus.views.mapwidgets.configure.panel;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.List;
import java.util.Set;

public class WidgetsListFragment extends Fragment implements OnScrollChangedListener {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;

	private View changeOrderListButton;
	private View changeOrderFooterButton;
	private LinearLayout widgetsContainer;

	private boolean nightMode;

	public void setSelectedPanel(@NonNull WidgetsPanel panel) {
		this.selectedPanel = panel;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		settings = app.getSettings();
		selectedAppMode = settings.getApplicationMode();
		nightMode = !settings.isLightContent();
		if (savedInstanceState != null) {
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_GROUP_ATTR));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.fragment_widgets_list, container, false);

		widgetsContainer = view.findViewById(R.id.widgets_list);
		changeOrderListButton = view.findViewById(R.id.change_order_button_in_list);
		changeOrderFooterButton = view.findViewById(R.id.change_order_button_in_bottom);

		NestedScrollView scrollView = view.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);

		updateContent();

		TextView panelTitle = view.findViewById(R.id.panel_title);
		panelTitle.setText(getString(selectedPanel.getTitleId()));

		setupReorderButton(changeOrderListButton);
		setupReorderButton(changeOrderFooterButton);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(null);
		}
	}

	public void setupReorderButton(@NonNull View view) {
		view.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ReorderWidgetsFragment.showInstance(activity.getSupportFragmentManager(), selectedPanel,
						selectedAppMode, getParentFragment());
			}
		});
		setupListItemBackground(view);
	}

	public void updateContent() {
		widgetsContainer.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		if (selectedPanel.isPagingAllowed()) {
			List<Set<MapWidgetInfo>> pagedWidgets =
					widgetRegistry.getAvailablePagedWidgetsForPanel(selectedAppMode, selectedPanel);
			for (int i = 0; i < pagedWidgets.size(); i++) {
				inflatePageItemView(i, inflater);
				inflatePageItemsViews(pagedWidgets.get(i), inflater);
			}
		} else {
			inflatePageItemsViews(widgetRegistry.getAvailableWidgetsForPanel(selectedAppMode, selectedPanel), inflater);
		}
	}

	private void inflatePageItemView(int index, @NonNull LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.configure_screen_list_item_page, widgetsContainer, false);
		View topDivider = view.findViewById(R.id.top_divider);
		TextView pageText = view.findViewById(R.id.page);
		AndroidUiHelper.updateVisibility(topDivider, index > 0);
		pageText.setText(getString(R.string.page_number, String.valueOf(index + 1)));
		widgetsContainer.addView(view);
	}

	private void inflatePageItemsViews(@NonNull Set<MapWidgetInfo> widgetsInfo, @NonNull LayoutInflater inflater) {
		int profileColor = selectedAppMode.getProfileColor(nightMode);
		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		for (MapWidgetInfo widgetInfo : widgetsInfo) {
			View view = inflater.inflate(R.layout.configure_screen_widget_item, widgetsContainer, false);

			TextView title = view.findViewById(R.id.title);
			title.setText(widgetInfo.getTitle(app));
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);

			boolean selected = widgetInfo.isEnabledForAppMode(selectedAppMode);

			ImageView imageView = view.findViewById(R.id.icon);
			WidgetViewHolder.updateWidgetIcon(imageView, widgetInfo, profileColor, defaultIconColor, selected, nightMode);

			ImageView secondaryIcon = view.findViewById(R.id.secondary_icon);
			secondaryIcon.setImageResource(R.drawable.ic_action_additional_option);
			AndroidUiHelper.updateVisibility(secondaryIcon, widgetInfo.getWidgetState() != null);

			CompoundButton compoundButton = view.findViewById(R.id.compound_button);
			compoundButton.setChecked(selected);
			UiUtilities.setupCompoundButton(nightMode, profileColor, compoundButton);

			view.findViewById(R.id.switch_container).setOnClickListener(view1 -> {
				compoundButton.performClick();

				boolean checked = compoundButton.isChecked();
				WidgetViewHolder.updateWidgetIcon(imageView, widgetInfo, profileColor, defaultIconColor, checked, nightMode);
				widgetRegistry.setVisibility(widgetInfo, checked);
			});

			view.setOnClickListener(v -> {
				if (widgetInfo.getWidgetState() == null) {
					boolean checked = !compoundButton.isChecked();
					compoundButton.setChecked(checked);
					WidgetViewHolder.updateWidgetIcon(imageView, widgetInfo, profileColor, defaultIconColor, checked, nightMode);
					widgetRegistry.setVisibility(widgetInfo, checked);
				} else {
					CallbackWithObject<WidgetState> callback = result -> {
						updateContent();
						return true;
					};
					widgetRegistry.showPopUpMenu(view, callback, widgetInfo.getWidgetState(), selectedAppMode,
							compoundButton.isChecked(), nightMode);
				}
			});
			setupListItemBackground(view);
			widgetsContainer.addView(view);
		}
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int activeColor = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
	}

	@Override
	public void onScrollChanged() {
		int y1 = AndroidUtils.getViewOnScreenY(changeOrderListButton);
		int y2 = AndroidUtils.getViewOnScreenY(changeOrderFooterButton);
		changeOrderFooterButton.setVisibility(y1 <= y2 ? View.GONE : View.VISIBLE);
	}
}