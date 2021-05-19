package net.osmand.plus.settings.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupHelper.OnRegisterDeviceListener;
import net.osmand.plus.backup.BackupHelper.OnRegisterUserListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.development.TestBackupActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED;

public class AuthorizeFragment extends BaseOsmAndFragment {

	private static final Log log = PlatformUtil.getLog(AuthorizeFragment.class);

	public static final String TAG = AuthorizeFragment.class.getSimpleName();

	private static final String OSMAND_EMAIL = "support@osmand.net";
	private static final String OSMAND_DOCS = "https://docs.osmand.net/en/main@latest/osmand/purchases/android";

	private static final String LOGIN_DIALOG_TYPE_KEY = "login_dialog_type_key";

	private static final int VERIFICATION_CODE_EXPIRATION_TIME_MIN = 10 * 60 * 1000; // 10 minutes

	private OsmandApplication app;
	private OsmandSettings settings;
	private BackupHelper backupHelper;

	private View mainView;
	private Toolbar toolbar;
	private ProgressBar progressBar;
	private TextView title;
	private TextView description;
	private View buttonContinue;

	private LoginDialogType dialogType = LoginDialogType.SIGN_UP;

	private long lastTimeCodeSent = 0;
	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		backupHelper = app.getBackupHelper();
		nightMode = !settings.isLightContent();

		if (savedInstanceState != null && savedInstanceState.containsKey(LOGIN_DIALOG_TYPE_KEY)) {
			dialogType = LoginDialogType.valueOf(savedInstanceState.getString(LOGIN_DIALOG_TYPE_KEY));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_cloud_authorize, container, false);
		AndroidUtils.addStatusBarPadding21v(app, view);

		title = view.findViewById(R.id.title);
		toolbar = view.findViewById(R.id.toolbar);
		mainView = view.findViewById(R.id.main_view);
		description = view.findViewById(R.id.description);
		progressBar = view.findViewById(R.id.progress_bar);
		buttonContinue = view.findViewById(R.id.continue_button);

		setupToolbar();
		setupTextWatchers();
		updateContent();
		setupSupportButton();

		UiUtilities.setupDialogButton(nightMode, buttonContinue, DialogButtonType.PRIMARY, R.string.shared_string_continue);

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(LOGIN_DIALOG_TYPE_KEY, dialogType.name());
		super.onSaveInstanceState(outState);
	}

	private void setupTextWatchers() {
		for (LoginDialogType type : LoginDialogType.values()) {
			View itemView = mainView.findViewById(type.viewId);
			EditText editText = itemView.findViewById(R.id.edit_text);
			editText.addTextChangedListener(getTextWatcher());
		}
	}

	private void setupToolbar() {
		AppBarLayout appBarLayout = mainView.findViewById(R.id.appbar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_help_online));
		actionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					WikipediaDialogFragment.showFullArticle(activity, Uri.parse(OSMAND_DOCS), nightMode);
				}
			}
		});
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_switch_container), false);
	}

	private void updateContent() {
		title.setText(dialogType.titleId);
		updateDescription();

		for (LoginDialogType type : LoginDialogType.values()) {
			View view = mainView.findViewById(type.viewId);

			if (dialogType == type) {
				if (dialogType == LoginDialogType.VERIFY_EMAIL) {
					setupVerifyEmailContainer(view);
				} else {
					if (dialogType == LoginDialogType.SIGN_IN) {
						setupAuthorizeContainer(view, dialogType, LoginDialogType.SIGN_UP);
					} else if (dialogType == LoginDialogType.SIGN_UP) {
						setupAuthorizeContainer(view, dialogType, LoginDialogType.SIGN_IN);
					}
				}
				AndroidUiHelper.updateVisibility(view, true);
			} else {
				AndroidUiHelper.updateVisibility(view, false);
			}
		}
	}

	private void setupAuthorizeContainer(View view, LoginDialogType currentType, LoginDialogType nextType) {
		TextView errorText = view.findViewById(R.id.error_text);
		View buttonAuthorize = view.findViewById(R.id.button);

		EditText editText = view.findViewById(R.id.edit_text);
		editText.setText(settings.BACKUP_USER_EMAIL.get());
		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(getActivity(), editText);

		AndroidUiHelper.updateVisibility(errorText, false);
		AndroidUiHelper.updateVisibility(buttonAuthorize, false);

		buttonAuthorize.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialogType = nextType;
				updateContent();
			}
		});
		UiUtilities.setupDialogButton(nightMode, buttonAuthorize, DialogButtonType.SECONDARY, nextType.titleId);
		AndroidUtils.setBackground(app, buttonAuthorize, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);

		buttonContinue.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = editText.getText().toString();
				if (AndroidUtils.isValidEmail(email)) {
					settings.BACKUP_USER_EMAIL.set(email);
					progressBar.setVisibility(View.VISIBLE);
					backupHelper.registerDevice("", geRegisterDeviceListener(currentType, errorText, buttonAuthorize));
				} else {
					editText.requestFocus();
					errorText.setText(R.string.osm_live_enter_email);
					buttonContinue.setEnabled(false);
				}
			}
		});
	}

	private void setupVerifyEmailContainer(View view) {
		TextView errorText = view.findViewById(R.id.error_text);
		EditText editText = view.findViewById(R.id.edit_text);
		View resendButton = view.findViewById(R.id.button);
		View codeMissingButton = view.findViewById(R.id.code_missing_button);
		View codeMissingDescription = view.findViewById(R.id.code_missing_description);

		AndroidUiHelper.updateVisibility(errorText, false);
		AndroidUiHelper.updateVisibility(resendButton, false);
		AndroidUiHelper.updateVisibility(codeMissingDescription, false);

		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(getActivity(), editText);

		resendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				registerUser(errorText);
				AndroidUiHelper.updateVisibility(progressBar, true);
			}
		});
		codeMissingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lastTimeCodeSent > 0 && System.currentTimeMillis() - lastTimeCodeSent >= VERIFICATION_CODE_EXPIRATION_TIME_MIN) {
					registerUser(errorText);
					AndroidUiHelper.updateVisibility(progressBar, true);
				} else {
					AndroidUiHelper.updateVisibility(resendButton, true);
					AndroidUiHelper.updateVisibility(codeMissingDescription, true);
				}
			}
		});
		UiUtilities.setupDialogButton(nightMode, resendButton, DialogButtonType.SECONDARY, R.string.resend_verification_code);
		AndroidUtils.setBackground(app, resendButton, nightMode, R.drawable.dlg_btn_transparent_light, R.drawable.dlg_btn_transparent_dark);

		buttonContinue.setEnabled(!Algorithms.isEmpty(editText.getText()));
		buttonContinue.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String token = editText.getText().toString();
				if (BackupHelper.isTokenValid(token)) {
					progressBar.setVisibility(View.VISIBLE);
					backupHelper.registerDevice(token, new OnRegisterDeviceListener() {

						@Override
						public void onRegisterDevice(int status, @Nullable String message, @Nullable String error) {
							FragmentActivity activity = getActivity();
							if (AndroidUtils.isActivityNotDestroyed(activity)) {
								progressBar.setVisibility(View.INVISIBLE);
								if (status == BackupHelper.STATUS_SUCCESS) {
									FragmentManager fragmentManager = activity.getSupportFragmentManager();
									if (!fragmentManager.isStateSaved()) {
										fragmentManager.popBackStack(SettingsScreenType.BACKUP_AND_RESTORE.name(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
									}
									Intent intent = new Intent(activity, TestBackupActivity.class);
									activity.startActivity(intent);
								} else {
									errorText.setText(message);
									buttonContinue.setEnabled(false);
									AndroidUiHelper.updateVisibility(errorText, true);
								}
							}
						}
					});
				} else {
					editText.requestFocus();
					editText.setError("Token is not valid");
				}
			}
		});
	}

	private TextWatcher getTextWatcher() {
		return new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				buttonContinue.setEnabled(!Algorithms.isEmpty(s));
			}
		};
	}

	private OnRegisterDeviceListener geRegisterDeviceListener(LoginDialogType type, TextView errorText, View nextTypeButton) {
		return new OnRegisterDeviceListener() {

			@Override
			public void onRegisterDevice(int status, @Nullable String message, @Nullable String error) {
				FragmentActivity activity = getActivity();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					int errorCode = BackupHelper.getErrorCode(error);
					if (errorCode == type.permittedErrorCode) {
						registerUser(errorText);
					} else if (errorCode != -1) {
						progressBar.setVisibility(View.INVISIBLE);
						if (errorCode == type.warningErrorCode) {
							errorText.setText(type.warningId);
							AndroidUiHelper.updateVisibility(nextTypeButton, true);
						} else {
							errorText.setText(message);
						}
						buttonContinue.setEnabled(false);
						AndroidUiHelper.updateVisibility(errorText, true);
					}
				}
			}
		};
	}

	private void registerUser(TextView errorText) {
		backupHelper.registerUser(settings.BACKUP_USER_EMAIL.get(), new OnRegisterUserListener() {
			@Override
			public void onRegisterUser(int status, @Nullable String message, @Nullable String error) {
				FragmentActivity activity = getActivity();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progressBar.setVisibility(View.INVISIBLE);
					if (status == BackupHelper.STATUS_SUCCESS) {
						lastTimeCodeSent = System.currentTimeMillis();
						dialogType = LoginDialogType.VERIFY_EMAIL;
						updateContent();
					} else {
						errorText.setText(message);
						buttonContinue.setEnabled(false);
						AndroidUiHelper.updateVisibility(errorText, true);
					}
				}
			}
		});
	}

	private void updateDescription() {
		if (dialogType != LoginDialogType.VERIFY_EMAIL) {
			description.setText(dialogType.descriptionId);
		} else {
			this.description.setText(createColoredSpannable(dialogType.descriptionId, settings.BACKUP_USER_EMAIL.get()));
		}
	}

	private SpannableString createColoredSpannable(@StringRes int textId, String link) {
		String text = getString(textId, link);
		SpannableString spannable = new SpannableString(text);
		int startIndex = text.indexOf(link);
		int endIndex = startIndex + link.length();
		int color = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
		spannable.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannable;
	}

	private void setupSupportButton() {
		TextView supportDescription = mainView.findViewById(R.id.contact_support_button);
		supportDescription.setText(createColoredSpannable(R.string.osmand_cloud_help_descr, OSMAND_EMAIL));
		supportDescription.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				app.sendSupportEmail(getString(R.string.backup_and_restore));
			}
		});
	}

	private enum LoginDialogType {

		SIGN_IN(R.id.sign_in_container, R.string.user_login, R.string.osmand_cloud_login_descr,
				R.string.cloud_email_not_registered, SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED, SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED),
		SIGN_UP(R.id.sign_up_container, R.string.register_opr_create_new_account, R.string.osmand_cloud_create_account_descr,
				R.string.cloud_email_already_registered, SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED, SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED),
		VERIFY_EMAIL(R.id.verify_email_container, R.string.verify_email_address, R.string.verify_email_address_descr,
				-1, -1, -1);

		int viewId;
		int titleId;
		int warningId;
		int descriptionId;
		int warningErrorCode;
		int permittedErrorCode;

		LoginDialogType(int viewId, int titleId, int descriptionId, int warningId, int warningErrorCode, int permittedErrorCode) {
			this.viewId = viewId;
			this.titleId = titleId;
			this.warningId = warningId;
			this.descriptionId = descriptionId;
			this.warningErrorCode = warningErrorCode;
			this.permittedErrorCode = permittedErrorCode;
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, boolean signUp) {
		if (!fragmentManager.isStateSaved()) {
			AuthorizeFragment fragment = new AuthorizeFragment();
			fragment.dialogType = signUp ? LoginDialogType.SIGN_UP : LoginDialogType.SIGN_IN;
			fragmentManager.beginTransaction().
					replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}