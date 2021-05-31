package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment.ChoosePlanDialogType;
import net.osmand.plus.dialogs.PluginInstalledBottomSheetDialog.PluginStateListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import org.apache.commons.logging.Log;

public class PluginInfoFragment extends BaseOsmAndFragment implements PluginStateListener {

	private static final Log log = PlatformUtil.getLog(PluginInfoFragment.class);

	private static final String TAG = PluginInfoFragment.class.getName();

	public static final String EXTRA_PLUGIN_ID = "plugin_id";
	public static final String PLUGIN_INFO = "plugin_info";

	private OsmandPlugin plugin;
	private OsmandApplication app;

	private View mainView;
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
				dismiss();
			}
		});
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		app = requireMyApplication();

		Bundle args = getArguments();
		if (args == null || !args.containsKey(EXTRA_PLUGIN_ID)) {
			log.error("Required extra '" + EXTRA_PLUGIN_ID + "' is missing");
			return null;
		}
		String pluginId = args.getString(EXTRA_PLUGIN_ID);
		if (pluginId == null) {
			log.error("Extra '" + EXTRA_PLUGIN_ID + "' is null");
			return null;
		}
		plugin = OsmandPlugin.getPlugin(pluginId);
		if (plugin == null) {
			log.error("Plugin '" + EXTRA_PLUGIN_ID + "' not found");
			return null;
		}

		Context context = requireContext();
		nightMode = !app.getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		mainView = themedInflater.inflate(R.layout.plugin, container, false);
		AndroidUtils.addStatusBarPadding21v(context, mainView);

		TextView toolbarTitle = mainView.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(plugin.getName());

		ImageView closeButton = mainView.findViewById(R.id.close_button);
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

		Drawable pluginImage = plugin.getAssetResourceImage();
		if (pluginImage != null) {
			ImageView img = mainView.findViewById(R.id.plugin_image);
			img.setImageDrawable(pluginImage);
		} else {
			mainView.findViewById(R.id.plugin_image_placeholder).setVisibility(View.VISIBLE);
		}

		TextView descriptionView = mainView.findViewById(R.id.plugin_description);
		descriptionView.setText(plugin.getDescription());

		int linkTextColorId = nightMode ? R.color.ctx_menu_bottom_view_url_color_dark : R.color.ctx_menu_bottom_view_url_color_light;
		int linkTextColor = ContextCompat.getColor(context, linkTextColorId);

		descriptionView.setLinkTextColor(linkTextColor);
		descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
		AndroidUtils.removeLinkUnderline(descriptionView);

		Button settingsButton = mainView.findViewById(R.id.plugin_settings);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					SettingsScreenType settingsScreenType = plugin.getSettingsScreenType();
					if (settingsScreenType != null) {
						Bundle args = new Bundle();
						args.putBoolean(PLUGIN_INFO, true);
						BaseSettingsFragment.showInstance(activity, settingsScreenType, null, args, null);
					}
				}
			}
		});

		CompoundButton enableDisableButton = mainView.findViewById(R.id.plugin_enable_disable);
		enableDisableButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (plugin.isActive() == isChecked) {
					return;
				}

				boolean ok = OsmandPlugin.enablePlugin(getActivity(), app, plugin, isChecked);
				if (!ok) {
					return;
				}
				updateState();
			}
		});
		Button getButton = mainView.findViewById(R.id.plugin_get);
		getButton.setText(plugin.isPaid() ? R.string.get_plugin : R.string.shared_string_install);
		getButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (plugin instanceof SRTMPlugin) {
						FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
						if (fragmentManager != null) {
							ChoosePlanDialogFragment.showDialogInstance(app, fragmentManager, ChoosePlanDialogType.HILLSHADE_SRTM_PLUGIN);
						}
					} else {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
					}
				} catch (Exception e) {
					//ignored
				}
			}
		});

		updateState();
		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		OsmandPlugin.checkInstalledMarketPlugins(app, getActivity());
		updateState();
	}

	private void updateState() {
		CompoundButton enableDisableButton = mainView.findViewById(R.id.plugin_enable_disable);
		Button getButton = mainView.findViewById(R.id.plugin_get);
		Button settingsButton = mainView.findViewById(R.id.plugin_settings);
		settingsButton.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_settings), null, null, null);
		View installHeader = mainView.findViewById(R.id.plugin_install_header);

		if (plugin.needsInstallation()) {
			getButton.setVisibility(View.VISIBLE);
			enableDisableButton.setVisibility(View.GONE);
			settingsButton.setVisibility(View.GONE);
			installHeader.setVisibility(View.VISIBLE);
			View worldGlobeIcon = installHeader.findViewById(R.id.ic_world_globe);
			Drawable worldGlobeDrawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_world_globe_dark);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				worldGlobeIcon.setBackground(worldGlobeDrawable);
			} else {
				worldGlobeIcon.setBackgroundDrawable(worldGlobeDrawable);
			}
		} else {
			getButton.setVisibility(View.GONE);
			enableDisableButton.setVisibility(View.VISIBLE);
			enableDisableButton.setChecked(plugin.isActive());

			if (plugin.getSettingsScreenType() == null || !plugin.isActive()) {
				settingsButton.setVisibility(View.GONE);
			} else {
				settingsButton.setVisibility(View.VISIBLE);
			}
			installHeader.setVisibility(View.GONE);
		}
	}

	@Override
	public void onPluginStateChanged(OsmandPlugin plugin) {
		updateState();
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				log.error(e);
			}
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager, OsmandPlugin plugin) {
		try {
			Bundle args = new Bundle();
			args.putString(EXTRA_PLUGIN_ID, plugin.getId());

			PluginInfoFragment fragment = new PluginInfoFragment();
			fragment.setArguments(args);
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