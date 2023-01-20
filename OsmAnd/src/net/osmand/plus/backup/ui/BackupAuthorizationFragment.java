package net.osmand.plus.backup.ui;

import static net.osmand.plus.utils.UiUtilities.setupDialogButton;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;

public class BackupAuthorizationFragment extends BaseOsmAndFragment implements InAppPurchaseListener {

	public static final String TAG = BackupAuthorizationFragment.class.getSimpleName();

	private OsmandApplication app;

	private View signUpButton;
	private View signInButton;

	private boolean nightMode;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = isNightMode(false);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_authorize_cloud, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		signUpButton = view.findViewById(R.id.sign_up_button);
		signInButton = view.findViewById(R.id.sign_in_button);

		updateButtons();
		setupToolbar(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		TextView title = view.findViewById(R.id.toolbar_title);
		title.setText(R.string.backup_and_restore);

		View subtitle = view.findViewById(R.id.toolbar_subtitle);
		AndroidUiHelper.updateVisibility(subtitle, false);

		ImageView closeButton = view.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.profile_button), false);
	}

	private void updateButtons() {
		boolean subscribed = InAppPurchaseHelper.isOsmAndProAvailable(app);
		if (subscribed) {
			setupAuthorizeButton(signUpButton, DialogButtonType.PRIMARY, R.string.register_opr_create_new_account, true);
		} else {
			signUpButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					ChoosePlanFragment.showInstance(activity, OsmAndFeature.OSMAND_CLOUD);
				}
			});
			setupDialogButton(nightMode, signUpButton, DialogButtonType.PRIMARY, R.string.shared_string_get);
		}
		setupAuthorizeButton(signInButton, DialogButtonType.SECONDARY, R.string.register_opr_have_account, false);
	}

	private void setupAuthorizeButton(View view, DialogButtonType buttonType, @StringRes int textId, boolean signUp) {
		setupDialogButton(nightMode, view, buttonType, textId);
		view.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AuthorizeFragment.showInstance(activity.getSupportFragmentManager(), signUp);
			}
		});
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		updateButtons();
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Fragment fragment = new BackupAuthorizationFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}