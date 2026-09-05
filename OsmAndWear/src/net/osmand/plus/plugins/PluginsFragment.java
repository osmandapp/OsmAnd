package net.osmand.plus.plugins;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginInstalledBottomSheetDialog.PluginStateListener;
import net.osmand.plus.plugins.custom.CustomOsmandPlugin;
import net.osmand.plus.plugins.online.OnlineOsmandPlugin;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import org.apache.commons.logging.Log;

public class PluginsFragment extends BaseOsmAndFragment implements PluginStateListener {

	private static final Log log = PlatformUtil.getLog(PluginsFragment.class);

	public static final String TAG = PluginsFragment.class.getName();

	public static final String OPEN_PLUGINS = "open_plugins";

	private PluginsListAdapter adapter;
	private LayoutInflater themedInflater;
	private boolean wasDrawerDisabled;
	private ProgressBar progressBar;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public LayoutInflater getThemedInflater() {
		return themedInflater;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentActivity activity = requireMyActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				FragmentActivity activity = getActivity();
				if (activity instanceof MapActivity) {
					dismissImmediate();
					MapActivity mapActivity = (MapActivity) activity;
					mapActivity.launchPrevActivityIntent();
				}
			}
		});
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.plugins, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		progressBar = view.findViewById(R.id.progress_bar);

		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.plugins_screen);

		ImageView closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> {
			Activity activity = getMyActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		UiUtilities.rotateImageByLayoutDirection(closeButton);

		adapter = new PluginsListAdapter(this, requireContext());

		ListView listView = view.findViewById(R.id.plugins_list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener((parent, view1, position, id) -> {
			Object tag = view1.getTag();
			if (tag instanceof OsmandPlugin) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					PluginInfoFragment.showInstance(activity.getSupportFragmentManager(), PluginsFragment.this, (OsmandPlugin) tag);
				}
			} else if (tag instanceof ConnectedApp) {
				switchEnabled((ConnectedApp) tag);
			}
		});

		updateProgressVisibility(true);
		PluginsHelper.fetchOnlinePlugins(app, plugins -> {
			for (OnlineOsmandPlugin plugin : plugins) {
				OsmandPlugin p = PluginsHelper.getPlugin(plugin.getId());
				if (p == null) {
					adapter.add(plugin);
				}
			}
			updateProgressVisibility(false);
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		Activity activity = getActivity();
		PluginsHelper.checkInstalledMarketPlugins(app, activity);
		adapter.notifyDataSetChanged();
		if (activity instanceof MapActivity) {
			MapActivity mapActivity = ((MapActivity) activity);
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (getActivity() instanceof MapActivity && !wasDrawerDisabled) {
			((MapActivity) getActivity()).enableDrawer();
		}
	}

	void enableDisablePlugin(OsmandPlugin plugin) {
		if (PluginsHelper.enablePlugin(getActivity(), app, plugin, !plugin.isEnabled())) {
			adapter.notifyDataSetChanged();
			notifyPluginStateListener(plugin);
		}
	}

	void installPlugin(OsmandPlugin plugin) {
		PluginsHelper.installPlugin(getActivity(), plugin, this::dismissImmediate);
	}

	void switchEnabled(@NonNull ConnectedApp connectedApp) {
		app.getAidlApi().switchEnabled(connectedApp);
		OsmandPlugin plugin = PluginsHelper.getPlugin(connectedApp.getPack());
		if (plugin != null) {
			PluginsHelper.enablePlugin(getActivity(), app, plugin, connectedApp.isEnabled());
			notifyPluginStateListener(plugin);
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onPluginStateChanged(@NonNull OsmandPlugin plugin) {
		adapter.notifyDataSetChanged();
		notifyPluginStateListener(plugin);
	}

	@Override
	public void onPluginInstalled(@NonNull OsmandPlugin plugin) {
		dismissImmediate();
	}

	private void notifyPluginStateListener(@NonNull OsmandPlugin plugin) {
		Fragment target = getTargetFragment();
		if (target instanceof PluginStateListener) {
			((PluginStateListener) target).onPluginStateChanged(plugin);
		}
	}

	void showOptionsMenu(View view, OsmandPlugin plugin) {
		PopupMenu optionsMenu = new PopupMenu(view.getContext(), view);
		if (!plugin.isOnline()) {
			MenuItem enableDisableItem = optionsMenu.getMenu().add(
					plugin.isEnabled() ?
							R.string.shared_string_disable :
							R.string.shared_string_enable);
			enableDisableItem.setOnMenuItemClickListener(item -> {
				enableDisablePlugin(plugin);
				optionsMenu.dismiss();
				return true;
			});
		} else {
			MenuItem installItem = optionsMenu.getMenu().add(R.string.shared_string_install);
			installItem.setOnMenuItemClickListener(item -> {
				installPlugin(plugin);
				optionsMenu.dismiss();
				return true;
			});
		}

		SettingsScreenType settingsScreenType = plugin.getSettingsScreenType();
		if (settingsScreenType != null && plugin.isActive()) {
			MenuItem settingsItem = optionsMenu.getMenu().add(R.string.shared_string_settings);
			settingsItem.setOnMenuItemClickListener(item -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					BaseSettingsFragment.showInstance(activity, settingsScreenType);
				}
				optionsMenu.dismiss();
				return true;
			});
		}

		if (!plugin.isOnline() && plugin instanceof CustomOsmandPlugin) {
			MenuItem settingsItem = optionsMenu.getMenu().add(R.string.shared_string_delete);
			settingsItem.setOnMenuItemClickListener(item -> {
				showDeletePluginDialog((CustomOsmandPlugin) plugin);
				optionsMenu.dismiss();
				return true;
			});
		}

		optionsMenu.show();
	}

	private void showDeletePluginDialog(CustomOsmandPlugin plugin) {
		Context context = getContext();
		if (context != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(getString(R.string.delete_confirmation_msg, plugin.getName()));
			builder.setMessage(R.string.are_you_sure);
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
				PluginsHelper.removeCustomPlugin(app, plugin);
				adapter.remove(plugin);
			});
			builder.show();
		}
	}

	private void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	public void dismissImmediate() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				log.error(e);
			}
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager) {
		return showInstance(fragmentManager, null);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			PluginsFragment fragment = new PluginsFragment();
			fragment.setTargetFragment(target, 0);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}