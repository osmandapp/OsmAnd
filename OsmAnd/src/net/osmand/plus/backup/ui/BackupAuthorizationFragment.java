package net.osmand.plus.backup.ui;

import static net.osmand.plus.UiUtilities.setupDialogButton;
import static net.osmand.plus.importfiles.ImportHelper.ImportType.SETTINGS;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.ExportSettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class BackupAuthorizationFragment extends BaseSettingsFragment implements InAppPurchaseListener {

	private static final String AUTHORIZE = "authorize";
	private static final String LOCAL_BACKUP = "local_backup";
	private static final String BACKUP_TO_FILE = "backup_to_file";
	private static final String RESTORE_FROM_FILE = "restore_from_file";
	private static final String LOCAL_BACKUP_DIVIDER = "local_backup_divider";

	private boolean localBackupVisible = true;

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@ColorRes
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null && Build.VERSION.SDK_INT >= 23 && !isNightMode()) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);
		View subtitle = view.findViewById(R.id.toolbar_subtitle);
		AndroidUiHelper.updateVisibility(subtitle, false);
	}

	@Override
	protected void setupPreferences() {
		Preference localBackup = findPreference(LOCAL_BACKUP);
		localBackup.setIconSpaceReserved(false);

		setupBackupToFilePref();
		setupRestoreFromFilePref();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		String prefId = preference.getKey();
		if (LOCAL_BACKUP.equals(prefId)) {
			bindLocalBackupPref(holder);
		} else if (AUTHORIZE.equals(prefId)) {
			bindAuthorizePref(holder);
		}
		super.onBindPreferenceViewHolder(preference, holder);
	}

	private void setupBackupToFilePref() {
		Preference backupToFile = findPreference(BACKUP_TO_FILE);
		backupToFile.setIcon(getIcon(R.drawable.ic_action_save_to_file, getActiveColorRes()));
	}

	private void setupRestoreFromFilePref() {
		Preference restoreFromFile = findPreference(RESTORE_FROM_FILE);
		restoreFromFile.setIcon(getIcon(R.drawable.ic_action_read_from_file, getActiveColorRes()));
	}

	private void bindLocalBackupPref(PreferenceViewHolder holder) {
		ImageView indicator = holder.itemView.findViewById(R.id.icon_logout);
		if (!localBackupVisible) {
			indicator.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_down));
			indicator.setContentDescription(getString(R.string.access_collapsed_list));
		} else {
			indicator.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_up));
			indicator.setContentDescription(getString(R.string.access_expanded_list));
		}
	}

	private void bindAuthorizePref(PreferenceViewHolder holder) {
		View signUpButton = holder.itemView.findViewById(R.id.sign_up_button);
		View signInButton = holder.itemView.findViewById(R.id.sign_in_button);

		LinearLayout iconsContainer = (LinearLayout) holder.itemView.findViewById(R.id.icons_container);
		int width = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 1.2);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin =  - (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.02);
		iconsContainer.setLayoutParams(params);
		fillIconsContainer(iconsContainer);

		boolean subscribed = InAppPurchaseHelper.isOsmAndProAvailable(app);
		if (subscribed) {
			setupAuthorizeButton(signUpButton, DialogButtonType.PRIMARY, R.string.register_opr_create_new_account, true);
		} else {
			signUpButton.setOnClickListener(v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.OSMAND_CLOUD);
				}
			});
			setupDialogButton(isNightMode(), signUpButton, DialogButtonType.PRIMARY, R.string.get_plugin);
		}
		setupAuthorizeButton(signInButton, DialogButtonType.SECONDARY, R.string.register_opr_have_account, false);
	}
	private void fillIconsContainer(LinearLayout view) {
		List<Integer> yellowIcons = new ArrayList<>();
		yellowIcons.add(R.drawable.ic_action_photo);
		yellowIcons.add(R.drawable.ic_action_favorite);
		yellowIcons.add(R.drawable.ic_action_micro_dark);
		yellowIcons.add(R.drawable.ic_notification_track);
		yellowIcons.add(R.drawable.ic_type_video);
		yellowIcons.add(R.drawable.ic_action_info_dark);
		yellowIcons.add(R.drawable.ic_action_openstreetmap_logo);
		yellowIcons.add(R.drawable.ic_action_flag);

		List<Integer> blueIcons = new ArrayList<>();
		blueIcons.add(R.drawable.ic_map);
		blueIcons.add(R.drawable.ic_action_settings);
		blueIcons.add(R.drawable.ic_layer_top);
		blueIcons.add(R.drawable.ic_plugin_srtm);
		blueIcons.add(R.drawable.ic_action_plan_route);
		blueIcons.add(R.drawable.ic_action_map_style);
		blueIcons.add(R.drawable.ic_action_file_routing);
		blueIcons.add(R.drawable.ic_action_hillshade_dark);

		List<Integer> greenIcons = new ArrayList<>();
		greenIcons.add(R.drawable.ic_action_gdirections_dark);
		greenIcons.add(R.drawable.ic_action_settings);
		greenIcons.add(R.drawable.ic_action_map_language);
		greenIcons.add(R.drawable.ic_action_car_dark);
		greenIcons.add(R.drawable.ic_action_pedestrian_dark);
		greenIcons.add(R.drawable.ic_action_volume_up);
		greenIcons.add(R.drawable.ic_action_sun);
		greenIcons.add(R.drawable.ic_action_ruler_unit);

		createIcons(view, yellowIcons, ContextCompat.getColor(view.getContext(),R.color.purchase_sc_toolbar_active_dark));
		createIcons(view, blueIcons, ContextCompat.getColor(view.getContext(),R.color.backup_restore_icons_blue));
		createIcons(view, greenIcons, ContextCompat.getColor(view.getContext(),R.color.purchase_save_discount));
	}

	private void createIcons(View view, List<Integer> icons, int color) {
		LinearLayout row = new LinearLayout(view.getContext());
		LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		if (color != ContextCompat.getColor(view.getContext(),R.color.backup_restore_icons_blue)){
			rowParams.leftMargin = AndroidUtils.dpToPx(view.getContext(), 25);
		}
		rowParams.bottomMargin = AndroidUtils.dpToPx(view.getContext(), 9);

		int ovalSize = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.1);
		int iconSize = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.067);

		for (Integer i:icons) {
			GradientDrawable oval = new GradientDrawable();
			oval.setShape(GradientDrawable.OVAL);
			oval.setStroke(AndroidUtils.dpToPx(view.getContext(), 1), color);
			oval.setSize(ovalSize, ovalSize);
			oval.setAlpha(51);

			LinearLayout container = new LinearLayout(view.getContext());
			LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			containerParams.gravity = Gravity.CENTER;
			containerParams.rightMargin = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.045);
			container.setBackground(oval);

			ImageView icon = new ImageView(view.getContext());
			icon.setImageResource(i);
			icon.setColorFilter(color);
			LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
			iconParams.leftMargin = iconSize / 4;
			iconParams.topMargin = iconSize / 4;

			container.addView(icon, iconParams);
			row.addView(container, containerParams);
		}
		((ViewGroup)view).addView(row, rowParams);
	}

	private void setupAuthorizeButton(View view, DialogButtonType buttonType, @StringRes int textId, final boolean signUp) {
		setupDialogButton(isNightMode(), view, buttonType, textId);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					AuthorizeFragment.showInstance(mapActivity.getSupportFragmentManager(), signUp);
				}
			}
		});
	}

	private void toggleLocalPrefsVisibility() {
		localBackupVisible = !localBackupVisible;
		findPreference(BACKUP_TO_FILE).setVisible(localBackupVisible);
		findPreference(RESTORE_FROM_FILE).setVisible(localBackupVisible);
		findPreference(LOCAL_BACKUP_DIVIDER).setVisible(localBackupVisible);
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);
		return recyclerView;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (BACKUP_TO_FILE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				ApplicationMode mode = getSelectedAppMode();
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				ExportSettingsFragment.showInstance(fragmentManager, mode, null, true);
				return true;
			}
		} else if (RESTORE_FROM_FILE.equals(prefId)) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getImportHelper().chooseFileToImport(SETTINGS, null);
				return true;
			}
		} else if (LOCAL_BACKUP.equals(prefId)) {
			toggleLocalPrefsVisibility();
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		String tag = SettingsScreenType.BACKUP_AUTHORIZATION.fragmentName;
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
			Fragment fragment = new BackupAuthorizationFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, tag)
					.addToBackStack(SettingsScreenType.BACKUP_AUTHORIZATION.name())
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {

	}

	@Override
	public void onGetItems() {

	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		updatePreference(findPreference(AUTHORIZE));
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {

	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {

	}
}