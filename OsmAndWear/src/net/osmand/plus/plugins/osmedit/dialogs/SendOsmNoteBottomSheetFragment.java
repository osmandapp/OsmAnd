package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.plus.plugins.osmedit.asynctasks.ValidateOsmLoginDetailsTask.ValidateOsmLoginListener;
import static net.osmand.plus.plugins.osmedit.dialogs.SendGpxBottomSheetFragment.showOpenStreetMapScreen;
import static net.osmand.plus.plugins.osmedit.dialogs.SendPoiBottomSheetFragment.OPENSTREETMAP_POINT;

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

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.fragments.DashOsmEditsFragment;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class SendOsmNoteBottomSheetFragment extends MenuBottomSheetDialogFragment
		implements ValidateOsmLoginListener, OsmAuthorizationListener {

	public static final String TAG = SendOsmNoteBottomSheetFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SendOsmNoteBottomSheetFragment.class);
	private OsmPoint[] poi;

	protected OsmandSettings settings;
	private TextView accountName;
	private LinearLayout accountBlockView;
	private LinearLayout signInView;
	private SwitchCompat uploadAnonymously;
	private OsmandApplication app;
	private OsmEditingPlugin plugin;
	private EditText noteText;

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(plugin.OSM_USER_DISPLAY_NAME.get());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = getMyApplication();
		plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (app == null || plugin == null) return;

		poi = AndroidUtils.getSerializable(getArguments(), OPENSTREETMAP_POINT, OsmPoint[].class);

		items.add(new TitleItem(getString(R.string.upload_osm_note)));

		View sendOsmNoteView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.send_osm_note_fragment, null);
		sendOsmNoteView.getViewTreeObserver().addOnGlobalLayoutListener(getShadowLayoutListener());

		noteText = sendOsmNoteView.findViewById(R.id.note_text);
		noteText.setText(((OsmNotesPoint) poi[0]).getText());
		noteText.setSelection(noteText.getText().length());
		if (noteText.requestFocus() && getActivity() != null) {
			AndroidUtils.showSoftKeyboard(getActivity(), noteText);
		}
		TextInputLayout noteHint = sendOsmNoteView.findViewById(R.id.note_hint);
		noteHint.setHint(AndroidUtils.addColon(app, R.string.osn_bug_name));
		accountBlockView = sendOsmNoteView.findViewById(R.id.account_container);
		signInView = sendOsmNoteView.findViewById(R.id.sign_in_container);
		uploadAnonymously = sendOsmNoteView.findViewById(R.id.upload_anonymously_switch);
		accountName = sendOsmNoteView.findViewById(R.id.user_name);
		updateAccountName();
		View signInButton = sendOsmNoteView.findViewById(R.id.sign_in_button);
		setupButtonIcon(signInButton, R.drawable.ic_action_openstreetmap_logo);
		signInButton.setOnClickListener(v -> {
			Fragment fragment = getParentFragment();
			if (fragment instanceof OsmAuthorizationListener) {
				app.getOsmOAuthHelper().addListener((OsmAuthorizationListener) fragment);
			}
			app.getOsmOAuthHelper().startOAuth((ViewGroup) getView(), nightMode);
		});
		updateSignIn(uploadAnonymously.isChecked());
		uploadAnonymously.setBackgroundResource(nightMode ? R.drawable.layout_bg_dark : R.drawable.layout_bg);
		int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		uploadAnonymously.setPadding(paddingSmall, 0, paddingSmall, 0);
		uploadAnonymously.setOnCheckedChangeListener((buttonView, isChecked) -> {
			updateSignIn(isChecked);
			if (nightMode) {
				uploadAnonymously.setBackgroundResource(
						isChecked ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
			} else {
				uploadAnonymously.setBackgroundResource(
						isChecked ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
			}
			uploadAnonymously.setPadding(paddingSmall, 0, paddingSmall, 0);
		});
		LinearLayout account = accountBlockView.findViewById(R.id.account_container);
		account.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showOpenStreetMapScreen(activity);
			}
			dismiss();
		});
		SimpleBottomSheetItem bottomSheetItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmNoteView)
				.create();
		items.add(bottomSheetItem);
	}

	private void updateAccountName() {
		String userNameOAuth = plugin.OSM_USER_DISPLAY_NAME.get();
		String userNameOpenID = plugin.OSM_USER_NAME_OR_EMAIL.get();
		String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
		accountName.setText(userName);
		updateSignIn(uploadAnonymously.isChecked());
	}

	private void updateSignIn(boolean isChecked) {
		boolean isLogged = isLogged();
		accountBlockView.setVisibility(isChecked || !isLogged ? View.GONE : View.VISIBLE);
		signInView.setVisibility(isChecked || isLogged ? View.GONE : View.VISIBLE);
	}

	private void setupButtonIcon(View buttonView, int drawableId) {
		Drawable icon = null;
		if (drawableId != -1) {
			icon = app.getUIUtilities().getIcon(drawableId, R.color.popup_text_color);
		}
		TextView buttonText = buttonView.findViewById(R.id.button_text);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(buttonText, icon, null, null, null);
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
				|| !Algorithms.isEmpty(plugin.OSM_USER_NAME_OR_EMAIL.get())
				&& !Algorithms.isEmpty(plugin.OSM_USER_PASSWORD.get());
	}
}
