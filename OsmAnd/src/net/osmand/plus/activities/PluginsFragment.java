package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dialogs.PluginInstalledBottomSheetDialog.PluginStateListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PluginsFragment extends BaseOsmAndFragment implements PluginStateListener {

	private static final Log log = PlatformUtil.getLog(PluginsFragment.class);

	public static final String TAG = PluginsFragment.class.getName();

	public static final String OPEN_PLUGINS = "open_plugins";

	private OsmandApplication app;
	private PluginsListAdapter adapter;

	private LayoutInflater themedInflater;
	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
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
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();

		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.plugins, container, false);
		AndroidUtils.addStatusBarPadding21v(getContext(), view);

		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.plugins_screen);

		ImageView closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Activity activity = getMyActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
		UiUtilities.rotateImageByLayoutDirection(closeButton);

		adapter = new PluginsListAdapter(requireContext());

		ListView listView = view.findViewById(R.id.plugins_list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Object tag = view.getTag();
				if (tag instanceof OsmandPlugin) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						PluginInfoFragment.showInstance(activity.getSupportFragmentManager(), (OsmandPlugin) tag);
					}
				} else if (tag instanceof ConnectedApp) {
					switchEnabled((ConnectedApp) tag);
				}
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		OsmandPlugin.checkInstalledMarketPlugins(app, getActivity());
		adapter.notifyDataSetChanged();
	}

	private void enableDisablePlugin(OsmandPlugin plugin, boolean enable) {
		if (OsmandPlugin.enablePlugin(getActivity(), app, plugin, enable)) {
			adapter.notifyDataSetChanged();
		}
	}

	private void switchEnabled(@NonNull ConnectedApp connectedApp) {
		app.getAidlApi().switchEnabled(connectedApp);
		OsmandPlugin plugin = OsmandPlugin.getPlugin(connectedApp.getPack());
		if (plugin != null) {
			OsmandPlugin.enablePlugin(getActivity(), app, plugin, connectedApp.isEnabled());
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onPluginStateChanged(OsmandPlugin plugin) {
		adapter.notifyDataSetChanged();
	}

	protected class PluginsListAdapter extends ArrayAdapter<Object> {

		PluginsListAdapter(Context context) {
			super(context, R.layout.plugins_list_item, new ArrayList<>());
			addAll(getFilteredPluginsAndApps());
		}

		private List<Object> getFilteredPluginsAndApps() {
			List<ConnectedApp> connectedApps = app.getAidlApi().getConnectedApps();
			List<OsmandPlugin> visiblePlugins = OsmandPlugin.getVisiblePlugins();

			for (Iterator<OsmandPlugin> iterator = visiblePlugins.iterator(); iterator.hasNext(); ) {
				OsmandPlugin plugin = iterator.next();
				for (ConnectedApp app : connectedApps) {
					if (plugin.getId().equals(app.getPack())) {
						iterator.remove();
					}
				}
			}
			List<Object> list = new ArrayList<>();
			list.addAll(connectedApps);
			list.addAll(visiblePlugins);

			return list;
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = themedInflater.inflate(R.layout.plugins_list_item, parent, false);
			}
			Context context = view.getContext();

			boolean active = false;
			int logoContDescId = R.string.shared_string_disable;
			String name = "";

			ImageButton pluginLogo = view.findViewById(R.id.plugin_logo);
			ImageView pluginOptions = view.findViewById(R.id.plugin_options);
			TextView pluginDescription = view.findViewById(R.id.plugin_description);

			Object item = getItem(position);
			if (item instanceof ConnectedApp) {
				final ConnectedApp app = (ConnectedApp) item;
				active = app.isEnabled();
				if (!active) {
					logoContDescId = R.string.shared_string_enable;
				}
				name = app.getName();
				pluginDescription.setText(R.string.third_party_application);
				pluginLogo.setImageDrawable(app.getIcon());
				pluginLogo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						switchEnabled(app);
					}
				});
				pluginOptions.setVisibility(View.GONE);
				pluginOptions.setOnClickListener(null);
				view.setTag(app);
			} else if (item instanceof OsmandPlugin) {
				final OsmandPlugin plugin = (OsmandPlugin) item;
				active = plugin.isActive();
				if (!active) {
					logoContDescId = plugin.needsInstallation()
							? R.string.access_shared_string_not_installed : R.string.shared_string_enable;
				}
				name = plugin.getName();
				pluginDescription.setText(plugin.getDescription());

				int color = AndroidUtils.getColorFromAttr(context, R.attr.list_background_color);
				Drawable pluginIcon = plugin.getLogoResource();
				if (pluginIcon.getConstantState() != null) {
					pluginIcon = pluginIcon.getConstantState().newDrawable().mutate();
				}
				pluginLogo.setImageDrawable(UiUtilities.tintDrawable(pluginIcon, color));
				pluginLogo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (plugin.isActive() || !plugin.needsInstallation()) {
							enableDisablePlugin(plugin, !plugin.isActive());
						}
					}
				});
				pluginOptions.setVisibility(View.VISIBLE);
				pluginOptions.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
				pluginOptions.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOptionsMenu(v, plugin);
					}
				});
				view.setTag(plugin);
			}

			pluginLogo.setContentDescription(getString(logoContDescId));
			if (active) {
				pluginLogo.setBackgroundResource(nightMode ? R.drawable.bg_plugin_logo_enabled_dark : R.drawable.bg_plugin_logo_enabled_light);
			} else {
				TypedArray attributes = context.getTheme().obtainStyledAttributes(new int[]{R.attr.bg_plugin_logo_disabled});
				pluginLogo.setBackgroundDrawable(attributes.getDrawable(0));
				attributes.recycle();
			}

			TextView pluginName = view.findViewById(R.id.plugin_name);
			pluginName.setText(name);
			pluginName.setContentDescription(name + " " + getString(active
					? R.string.item_checked
					: R.string.item_unchecked));

			return view;
		}
	}

	private void showOptionsMenu(View view, final OsmandPlugin plugin) {
		final PopupMenu optionsMenu = new PopupMenu(view.getContext(), view);
		if (plugin.isActive() || !plugin.needsInstallation()) {
			MenuItem enableDisableItem = optionsMenu.getMenu().add(
					plugin.isActive() ? R.string.shared_string_disable
							: R.string.shared_string_enable);
			enableDisableItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					enableDisablePlugin(plugin, !plugin.isActive());
					optionsMenu.dismiss();
					return true;
				}
			});
		}

		final SettingsScreenType settingsScreenType = plugin.getSettingsScreenType();
		if (settingsScreenType != null && plugin.isActive()) {
			MenuItem settingsItem = optionsMenu.getMenu().add(R.string.shared_string_settings);
			settingsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						BaseSettingsFragment.showInstance(activity, settingsScreenType);
					}
					optionsMenu.dismiss();
					return true;
				}
			});
		}

		if (plugin instanceof CustomOsmandPlugin) {
			MenuItem settingsItem = optionsMenu.getMenu().add(R.string.shared_string_delete);
			settingsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					showDeletePluginDialog((CustomOsmandPlugin) plugin);
					optionsMenu.dismiss();
					return true;
				}
			});
		}

		optionsMenu.show();
	}

	private void showDeletePluginDialog(final CustomOsmandPlugin plugin) {
		Context context = getContext();
		if (context != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(getString(R.string.delete_confirmation_msg, plugin.getName()));
			builder.setMessage(R.string.are_you_sure);
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					OsmandPlugin.removeCustomPlugin(app, plugin);
					adapter.remove(plugin);
				}
			});
			builder.show();
		}
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

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			PluginsFragment fragment = new PluginsFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}