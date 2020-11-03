package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.OsmLoginDataBottomSheet;

public class LoginBottomSheetFragment extends MenuBottomSheetDialogFragment {

    private ApplicationMode appMode;
    private OsmOAuthAuthorizationAdapter client;
    private static final String OSM_LOGIN_DATA = "osm_login_data";

    public static final String TAG = LoginBottomSheetFragment.class.getSimpleName();

    @Override
    public void createMenuItems(Bundle savedInstanceState) {
        items.add(new ShortDescriptionItem.Builder()
                .setDescription(getString(R.string.open_street_map_login_mode))
                .setTitle(getString(R.string.login_open_street_map_org))
                .setLayoutId(R.layout.bottom_sheet_login)
                .create());

        items.add(new DividerSpaceItem(getContext(),
                getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_margin)));

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
        TextView textViewIcon = thirdButton.findViewById(R.id.button_text);
        Drawable drawable = getResources().getDrawable(R.drawable.ic_action_openstreetmap_logo);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, getResources().getColor(R.color.popup_text_color));
        AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
                textViewIcon,
                drawable,
                null,
                null,
                null);
        textViewIcon.setCompoundDrawablePadding(AndroidUtils.dpToPx(getActivity(), 8f));
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
    public int getSecondDividerHeight() {
        return getResources().getDimensionPixelSize(R.dimen.content_padding_small);
    }

    public ApplicationMode getSelectedAppMode() {
        return appMode;
    }

    @Override
    protected void onRightBottomButtonClick() {
        ApplicationMode appMode = getSelectedAppMode();
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            OsmLoginDataBottomSheet.showInstance(fragmentManager, OSM_LOGIN_DATA, getTargetFragment(), false, appMode);
        }
        dismiss();
    }

    public OsmOAuthAuthorizationAdapter getClient() {
        return client;
    }

    @Override
    protected void onThirdBottomButtonClick() {
        View view = getView();
        client = new OsmOAuthAuthorizationAdapter(getMyApplication());
        if (view != null) {
            client.startOAuth((ViewGroup) view);
        }
    }

    @Override
    protected DialogButtonType getThirdBottomButtonType() {
        return (DialogButtonType.PRIMARY);
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

    public interface LoginOsmAutorizationListener {
        void informAutorizationPrefsUpdate();
    }

    public void authorize(String oauthVerifier) {
        if (client != null) {
            client.authorize(oauthVerifier);
        }
        Fragment target = getTargetFragment();
        if (target instanceof LoginOsmAutorizationListener) {
            ((LoginOsmAutorizationListener) target).informAutorizationPrefsUpdate();
        }
        dismiss();
    }
}


