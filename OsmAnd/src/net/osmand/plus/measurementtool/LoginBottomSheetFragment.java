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
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;

import org.apache.commons.logging.Log;

import static net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;

public class LoginBottomSheetFragment extends MenuBottomSheetDialogFragment implements OsmAuthorizationListener {

	public static final String TAG = LoginBottomSheetFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LoginBottomSheetFragment.class);

	private OsmOAuthHelper osmOAuthHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		return R.string.sign_in_with_open_street_map;
	}

	@Override
	protected void setupRightButton() {
		super.setupRightButton();
		Drawable icon = getIcon(R.drawable.ic_action_openstreetmap_logo, R.color.popup_text_color);
		TextView buttonText = rightButton.findViewById(R.id.button_text);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(buttonText, icon, null, null, null);
	}

	@Override
	public int getFirstDividerHeight() {
		return getDimensionPixelSize(R.dimen.card_content_padding_large);
	}

	@Override
	protected void onRightBottomButtonClick() {
		View view = getView();
		if (view != null) {
			if (getMapActivity() == null && getTargetFragment() instanceof OsmAuthorizationListener l) {
				osmOAuthHelper.addListener(l);
			}
			osmOAuthHelper.startOAuth((ViewGroup) view, nightMode);
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			LoginBottomSheetFragment fragment = new LoginBottomSheetFragment();
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void authorizationCompleted() {
		dismissAllowingStateLoss();
	}
}
