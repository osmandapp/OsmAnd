package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.BaseSettingsFragment;

import org.apache.commons.logging.Log;

import static android.view.View.GONE;

public class SelectFolderBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = "SelectFolderBottomSheet";
	private static final Log LOG = PlatformUtil.getLog(SelectFolderBottomSheet.class);
	private static final int CHOOSE_FOLDER_REQUEST_CODE = 0;

	private static final String EDIT_TEXT_PREFERENCE_KEY = "edit_text_preference_key";
	private static final String DIALOG_TITLE = "dialog_title";
	private static final String DESCRIPTION = "description";
	private static final String BTN_TITLE = "btn_title";
	private static final String ET_WAS_FOCUSED = "edit_text_was_focused";
	public static final String NEW_PATH = "path";
	public static final String PATH_CHANGED = "changed";

	private EditText editText;

	private String currentPath;
	private String dialogTitle;
	private String btnTitle;
	private String description;
	private boolean etWasFocused;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context ctx = getContext();

		String text = null;
		if (savedInstanceState != null) {
			String folderPath = savedInstanceState.getString(NEW_PATH);
			if (folderPath != null) {
				currentPath = folderPath;
			}
			text = savedInstanceState.getString(EDIT_TEXT_PREFERENCE_KEY);
			dialogTitle = savedInstanceState.getString(DIALOG_TITLE);
			description = savedInstanceState.getString(DESCRIPTION);
			btnTitle = savedInstanceState.getString(BTN_TITLE);
			etWasFocused = savedInstanceState.getBoolean(ET_WAS_FOCUSED);
		}

		if (ctx == null || currentPath == null) {
			return;
		}

		if (dialogTitle != null) {
			items.add(new TitleItem(dialogTitle));
		}

		View mainView = View.inflate(ctx, R.layout.bottom_sheet_select_folder, null);

		TextView tvDescription = mainView.findViewById(R.id.description);
		TextView tvBtnTitle = mainView.findViewById(R.id.title);
		editText = mainView.findViewById(R.id.text);

		View divider = mainView.findViewById(R.id.divider);
		View btnOpenChoseDialog = mainView.findViewById(R.id.button);

//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//			if (btnTitle != null) {
//				tvBtnTitle.setText(btnTitle);
//				int colorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
//				int color = ContextCompat.getColor(ctx, colorResId);
//				Drawable drawable = UiUtilities.getColoredSelectableDrawable(ctx, color, 0.3f);
//				AndroidUtils.setBackground(btnOpenChoseDialog, drawable);
//				btnOpenChoseDialog.setOnClickListener(new View.OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						openDocumentTree();
//					}
//				});
//			}
//		} else {
		divider.setVisibility(GONE);
		btnOpenChoseDialog.setVisibility(GONE);
//		}

		if (text != null) {
			editText.setText(text);
		}

		if (description != null) {
			tvDescription.setText(description);
		}

		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !etWasFocused) {
					etWasFocused = true;
					editText.setText(currentPath);
				}
			}
		});

		BaseBottomSheetItem baseItem = new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(baseItem);

	}

	public static boolean showInstance(FragmentManager fm, String prefId, String currentPath, Fragment target,
	                                   String dialogTitle, String description, String btnTitle, boolean usedOnMap) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				SelectFolderBottomSheet fragment = new SelectFolderBottomSheet();
				fragment.setCurrentPath(currentPath);
				fragment.setTargetFragment(target, 0);
				fragment.setDialogTitle(dialogTitle);
				fragment.setDescription(description);
				fragment.setBtnTitle(btnTitle);
				fragment.setUsedOnMap(usedOnMap);
				fragment.show(fm, TAG);
			}
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof BaseSettingsFragment) {
			String newPath = editText.getText().toString().trim();
			if (!newPath.equals("")) {
				boolean pathChanged = !newPath.equals(currentPath);
				Bundle bundle = new Bundle();
				bundle.putBoolean(TAG, true);
				bundle.putString(NEW_PATH, newPath);
				bundle.putBoolean(PATH_CHANGED, pathChanged);
				((BaseSettingsFragment) fragment).onPreferenceChange(getPreference(), bundle);
			}
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(EDIT_TEXT_PREFERENCE_KEY, editText.getText().toString());
		outState.putString(NEW_PATH, currentPath);
		outState.putString(DIALOG_TITLE, dialogTitle);
		outState.putString(DESCRIPTION, description);
		outState.putString(BTN_TITLE, btnTitle);
		outState.putBoolean(ET_WAS_FOCUSED, etWasFocused);
	}

	public void setCurrentPath(String currentPath) {
		this.currentPath = currentPath;
	}

	public void setDialogTitle(String dialogTitle) {
		this.dialogTitle = dialogTitle;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setBtnTitle(String btnTitle) {
		this.btnTitle = btnTitle;
	}

}