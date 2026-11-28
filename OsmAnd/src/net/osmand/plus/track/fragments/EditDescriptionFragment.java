package net.osmand.plus.track.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.EditTextEx;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class EditDescriptionFragment extends BaseFullScreenDialogFragment {

	public static final String TAG = EditDescriptionFragment.class.getSimpleName();

	private static final String ORIGINAL_TEXT_KEY = "edit_description_original";
	private static final String EDITED_TEXT_KEY = "edit_description_edited";
	private static final String SELECTION_START_KEY = "selection_start";
	private static final String SELECTION_END_KEY = "selection_end";

	private DescriptionStateStorage originalTextStorage;
	private DescriptionStateStorage editedTextStorage;

	private EditTextEx etText;
	private String mText;
	@Nullable
	private String restoredText;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		originalTextStorage = new DescriptionStateStorage(app, ORIGINAL_TEXT_KEY);
		editedTextStorage = new DescriptionStateStorage(app, EDITED_TEXT_KEY);
		if (savedInstanceState != null) {
			mText = originalTextStorage.restore(savedInstanceState);
			restoredText = editedTextStorage.restore(savedInstanceState);
			if (mText == null || restoredText == null) {
				dismiss();
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.dialog_edit_gpx_description, container, false);

		etText = view.findViewById(R.id.description);
		// the text is saved by the fragment, see onSaveInstanceState()
		etText.setSaveEnabled(false);
		etText.requestFocus();

		view.findViewById(R.id.btn_close).setOnClickListener(v -> {
			if (shouldClose()) {
				dismiss();
			} else {
				showDismissDialog();
			}
		});

		setupSaveButton(view);

		String text = restoredText != null ? restoredText : mText;
		if (text != null) {
			etText.append(text);
		}
		if (savedInstanceState != null) {
			restoreSelection(savedInstanceState);
		}
		return view;
	}

	private void restoreSelection(@NonNull Bundle savedInstanceState) {
		int length = etText.length();
		int start = savedInstanceState.getInt(SELECTION_START_KEY, length);
		int end = savedInstanceState.getInt(SELECTION_END_KEY, length);
		if (start >= 0 && end >= 0) {
			etText.setSelection(Math.min(start, length), Math.min(end, length));
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mText == null) {
			return;
		}
		originalTextStorage.save(outState, mText);

		Editable editable = etText != null ? etText.getText() : null;
		if (editable != null) {
			outState.putInt(SELECTION_START_KEY, etText.getSelectionStart());
			outState.putInt(SELECTION_END_KEY, etText.getSelectionEnd());
			editedTextStorage.save(outState, editable.toString());
		} else {
			editedTextStorage.save(outState, restoredText != null ? restoredText : mText);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		originalTextStorage.clearIfClosed(this);
		editedTextStorage.clearIfClosed(this);
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@Override
	protected int getStatusBarColorId() {
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	private boolean shouldClose() {
		Editable editable = etText.getText();
		if (mText == null || editable == null) {
			return true;
		}
		return mText.equals(editable.toString());
	}

	private void setupSaveButton(View view) {
		View btnSaveContainer = view.findViewById(R.id.btn_save_container);
		btnSaveContainer.setOnClickListener(v -> {
			Editable editable = etText.getText();
			if (editable != null && !onSaveEditedText(editable.toString())) {
				dismiss();
			}
		});

		Context ctx = btnSaveContainer.getContext();
		AndroidUtils.setBackground(ctx, btnSaveContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

		View btnSave = view.findViewById(R.id.btn_save);
		int drawableRes = nightMode ? R.drawable.btn_solid_border_dark : R.drawable.btn_solid_border_light;
		AndroidUtils.setBackground(btnSave, getIcon(drawableRes));
	}

	private void showDismissDialog() {
		AlertDialogData dialogData = new AlertDialogData(requireActivity(), nightMode)
				.setTitle(R.string.shared_string_dismiss)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		CustomAlert.showSimpleMessage(dialogData, R.string.exit_without_saving);
	}

	private boolean onSaveEditedText(@NonNull String editedText) {
		Fragment target = getTargetFragment();
		if (target instanceof OnSaveDescriptionCallback callback) {
			return callback.onSaveEditedDescription(editedText, this::dismiss);
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String description, @Nullable Fragment target) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			EditDescriptionFragment fragment = new EditDescriptionFragment();
			fragment.mText = description;
			fragment.setTargetFragment(target, 0);
			fragment.show(fm, TAG);
		}
	}

	public interface OnSaveDescriptionCallback {
		boolean onSaveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback);
	}

	public interface OnDescriptionSavedCallback {
		void onDescriptionSaved();
	}

	@Override
	public String getAnalyticsScreen() {
		return "edit_description";
	}
}
