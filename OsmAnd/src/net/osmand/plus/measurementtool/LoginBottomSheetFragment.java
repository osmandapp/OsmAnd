package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.settings.bottomsheets.OsmLoginDataBottomSheet;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class LoginBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = LoginBottomSheetFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(LoginBottomSheetFragment.class);
	private static final String OSM_LOGIN_DATA = "osm_login_data";

	private OsmOAuthAuthorizationAdapter authorizationAdapter;

    @Override
    public void createMenuItems(Bundle savedInstanceState) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectAll()
				.penaltyLog()
				.build());

        OsmandApplication app = requiredMyApplication();
        authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
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
		return R.string.sing_in_with_open_street_map;
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
			authorizationAdapter.startOAuth((ViewGroup) view);
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

	public void authorize(String oauthVerifier) {
		if (authorizationAdapter != null) {
			authorizationAdapter.authorize(oauthVerifier);
			updateUserName();
		}
		Fragment target = getTargetFragment();
		if (target instanceof OsmAuthorizationListener) {
			((OsmAuthorizationListener) target).authorizationCompleted();
		}
		dismiss();
	}

	private void updateUserName() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			String userName = "";
			try {
				userName = authorizationAdapter.getUserName();
			} catch (InterruptedException e) {
				log.error(e);
			} catch (ExecutionException e) {
				log.error(e);
			} catch (IOException e) {
				log.error(e);
			} catch (XmlPullParserException e) {
				log.error(e);
			}
			app.getSettings().USER_DISPLAY_NAME.set(userName);
		}
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}
