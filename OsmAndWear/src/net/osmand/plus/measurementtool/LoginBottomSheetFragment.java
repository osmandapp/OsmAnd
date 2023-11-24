package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.settings.bottomsheets.OsmLoginDataBottomSheet;

import org.apache.commons.logging.Log;

import static net.osmand.plus.plugins.osmedit.fragments.OsmEditingFragment.OSM_LOGIN_DATA;
import static net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;

public class LoginBottomSheetFragment extends MenuBottomSheetDialogFragment implements OsmAuthorizationListener {

	public static final String TAG = LoginBottomSheetFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LoginBottomSheetFragment.class);

	private OsmOAuthHelper osmOAuthHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = requiredMyApplication();
		osmOAuthHelper = app.getOsmOAuthHelper();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new SimpleBottomSheetItem.Builder().setLayoutId(R.layout.bottom_sheet_login).create());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.use_login_password;
	}

	@Override
	protected void setupThirdButton() {
		super.setupThirdButton();
		OsmandApplication app = getMyApplication();
		if (app != null) {
			Drawable icon = app.getUIUtilities().getIcon(R.drawable.ic_action_openstreetmap_logo, R.color.popup_text_color);
			TextView buttonText = thirdButton.findViewById(R.id.button_text);
			AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(buttonText, icon, null, null, null);
		}
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.sign_in_with_open_street_map;
	}

	@Override
	public int getFirstDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.card_content_padding_large);
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null) {
			OsmLoginDataBottomSheet.showInstance(fragmentManager, OSM_LOGIN_DATA, getTargetFragment(), usedOnMap, null);
		}
		dismiss();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		View view = getView();
		if (view != null) {
			Fragment fragment = getTargetFragment();
			if (!(getActivity() instanceof MapActivity) && fragment instanceof OsmAuthorizationListener) {
				osmOAuthHelper.addListener((OsmAuthorizationListener) fragment);
			}
			osmOAuthHelper.startOAuth((ViewGroup) view, nightMode);
		}
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return (DialogButtonType.SECONDARY);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment) {
		try {
			if (!fragmentManager.isStateSaved()) {
				LoginBottomSheetFragment fragment = new LoginBottomSheetFragment();
				fragment.setTargetFragment(targetFragment, 0);
				fragment.show(fragmentManager, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	public void authorizationCompleted() {
		dismiss();
	}
}
