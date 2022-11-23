package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
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
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.SETTINGS;
import static net.osmand.plus.utils.UiUtilities.setupDialogButton;

public class BackupAuthorizationFragment extends BaseSettingsFragment implements InAppPurchaseListener {

	private static final String AUTHORIZE = "authorize";

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), isNightMode());
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);
		View subtitle = view.findViewById(R.id.toolbar_subtitle);
		AndroidUiHelper.updateVisibility(subtitle, false);
		view.findViewById(R.id.appbar).setOutlineProvider(null);
	}

	@Override
	protected void setupPreferences() {
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		String prefId = preference.getKey();
		if (AUTHORIZE.equals(prefId)) {
			bindAuthorizePref(holder);
		}
		super.onBindPreferenceViewHolder(preference, holder);
	}


	private void bindAuthorizePref(PreferenceViewHolder holder) {
		View signUpButton = holder.itemView.findViewById(R.id.sign_up_button);
		View signInButton = holder.itemView.findViewById(R.id.sign_in_button);

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
			setupDialogButton(isNightMode(), signUpButton, DialogButtonType.PRIMARY, R.string.shared_string_get);
		}
		setupAuthorizeButton(signInButton, DialogButtonType.SECONDARY, R.string.register_opr_have_account, false);
	}

	private void setupAuthorizeButton(View view, DialogButtonType buttonType, @StringRes int textId, boolean signUp) {
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

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);
		return recyclerView;
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