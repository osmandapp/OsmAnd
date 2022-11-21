package net.osmand.plus.plugins;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.plugins.PluginInstalledBottomSheetDialog.PluginStateListener;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

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
		return ColorUtilities.getStatusBarColorId(nightMode);
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
		plugin = getPluginFromArgs();
		if (plugin == null) {
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
		closeButton.setOnClickListener(v -> {
			Activity activity = getMyActivity();
			if (activity != null) {
				activity.onBackPressed();
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

		OsmAndAppCustomization customization = app.getAppCustomization();
		if (customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(descriptionView, Linkify.ALL)) {
			int linkTextColorId = nightMode ? R.color.ctx_menu_bottom_view_url_color_dark : R.color.ctx_menu_bottom_view_url_color_light;
			descriptionView.setLinkTextColor(ContextCompat.getColor(context, linkTextColorId));
			descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
			AndroidUtils.removeLinkUnderline(descriptionView);
		}
		Button settingsButton = mainView.findViewById(R.id.plugin_settings);
		settingsButton.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				SettingsScreenType settingsScreenType = plugin.getSettingsScreenType();
				if (settingsScreenType != null) {
					Bundle args = new Bundle();
					args.putBoolean(PLUGIN_INFO, true);
					BaseSettingsFragment.showInstance(activity, settingsScreenType, null, args, null);
				}
			}
		});

		CompoundButton enableDisableButton = mainView.findViewById(R.id.plugin_enable_disable);
		enableDisableButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (plugin.isEnabled() != isChecked) {
				if (PluginsHelper.enablePlugin(getActivity(), app, plugin, isChecked)) {
					updateState();

					Fragment target = getTargetFragment();
					if (target instanceof PluginStateListener) {
						((PluginStateListener) target).onPluginStateChanged(plugin);
					}
				}
			}
		});
		Button getButton = mainView.findViewById(R.id.plugin_get);
		getButton.setText(plugin.isPaid() ? R.string.shared_string_get : R.string.shared_string_install);
		getButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				OsmAndFeature feature = plugin.getOsmAndFeature();
				if (feature != null) {
					ChoosePlanFragment.showInstance(activity, feature);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL()));
					AndroidUtils.startActivityIfSafe(activity, intent);
				}
			}
		});

		updateState();
		return mainView;
	}

	@Nullable
	private OsmandPlugin getPluginFromArgs() {
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
		OsmandPlugin plugin = PluginsHelper.getPlugin(pluginId);
		if (plugin == null) {
			log.error("Plugin '" + EXTRA_PLUGIN_ID + "' not found");
			return null;
		}
		return plugin;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (plugin != null) {
			PluginsHelper.checkInstalledMarketPlugins(app, getActivity());
			updateState();
		} else {
			dismiss();
		}
	}

	private void updateState() {
		CompoundButton enableDisableButton = mainView.findViewById(R.id.plugin_enable_disable);
		Button getButton = mainView.findViewById(R.id.plugin_get);
		Button settingsButton = mainView.findViewById(R.id.plugin_settings);
		settingsButton.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_settings), null, null, null);
		View installHeader = mainView.findViewById(R.id.plugin_install_header);

		if (plugin.isLocked()) {
			getButton.setVisibility(View.VISIBLE);
			settingsButton.setVisibility(View.GONE);
			installHeader.setVisibility(View.VISIBLE);
			View worldGlobeIcon = installHeader.findViewById(R.id.ic_world_globe);
			Drawable worldGlobeDrawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_world_globe_dark);
			worldGlobeIcon.setBackground(worldGlobeDrawable);
		} else {
			getButton.setVisibility(View.GONE);

			if (plugin.getSettingsScreenType() == null || !plugin.isActive()) {
				settingsButton.setVisibility(View.GONE);
			} else {
				settingsButton.setVisibility(View.VISIBLE);
			}
			installHeader.setVisibility(View.GONE);
		}
		enableDisableButton.setChecked(plugin.isEnabled());
	}

	@Override
	public void onPluginStateChanged(@NonNull OsmandPlugin osmandPlugin) {
		if (Algorithms.stringsEqual(plugin.getId(), osmandPlugin.getId())) {
			updateState();
		}
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target, @NonNull OsmandPlugin plugin) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(EXTRA_PLUGIN_ID, plugin.getId());

			PluginInfoFragment fragment = new PluginInfoFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}