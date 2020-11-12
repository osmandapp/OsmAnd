package net.osmand.plus.osmedit.dialogs;

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
import net.osmand.plus.osmedit.OpenstreetmapPoint;
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
import static net.osmand.plus.osmedit.ValidateOsmLoginDetailsTask.*;
import static net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.*;

public class SendOsmNoteBottomSheetFragment extends MenuBottomSheetDialogFragment implements ValidateOsmLoginListener,
		OsmAuthorizationListener {

	public static final String TAG = SendOsmNoteBottomSheetFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SendOsmNoteBottomSheetFragment.class);
	public static final String OPENSTREETMAP_POINT = "openstreetmap_point";
	public static final String POI_UPLOADER_TYPE = "poi_uploader_type";
	private OsmPoint[] poi;

	protected OsmandSettings settings;
	private TextView accountName;
	private LinearLayout accountBlockView;
	private LinearLayout signInView;
	private SwitchCompat uploadAnonymously;

	public enum PoiUploaderType {
		SIMPLE,
		FRAGMENT
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(getMyApplication().getSettings().USER_DISPLAY_NAME.get());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);
		OsmandApplication app = getMyApplication();

		items.add(new TitleItem(getString(R.string.upload_osm_note)));

		final View sendOsmNoteView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.send_osm_note_fragment, null);

		TextView noteText = sendOsmNoteView.findViewById(R.id.note_text);
		noteText.setText(((OsmNotesPoint) poi[0]).getText());
		TextInputLayout noteHint = sendOsmNoteView.findViewById(R.id.note_hint);
		noteHint.setHint(AndroidUtils.addColon(app, R.string.osn_bug_name));
		accountBlockView = sendOsmNoteView.findViewById(R.id.account_container);
		signInView = sendOsmNoteView.findViewById(R.id.sign_in_container);
		uploadAnonymously = sendOsmNoteView.findViewById(R.id.upload_anonymously_switch);
		accountName = sendOsmNoteView.findViewById(R.id.user_name);

		settings = app.getSettings();
		updateAccountName();
		View signInButton = sendOsmNoteView.findViewById(R.id.sign_in_button);
		setupButton(signInButton, R.string.sing_in_with_open_street_map, DialogButtonType.PRIMARY,
				R.drawable.ic_action_openstreetmap_logo);
		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = requiredMyApplication();
				app.getOsmOAuthHelper().startOAuth((ViewGroup) v);
			}
		});
		View loginButton = sendOsmNoteView.findViewById(R.id.login_button);
		setupButton(loginButton, R.string.use_login_password, DialogButtonType.SECONDARY, -1);
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmLoginDataBottomSheet.showInstance(getFragmentManager(), OSM_LOGIN_DATA,
						SendOsmNoteBottomSheetFragment.this, usedOnMap, null);
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
		final SimpleBottomSheetItem bottomSheetItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmNoteView)
				.create();
		items.add(bottomSheetItem);
	}

	private void updateAccountName() {
		String userNameOAuth = settings.USER_DISPLAY_NAME.get();
		String userNameOpenID = settings.USER_NAME.get();
		String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
		accountName.setText(userName);
		updateSignIn(uploadAnonymously.isChecked());
	}

	private void updateSignIn(boolean isChecked) {
		boolean isLogin = isLogin();
		accountBlockView.setVisibility(isChecked || !isLogin ? View.GONE : View.VISIBLE);
		signInView.setVisibility(isChecked || isLogin ? View.GONE : View.VISIBLE);
	}

	private void setupButton(View buttonView, int buttonTextId, DialogButtonType buttonType, int drawableId) {
		Drawable icon = null;
		if (drawableId != -1) {
			icon = getMyApplication().getUIUtilities().getIcon(drawableId, R.color.popup_text_color);
		}
		TextView buttonText = buttonView.findViewById(R.id.button_text);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(buttonText, icon, null, null, null);
		setupDialogButton(nightMode, buttonView, buttonType, buttonTextId);
	}

	public static void showInstance(@NonNull FragmentManager fm, @NonNull OsmPoint[] points,
	                                @NonNull PoiUploaderType uploaderType) {
		try {
			if (!fm.isStateSaved()) {
				SendOsmNoteBottomSheetFragment fragment = new SendOsmNoteBottomSheetFragment();
				Bundle bundle = new Bundle();
				bundle.putSerializable(OPENSTREETMAP_POINT, points);
				bundle.putString(POI_UPLOADER_TYPE, uploaderType.name());
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
		View view = getView();
		boolean hasPoiGroup = false;
		assert poi != null;
		for (OsmPoint p : poi) {
			if (p.getGroup() == OsmPoint.Group.POI) {
				hasPoiGroup = true;
				break;
			}
		}
		final boolean hasPOI = hasPoiGroup;
		final SwitchCompat uploadAnonymously = (SwitchCompat) view.findViewById(R.id.upload_anonymously_switch);
		final EditText messageEditText = (EditText) view.findViewById(R.id.message_field);
		final SendPoiDialogFragment.PoiUploaderType poiUploaderType = SendPoiDialogFragment.PoiUploaderType.valueOf(getArguments().getString(POI_UPLOADER_TYPE, SendPoiDialogFragment.PoiUploaderType.SIMPLE.name()));
		final ProgressDialogPoiUploader progressDialogPoiUploader;
		if (poiUploaderType == SendPoiDialogFragment.PoiUploaderType.SIMPLE && getActivity() instanceof MapActivity) {
			progressDialogPoiUploader = new SimpleProgressDialogPoiUploader((MapActivity) getActivity());
		} else {
			progressDialogPoiUploader = (ProgressDialogPoiUploader) getParentFragment();
		}
		if (progressDialogPoiUploader != null) {
			String comment = messageEditText.getText().toString();
			if (comment.length() > 0) {
				for (OsmPoint osmPoint : poi) {
					if (osmPoint.getGroup() == OsmPoint.Group.POI) {
						((OpenstreetmapPoint) osmPoint).setComment(comment);
						break;
					}
				}
			}
			progressDialogPoiUploader.showProgressDialog(poi,
					false,
					!hasPOI && uploadAnonymously.isChecked());
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

	private boolean isLogin() {
		OsmandApplication app = getMyApplication();
		OsmandSettings settings = app.getSettings();
		OsmOAuthAuthorizationAdapter adapter = app.getOsmOAuthHelper().getAuthorizationAdapter();
		return adapter.isValidToken()
				|| !Algorithms.isEmpty(settings.USER_NAME.get())
				&& !Algorithms.isEmpty(settings.USER_PASSWORD.get());
	}
}
