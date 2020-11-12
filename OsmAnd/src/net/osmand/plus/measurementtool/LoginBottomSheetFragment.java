package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.settings.bottomsheets.OsmLoginDataBottomSheet;

import org.apache.commons.logging.Log;

import static net.osmand.plus.myplaces.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

public class LoginBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = LoginBottomSheetFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(LoginBottomSheetFragment.class);
	public static final String OSM_LOGIN_DATA = "osm_login_data";

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
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Bundle params = new Bundle();
			params.putBoolean(OSM_LOGIN_DATA, true);

			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, GPX_TAB);
			MapActivity.launchMapActivityMoveToTop(activity, bundle, null, params);
		}
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return (DialogButtonType.SECONDARY);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment) {
		if (!fragmentManager.isStateSaved()) {
			LoginBottomSheetFragment fragment = new LoginBottomSheetFragment();
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public void authorize() {
		Fragment target = getTargetFragment();
		if (target instanceof OsmAuthorizationListener) {
			((OsmAuthorizationListener) target).authorizationCompleted();
		}
		dismiss();
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}
