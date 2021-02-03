package net.osmand.plus.osmedit.dialogs;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.osmedit.DashOsmEditsFragment;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.OsmLoginDataBottomSheet;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.plus.UiUtilities.setupDialogButton;
import static net.osmand.plus.osmedit.OsmEditingFragment.OSM_LOGIN_DATA;
import static net.osmand.plus.osmedit.ValidateOsmLoginDetailsTask.ValidateOsmLoginListener;
import static net.osmand.plus.osmedit.dialogs.SendGpxBottomSheetFragment.showOpenStreetMapScreen;
import static net.osmand.plus.osmedit.dialogs.SendPoiBottomSheetFragment.OPENSTREETMAP_POINT;

public class SendOsmNoteBottomSheetFragment extends MenuBottomSheetDialogFragment implements ValidateOsmLoginListener,
		OsmAuthorizationListener {

	public static final String TAG = SendOsmNoteBottomSheetFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SendOsmNoteBottomSheetFragment.class);
	private OsmPoint[] poi;

	protected OsmandSettings settings;
	private TextView accountName;
	private LinearLayout accountBlockView;
	private LinearLayout signInView;
	private SwitchCompat uploadAnonymously;
	private OsmandApplication app;
	private EditText noteText;

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(settings.OSM_USER_DISPLAY_NAME.get());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = getMyApplication();
		if (app == null) {
			return;
		}
		settings = app.getSettings();
		poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);

		items.add(new TitleItem(getString(R.string.upload_osm_note)));

		final View sendOsmNoteView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.send_osm_note_fragment, null);
		sendOsmNoteView.getViewTreeObserver().addOnGlobalLayoutListener(getShadowLayoutListener());

		noteText = sendOsmNoteView.findViewById(R.id.note_text);
		noteText.setText(((OsmNotesPoint) poi[0]).getText());
		noteText.setSelection(noteText.getText().length());
		TextInputLayout noteHint = sendOsmNoteView.findViewById(R.id.note_hint);
		noteHint.setHint(AndroidUtils.addColon(app, R.string.osn_bug_name));
		accountBlockView = sendOsmNoteView.findViewById(R.id.account_container);
		signInView = sendOsmNoteView.findViewById(R.id.sign_in_container);
		uploadAnonymously = sendOsmNoteView.findViewById(R.id.upload_anonymously_switch);
		accountName = sendOsmNoteView.findViewById(R.id.user_name);
		updateAccountName();
		View signInButton = sendOsmNoteView.findViewById(R.id.sign_in_button);
		setupButton(signInButton, R.string.sign_in_with_open_street_map, DialogButtonType.PRIMARY,
				R.drawable.ic_action_openstreetmap_logo);
		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = getParentFragment();
				if (fragment instanceof OsmAuthorizationListener) {
					app.getOsmOAuthHelper().addListener((OsmAuthorizationListener) fragment);
				}
				app.getOsmOAuthHelper().startOAuth((ViewGroup) getView(), nightMode);
			}
		});
		View loginButton = sendOsmNoteView.findViewById(R.id.login_button);
		setupButton(loginButton, R.string.use_login_password, DialogButtonType.SECONDARY, -1);
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					OsmLoginDataBottomSheet.showInstance(fragmentManager, OSM_LOGIN_DATA,
							SendOsmNoteBottomSheetFragment.this, usedOnMap, null);
				}
			}
		});
		updateSignIn(uploadAnonymously.isChecked());
		uploadAnonymously.setBackgroundResource(nightMode ? R.drawable.layout_bg_dark : R.drawable.layout_bg);
		final int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		uploadAnonymously.setPadding(paddingSmall, 0, paddingSmall, 0);
		uploadAnonymously.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateSignIn(isChecked);
				if (nightMode) {
					uploadAnonymously.setBackgroundResource(
							isChecked ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
				} else {
					uploadAnonymously.setBackgroundResource(
							isChecked ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
				}
				uploadAnonymously.setPadding(paddingSmall, 0, paddingSmall, 0);
			}
		});
		LinearLayout account = accountBlockView.findViewById(R.id.account_container);
		account.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					showOpenStreetMapScreen(activity);
				}
				dismiss();
			}
		});
		final SimpleBottomSheetItem bottomSheetItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmNoteView)
				.create();
		items.add(bottomSheetItem);
	}

	private void updateAccountName() {
		String userNameOAuth = settings.OSM_USER_DISPLAY_NAME.get();
		String userNameOpenID = settings.OSM_USER_NAME.get();
		String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
		accountName.setText(userName);
		updateSignIn(uploadAnonymously.isChecked());
	}

	private void updateSignIn(boolean isChecked) {
		boolean isLogged = isLogged();
		accountBlockView.setVisibility(isChecked || !isLogged ? View.GONE : View.VISIBLE);
		signInView.setVisibility(isChecked || isLogged ? View.GONE : View.VISIBLE);
	}

	private void setupButton(View buttonView, int buttonTextId, DialogButtonType buttonType, int drawableId) {
		Drawable icon = null;
		if (drawableId != -1) {
			icon = app.getUIUtilities().getIcon(drawableId, R.color.popup_text_color);
		}
		TextView buttonText = buttonView.findViewById(R.id.button_text);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(buttonText, icon, null, null, null);
		setupDialogButton(nightMode, buttonView, buttonType, buttonTextId);
	}

	public static void showInstance(@NonNull FragmentManager fm, @NonNull OsmPoint[] points) {
		try {
			if (!fm.isStateSaved()) {
				SendOsmNoteBottomSheetFragment fragment = new SendOsmNoteBottomSheetFragment();
				Bundle bundle = new Bundle();
				bundle.putSerializable(OPENSTREETMAP_POINT, points);
				fragment.setArguments(bundle);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return (DialogButtonType.PRIMARY);
	}

	@Override
	protected void onRightBottomButtonClick() {
		ProgressDialogPoiUploader progressDialogPoiUploader = null;
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			if (getParentFragment() instanceof DashOsmEditsFragment) {
				progressDialogPoiUploader = (ProgressDialogPoiUploader) getParentFragment();
			} else {
				progressDialogPoiUploader = new SimpleProgressDialogPoiUploader((MapActivity) activity);
			}
		} else if (getParentFragment() instanceof ProgressDialogPoiUploader) {
			progressDialogPoiUploader = (ProgressDialogPoiUploader) getParentFragment();
		}
		if (progressDialogPoiUploader != null) {
			((OsmNotesPoint) poi[0]).setText(noteText.getText().toString());
			progressDialogPoiUploader.showProgressDialog(poi, false, uploadAnonymously.isChecked());
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}

	@Override
	public void authorizationCompleted() {
		updateAccountName();
	}

	@Override
	public void loginValidationFinished(String warning) {
		updateAccountName();
	}

	private boolean isLogged() {
		OsmOAuthAuthorizationAdapter adapter = app.getOsmOAuthHelper().getAuthorizationAdapter();
		return adapter.isValidToken()
				|| !Algorithms.isEmpty(settings.OSM_USER_NAME.get())
				&& !Algorithms.isEmpty(settings.OSM_USER_PASSWORD.get());
	}
}
