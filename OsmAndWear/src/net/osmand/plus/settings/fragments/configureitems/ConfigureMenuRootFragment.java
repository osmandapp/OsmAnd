package net.osmand.plus.settings.fragments.configureitems;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigureMenuRootFragment extends BaseOsmAndFragment {

	public static final String TAG = ConfigureMenuRootFragment.class.getName();

	private ApplicationMode appMode;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
		}
		if (appMode == null) {
			appMode = settings.getApplicationMode();
		}
		nightMode = !settings.isLightContentForMode(appMode);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = themedInflater.inflate(R.layout.fragment_ui_customization, container, false);
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view);

		setupToolbar(view);
		setupRecyclerView(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		ImageButton button = view.findViewById(R.id.close_button);
		button.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
		button.setOnClickListener(v -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				manager.popBackStack();
			}
		});
		TextView title = view.findViewById(R.id.toolbar_title);
		title.setText(R.string.ui_customization);
		title.setTextColor(getColor(nightMode ? R.color.text_color_primary_dark : R.color.list_background_color_dark));

		TextView subTitle = view.findViewById(R.id.toolbar_subtitle);
		subTitle.setText(appMode.toHumanString());
		subTitle.setTextColor(getColor(R.color.text_color_secondary_light));
		AndroidUiHelper.updateVisibility(subTitle, true);
	}

	private void setupRecyclerView(@NonNull View view) {
		List<Object> items = new ArrayList<>();
		items.add(getString(R.string.ui_customization_description, getString(R.string.prefs_plugins)));
		items.addAll(Arrays.asList(ScreenType.values()));

		FragmentActivity activity = requireActivity();
		CustomizationItemsAdapter adapter = new CustomizationItemsAdapter(activity, items, appMode, nightMode, type -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				ConfigureMenuItemsFragment.showInstance(manager, appMode, type);
			}
			return true;
		});
		RecyclerView recyclerView = view.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();

		FragmentActivity activity = requireActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).disableDrawer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		FragmentActivity activity = requireActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).enableDrawer();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull ApplicationMode appMode, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ConfigureMenuRootFragment fragment = new ConfigureMenuRootFragment();
			fragment.appMode = appMode;
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
