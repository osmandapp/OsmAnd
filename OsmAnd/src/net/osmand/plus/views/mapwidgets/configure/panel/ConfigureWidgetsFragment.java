package net.osmand.plus.views.mapwidgets.configure.panel;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
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
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment.WidgetsOrderListener;

import java.util.Arrays;
import java.util.List;

public class ConfigureWidgetsFragment extends BaseOsmAndFragment implements WidgetsOrderListener, OnOffsetChangedListener {

	public static final String TAG = ConfigureWidgetsFragment.class.getSimpleName();

	private static final String INFO_LINK = "https://docs.osmand.net/en/main@latest/osmand/widgets";

	private static final String APP_MODE_ATTR = "app_mode_key";
	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private WidgetsListFragment selectedFragment;

	private Toolbar toolbar;
	private TabLayout tabLayout;
	private ViewPager2 viewPager;
	private View compensationView;

	private boolean nightMode;

	public void setSelectedPanel(@NonNull WidgetsPanel panel) {
		this.selectedPanel = panel;
	}

	public void setSelectedAppMode(@NonNull ApplicationMode appMode) {
		this.selectedAppMode = appMode;
	}

	public void setSelectedFragment(@Nullable WidgetsListFragment fragment) {
		this.selectedFragment = fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();

		if (savedInstanceState != null) {
			String appModeKey = savedInstanceState.getString(APP_MODE_ATTR);
			selectedAppMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_GROUP_ATTR));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.fragment_configure_widgets, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(app, view);
		}
		AppBarLayout appBar = view.findViewById(R.id.appbar);
		appBar.addOnOffsetChangedListener(this);

		toolbar = view.findViewById(R.id.toolbar);
		tabLayout = view.findViewById(R.id.tab_layout);
		viewPager = view.findViewById(R.id.view_pager);
		compensationView = view.findViewById(R.id.compensation_view);

		setupToolbar();
		setupTabLayout();

		return view;
	}

	private void setupToolbar() {
		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		UiUtilities.rotateImageByLayoutDirection(backButton);

		View infoButton = toolbar.findViewById(R.id.info_button);
		infoButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, Uri.parse(INFO_LINK), nightMode);
			}
		});
		TextView tvSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
		tvSubtitle.setText(selectedAppMode.toHumanString());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_ATTR, selectedAppMode.getStringKey());
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
	}

	private void setupTabLayout() {
		WidgetsTabAdapter tabAdapter = new WidgetsTabAdapter(this);
		viewPager.setAdapter(tabAdapter);
		viewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				selectedPanel = WidgetsPanel.values()[position];
			}
		});

		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
		});
		mediator.attach();

		int profileColor = selectedAppMode.getProfileColor(nightMode);
		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		tabLayout.setSelectedTabIndicatorColor(profileColor);
		tabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {

			@Override
			public void onTabSelected(Tab tab) {
				setupTabIconColor(tab, profileColor);
			}

			@Override
			public void onTabUnselected(Tab tab) {
				setupTabIconColor(tab, defaultIconColor);
			}

			@Override
			public void onTabReselected(Tab tab) { }

		});

		List<WidgetsPanel> panels = Arrays.asList(WidgetsPanel.values());
		for (int i = 0; i < tabLayout.getTabCount(); i++) {
			Tab tab = tabLayout.getTabAt(i);
			WidgetsPanel panel = panels.get(i);
			if (tab != null) {
				tab.setTag(panel);
				tab.setIcon(panel.getIconId());
			}
		}

		int position = panels.indexOf(selectedPanel);
		viewPager.setCurrentItem(position, false);

		if (position == 0) {
			Tab tab = tabLayout.getTabAt(position);
			setupTabIconColor(tab, profileColor);
		}
	}

	public void setupTabIconColor(@Nullable Tab tab, int color) {
		if (tab != null) {
			Drawable icon = tab.getIcon();
			if (icon != null) {
				icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
			}
		}
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		int height = toolbar.getHeight() - Math.abs(verticalOffset);
		compensationView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
	}

	@Override
	public void onWidgetsOrderApplied() {
		if (selectedFragment != null) {
			selectedFragment.updateContent();
		}
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null && !nightMode) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureWidgetsFragment fragment = new ConfigureWidgetsFragment();
			fragment.setSelectedPanel(panel);
			fragment.setSelectedAppMode(appMode);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}