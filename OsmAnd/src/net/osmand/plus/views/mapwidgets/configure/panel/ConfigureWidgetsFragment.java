package net.osmand.plus.views.mapwidgets.configure.panel;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.Tab;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.reorder.OnNewOrderAppliedCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;

import java.util.ArrayList;
import java.util.List;

public class ConfigureWidgetsFragment extends BaseOsmAndFragment implements OnNewOrderAppliedCallback {

	public static final String TAG = ConfigureWidgetsFragment.class.getSimpleName();

	private static final String INFO_LINK = "https://docs.osmand.net/en/main@latest/osmand/widgets";

	private static final String APP_MODE_ATTR = "app_mode_key";
	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private ApplicationMode appMode;

	private View view;
	private Toolbar toolbar;
	private View btnChangeOrder;
	private TabLayout tabLayout;
	private ViewPager2 viewPager;

	private List<WidgetsPanel> availablePanels;
	private WidgetsListFragment currentListFragment;
	private WidgetsPanel selectedPanel;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();

		if (savedInstanceState != null) {
			restoreData(savedInstanceState);
		}

		availablePanels = new ArrayList<>();
		availablePanels.add(WidgetsPanel.LEFT);
		availablePanels.add(WidgetsPanel.RIGHT);
		availablePanels.add(WidgetsPanel.TOP);
		availablePanels.add(WidgetsPanel.BOTTOM);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		inflater = UiUtilities.getInflater(getContext(), nightMode);

		view = inflater.inflate(R.layout.fragment_configure_widgets, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(app, view);
		}

		toolbar = view.findViewById(R.id.toolbar);
		tabLayout = view.findViewById(R.id.tab_layout);
		btnChangeOrder = view.findViewById(R.id.change_order_button_in_bottom);
		viewPager = view.findViewById(R.id.view_pager);

		setupToolbar();
		setupTabLayout();
		setupReorderButton(btnChangeOrder);

		return view;
	}

	private void setupToolbar() {
		ImageButton backBtn = toolbar.findViewById(R.id.back_button);
		backBtn.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		UiUtilities.rotateImageByLayoutDirection(backBtn);
		View infoBtn = toolbar.findViewById(R.id.info_button);
		infoBtn.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, Uri.parse(INFO_LINK), nightMode);
			}
		});
		TextView tvSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
		String appModeName = appMode.toHumanString();
		tvSubtitle.setText(appModeName);
	}

	private void setupTabLayout() {
		WidgetsTabAdapter tabAdapter = new WidgetsTabAdapter(this, availablePanels);
		viewPager.setAdapter(tabAdapter);

		viewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				selectedPanel = availablePanels.get(position);
			}
		});

		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> { });
		mediator.attach();

		for (int i = 0; i < tabLayout.getTabCount(); i++) {
			Tab tab = tabLayout.getTabAt(i);
			WidgetsPanel panel = availablePanels.get(i);
			if (tab != null ) {
				tab.setTag(panel);
				tab.setIcon(panel.getIconId());
			}
		}

		int selectedTabPosition = availablePanels.indexOf(selectedPanel);
		viewPager.setCurrentItem(selectedTabPosition, false);
	}

	public void setupReorderButton(View btnChangeOrder) {
		btnChangeOrder.setOnClickListener(v -> onReorderButtonClicked());
		setupListItemBackground(btnChangeOrder);
	}

	private void onReorderButtonClicked() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ReorderWidgetsFragment.showInstance(this, activity, selectedPanel, appMode);
		}
	}

	@Nullable
	public View getBtnChangeOrder() {
		return btnChangeOrder;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_ATTR, appMode.getStringKey());
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
	}

	private void restoreData(@NonNull Bundle savedInstanceState) {
		String appModeKey = savedInstanceState.getString(APP_MODE_ATTR);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		String groupName = savedInstanceState.getString(SELECTED_GROUP_ATTR);
		selectedPanel = WidgetsPanel.valueOf(groupName);
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	public void setSelectedPanel(WidgetsPanel selectedPanel) {
		this.selectedPanel = selectedPanel;
	}

	public WidgetsPanel getSelectedPanel() {
		return selectedPanel;
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public void setCurrentListFragment(WidgetsListFragment currentListFragment) {
		this.currentListFragment = currentListFragment;
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null && Build.VERSION.SDK_INT >= 23 && !nightMode) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onNewOrderApplied() {
		if (currentListFragment != null) {
			currentListFragment.updateContent();
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull WidgetsPanel selectedGroup,
	                                @NonNull ApplicationMode appMode) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureWidgetsFragment fragment = new ConfigureWidgetsFragment();
			fragment.setSelectedPanel(selectedGroup);
			fragment.setAppMode(appMode);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

}
