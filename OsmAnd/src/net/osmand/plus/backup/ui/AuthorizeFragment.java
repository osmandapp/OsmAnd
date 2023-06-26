package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT;
import static net.osmand.plus.backup.BackupHelper.SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED;
import static net.osmand.plus.backup.ui.LoginDialogType.DELETE_ACCOUNT;
import static net.osmand.plus.backup.ui.LoginDialogType.SIGN_IN;
import static net.osmand.plus.backup.ui.LoginDialogType.SIGN_UP;
import static net.osmand.plus.backup.ui.LoginDialogType.VERIFY_EMAIL;
import static net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.SOFT_KEYBOARD_MIN_DETECTION_SIZE;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnRegisterDeviceListener;
import net.osmand.plus.backup.BackupListeners.OnRegisterUserListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class AuthorizeFragment extends BaseOsmAndFragment implements OnRegisterUserListener, OnRegisterDeviceListener {

	private static final Log LOG = PlatformUtil.getLog(AuthorizeFragment.class);

	public static final String TAG = AuthorizeFragment.class.getSimpleName();

	private static final String LOGIN_DIALOG_TYPE_KEY = "login_dialog_type_key";
	private static final String SIGN_IN_KEY = "sign_in_key";

	private static final int VERIFICATION_CODE_EXPIRATION_TIME_MIN = 10 * 60 * 1000; // 10 minutes

	private BackupHelper backupHelper;

	private View mainView;
	private Toolbar toolbar;
	private ProgressBar progressBar;
	private TextView description;
	private View buttonContinue;
	private View buttonChoosePlan;
	private EditText editText;
	private TextView errorText;
	private View buttonAuthorize;
	private View keyboardSpace;
	private View space;

	private LoginDialogType dialogType = SIGN_UP;
	private LoginDialogType firstDialogType = SIGN_UP;

	private boolean signIn;

	private long lastTimeCodeSent;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@NonNull
	public LoginDialogType getDialogType() {
		return dialogType;
	}

	public void setDialogType(@NonNull LoginDialogType dialogType) {
		this.dialogType = dialogType;
		if (dialogType != VERIFY_EMAIL) {
			signIn = dialogType == SIGN_IN;
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(LOGIN_DIALOG_TYPE_KEY)) {
				dialogType = LoginDialogType.valueOf(savedInstanceState.getString(LOGIN_DIALOG_TYPE_KEY));
			}
			if (savedInstanceState.containsKey(SIGN_IN_KEY)) {
				signIn = savedInstanceState.getBoolean(SIGN_IN_KEY);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_cloud_authorize, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		space = view.findViewById(R.id.space);
		toolbar = view.findViewById(R.id.toolbar);
		mainView = view.findViewById(R.id.main_view);
		description = view.findViewById(R.id.description);
		progressBar = view.findViewById(R.id.progress_bar);
		buttonContinue = view.findViewById(R.id.continue_button);
		buttonChoosePlan = view.findViewById(R.id.get_button);
		keyboardSpace = view.findViewById(R.id.keyboard_space);

		setupToolbar();
		setupTextWatchers();
		updateContent();
		setupSupportButton();
		setupKeyboardListener();

		UiUtilities.setupDialogButton(nightMode, buttonChoosePlan, DialogButtonType.SECONDARY, R.string.shared_string_get);
		UiUtilities.setupDialogButton(nightMode, buttonContinue, DialogButtonType.PRIMARY, R.string.shared_string_continue);

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(SIGN_IN_KEY, signIn);
		outState.putString(LOGIN_DIALOG_TYPE_KEY, dialogType.name());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		backupHelper.getBackupListeners().addRegisterUserListener(this);
		backupHelper.getBackupListeners().addRegisterDeviceListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		backupHelper.getBackupListeners().removeRegisterUserListener(this);
		backupHelper.getBackupListeners().removeRegisterDeviceListener(this);
	}

	private void setupTextWatchers() {
		for (LoginDialogType type : LoginDialogType.values()) {
			View itemView = mainView.findViewById(type.viewId);
			EditText editText = itemView.findViewById(R.id.edit_text);
			editText.addTextChangedListener(getTextWatcher());

			if (type == SIGN_UP) {
				EditText promoCodeEditText = itemView.findViewById(R.id.promocode_edit_text);
				promoCodeEditText.addTextChangedListener(getPromoTextWatcher(editText));
			}
		}
	}

	private void setupToolbar() {
		AppBarLayout appBarLayout = mainView.findViewById(R.id.appbar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void updateContent() {
		toolbar.setTitle(dialogType.titleId);
		updateDescription();

		for (LoginDialogType type : LoginDialogType.values()) {
			View view = mainView.findViewById(type.viewId);

			boolean selected = dialogType == type;
			if (selected) {
				if (dialogType == VERIFY_EMAIL) {
					setupVerifyEmailContainer(view);
				} else if (dialogType == SIGN_IN) {
					setupAuthorizeContainer(view, SIGN_UP);
				} else if (dialogType == SIGN_UP) {
					setupAuthorizeContainer(view, SIGN_IN);
				} else if (dialogType == DELETE_ACCOUNT) {
					setupDeleteAccountContainer(view);
				}
			}
			AndroidUiHelper.updateVisibility(view, selected);
		}
	}

	private void setupAuthorizeContainer(@NonNull View view, @NonNull LoginDialogType nextType) {
		errorText = view.findViewById(R.id.error_text);
		buttonAuthorize = view.findViewById(R.id.button);

		editText = view.findViewById(R.id.edit_text);
		EditText promoEditText = view.findViewById(R.id.promocode_edit_text);

		editText.setText(settings.BACKUP_USER_EMAIL.get());
		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(requireActivity(), editText);

		updateErrorVisibility(false);
		AndroidUiHelper.updateVisibility(buttonAuthorize, false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.promocode_container), dialogType == SIGN_UP && promoCodeSupported());

		buttonAuthorize.setOnClickListener(v -> {
			setDialogType(nextType);
			updateContent();
		});
		UiUtilities.setupDialogButton(nightMode, buttonAuthorize, DialogButtonType.SECONDARY_ACTIVE, nextType.titleId);

		buttonChoosePlan.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ChoosePlanFragment.showInstance(activity, OsmAndFeature.OSMAND_CLOUD);
			}
		});
		AndroidUiHelper.updateVisibility(buttonChoosePlan, false);

		buttonContinue.setOnClickListener(v -> {
			String email = editText.getText().toString();
			String promoCode = promoEditText.getText().toString();
			if (AndroidUtils.isValidEmail(email)) {
				settings.BACKUP_USER_EMAIL.set(email);
				settings.BACKUP_PROMOCODE.set(promoCode);

				progressBar.setVisibility(View.VISIBLE);
				backupHelper.registerDevice("");
			} else {
				editText.requestFocus();
				errorText.setText(R.string.osm_live_enter_email);
				buttonContinue.setEnabled(false);
				updateErrorVisibility(true);
			}
		});
	}

	private void setupVerifyEmailContainer(@NonNull View view) {
		errorText = view.findViewById(R.id.error_text);
		editText = view.findViewById(R.id.edit_text);
		View resendButton = view.findViewById(R.id.button);
		View codeMissingButton = view.findViewById(R.id.code_missing_button);
		View codeMissingDescription = view.findViewById(R.id.code_missing_description);

		updateErrorVisibility(false);
		AndroidUiHelper.updateVisibility(resendButton, false);
		AndroidUiHelper.updateVisibility(codeMissingDescription, false);

		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(requireActivity(), editText);

		resendButton.setOnClickListener(v -> {
			registerUser();
			AndroidUiHelper.updateVisibility(progressBar, true);
		});
		codeMissingButton.setOnClickListener(v -> {
			if (lastTimeCodeSent > 0 && System.currentTimeMillis() - lastTimeCodeSent >= VERIFICATION_CODE_EXPIRATION_TIME_MIN) {
				registerUser();
				AndroidUiHelper.updateVisibility(progressBar, true);
			} else {
				AndroidUiHelper.updateVisibility(resendButton, true);
				AndroidUiHelper.updateVisibility(codeMissingDescription, true);
			}
		});
		UiUtilities.setupDialogButton(nightMode, resendButton, DialogButtonType.SECONDARY_ACTIVE, R.string.resend_verification_code);

		buttonContinue.setEnabled(!Algorithms.isEmpty(editText.getText()));
		buttonContinue.setOnClickListener(v -> registerDevice(editText.getText().toString()));
	}

	private void setupDeleteAccountContainer(@NonNull View view) {
		errorText = view.findViewById(R.id.error_text);
		editText = view.findViewById(R.id.edit_text);
		editText.setText(null);
		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(requireActivity(), editText);

		buttonContinue.setOnClickListener(v -> {
			String email = editText.getText().toString();
			String savedEmail = settings.BACKUP_USER_EMAIL.get();

			boolean emailMatch = Algorithms.stringsEqual(email, savedEmail);
			if (AndroidUtils.isValidEmail(email) && emailMatch) {
				progressBar.setVisibility(View.VISIBLE);
				backupHelper.registerDevice("");
			} else {
				editText.requestFocus();
				errorText.setText(emailMatch ? R.string.osm_live_enter_email : R.string.backup_error_delete_account_email_match);
				buttonContinue.setEnabled(false);
				updateErrorVisibility(true);
			}
		});
		updateErrorVisibility(false);
		AndroidUiHelper.updateVisibility(buttonChoosePlan, false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.button), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.promocode_container), false);
	}

	private void updateErrorVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(errorText, visible);
	}

	public void setToken(@Nullable String token) {
		editText.setText(token);
		registerDevice(token);
	}

	private void registerDevice(@Nullable String token) {
		if (!Algorithms.isEmpty(token) && BackupHelper.isTokenValid(token)) {
			progressBar.setVisibility(View.VISIBLE);
			backupHelper.registerDevice(token);
			Activity activity = getActivity();
			if (AndroidUtils.isActivityNotDestroyed(activity)) {
				AndroidUtils.hideSoftKeyboard(activity, editText);
			}
		} else {
			editText.requestFocus();
			editText.setError(getString(R.string.token_is_not_valid));
		}
	}

	private TextWatcher getTextWatcher() {
		return new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				buttonContinue.setEnabled(!Algorithms.isEmpty(s));
			}
		};
	}

	private TextWatcher getPromoTextWatcher(EditText editText) {
		return new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				buttonContinue.setEnabled(!Algorithms.isEmpty(editText.getText()));
			}
		};
	}

	private void registerUser() {
		boolean login = dialogType == SIGN_IN || dialogType == DELETE_ACCOUNT || signIn;
		backupHelper.registerUser(settings.BACKUP_USER_EMAIL.get(), settings.BACKUP_PROMOCODE.get(), login);
	}

	@Override
	public void onRegisterUser(int status, @Nullable String message, @Nullable BackupError error) {
		FragmentActivity activity = getActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			progressBar.setVisibility(View.INVISIBLE);
			if (status == BackupHelper.STATUS_SUCCESS) {
				lastTimeCodeSent = System.currentTimeMillis();
				setDialogType(VERIFY_EMAIL);
				updateContent();
			} else {
				boolean choosePlanVisible = false;
				if (error != null) {
					int code = error.getCode();
					choosePlanVisible = !promoCodeSupported()
							&& (code == SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION
							|| code == SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED
							|| code == SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT);
				}
				errorText.setText(error != null ? error.getLocalizedError(app) : message);
				buttonContinue.setEnabled(false);
				updateErrorVisibility(true);
				AndroidUiHelper.updateVisibility(buttonChoosePlan, choosePlanVisible);
			}
		}
	}

	@Override
	public void onRegisterDevice(int status, @Nullable String message, @Nullable BackupError error) {
		FragmentActivity activity = getActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			int errorCode = error != null ? error.getCode() : -1;
			if (dialogType == VERIFY_EMAIL) {
				progressBar.setVisibility(View.INVISIBLE);
				if (status == BackupHelper.STATUS_SUCCESS) {
					dismiss();
					if (firstDialogType != DELETE_ACCOUNT) {
						openCloudScreen();
					} else {
						openDeletionScreen();
					}
				} else {
					errorText.setText(error != null ? error.getLocalizedError(app) : message);
					buttonContinue.setEnabled(false);
					updateErrorVisibility(true);
				}
			} else {
				if (errorCode == dialogType.permittedErrorCode) {
					registerUser();
				} else if (errorCode != -1) {
					progressBar.setVisibility(View.INVISIBLE);
					if (errorCode == dialogType.warningErrorCode) {
						errorText.setText(dialogType.warningId);
						AndroidUiHelper.updateVisibility(buttonAuthorize, !promoCodeSupported());
					} else {
						errorText.setText(error.getLocalizedError(app));
					}
					buttonContinue.setEnabled(false);
					updateErrorVisibility(true);
				}
				AndroidUiHelper.updateVisibility(buttonChoosePlan, false);
			}
		}
	}

	private void dismiss() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null && !fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(BackupAuthorizationFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	private void openCloudScreen() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null && !fragmentManager.isStateSaved()) {
			BackupCloudFragment.showInstance(fragmentManager, signIn ? SIGN_IN : SIGN_UP);
		}
	}

	private void openDeletionScreen() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null) {
			DeleteAccountFragment.showInstance(fragmentManager);
		}
	}

	private boolean promoCodeSupported() {
		return !Version.isGooglePlayEnabled();
	}

	private void updateDescription() {
		if (dialogType != VERIFY_EMAIL) {
			description.setText(dialogType.descriptionId);
		} else {
			description.setText(createColoredSpannable(dialogType.descriptionId, settings.BACKUP_USER_EMAIL.get()));
		}
	}

	private SpannableString createColoredSpannable(@StringRes int textId, String link) {
		String text = getString(textId, link);
		SpannableString spannable = new SpannableString(text);
		int startIndex = text.indexOf(link);
		int endIndex = startIndex + link.length();
		int color = ColorUtilities.getActiveColor(app, nightMode);
		spannable.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannable;
	}

	private void setupSupportButton() {
		TextView supportDescription = mainView.findViewById(R.id.contact_support_button);
		String email = getString(R.string.support_email);
		supportDescription.setText(createColoredSpannable(R.string.osmand_cloud_help_descr, email));
		supportDescription.setOnClickListener(v -> app.getFeedbackHelper().sendSupportEmail(getString(R.string.backup_and_restore)));
	}

	private void setupKeyboardListener() {
		if (AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			mainView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				private int previousKeyboardHeight;

				@Override
				public void onGlobalLayout() {
					Rect r = new Rect();
					mainView.getWindowVisibleDisplayFrame(r);
					Activity activity = getMyActivity();
					if (activity == null) {
						return;
					}
					int screenHeight = mainView.getHeight();
					int keypadHeight = screenHeight - r.bottom;
					int keypadSpace = keypadHeight;
					if (!AndroidUtils.isInFullScreenMode(activity)) {
						keypadSpace += AndroidUtils.getStatusBarHeight(activity);
					}
					boolean softKeyboardVisible = keypadHeight > screenHeight * SOFT_KEYBOARD_MIN_DETECTION_SIZE;

					if (previousKeyboardHeight != keypadHeight) {
						previousKeyboardHeight = keypadHeight;
						if (softKeyboardVisible) {
							space.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, space.getHeight() / 2));
							keyboardSpace.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keypadSpace));
						} else {
							space.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
						}
						AndroidUiHelper.updateVisibility(keyboardSpace, softKeyboardVisible);
					}
				}
			});
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull LoginDialogType dialogType) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AuthorizeFragment fragment = new AuthorizeFragment();
			fragment.firstDialogType = dialogType;
			fragment.setDialogType(dialogType);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}