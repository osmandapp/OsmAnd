package net.osmand.plus.settings.bottomsheets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

	/*@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void openDocumentTree() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		if (currentPath != null) {
			Uri uri = Uri.fromFile(new File(currentPath));
		}
		startActivityForResult(intent, CHOOSE_FOLDER_REQUEST_CODE);
	}*/


	/*@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		Uri result = data.getData();
		if (result == null) {
			return;
		}
		if (requestCode == CHOOSE_FOLDER_REQUEST_CODE) {
			DocumentFile documentFile = DocumentFile.fromTreeUri(getContext(), result);
			File f = null;
			try {
				f = FileUtil.from(getContext(), documentFile.getUri());
			} catch (IOException e) {
				e.printStackTrace();
			}
			Toast.makeText(getMyApplication(), f.getAbsolutePath(), Toast.LENGTH_LONG).show();
			for (DocumentFile file : documentFile.listFiles()) {
				if (file.isDirectory()) {
					Toast.makeText(getMyApplication(), file.getName(), Toast.LENGTH_SHORT).show();
					etWasFocused = true;
					editText.setText(file.getName());
					break;
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}*/

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
			String newPath = editText.getText().toString();
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

	
	
	
	
	
	
	/*private static class FileUtil {
		private static final int EOF = -1;
		private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

		private FileUtil() {

		}

		public static File from(Context context, Uri uri) throws IOException {
			InputStream inputStream = context.getContentResolver().openInputStream(uri);
			String fileName = getFileName(context, uri);
			String[] splitName = splitFileName(fileName);
			File tempFile = File.createTempFile(splitName[0], splitName[1]);
			tempFile = rename(tempFile, fileName);
			tempFile.deleteOnExit();
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(tempFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if (inputStream != null) {
				copy(inputStream, out);
				inputStream.close();
			}

			if (out != null) {
				out.close();
			}
			return tempFile;
		}

		private static String[] splitFileName(String fileName) {
			String name = fileName;
			String extension = "";
			int i = fileName.lastIndexOf(".");
			if (i != -1) {
				name = fileName.substring(0, i);
				extension = fileName.substring(i);
			}

			return new String[]{name, extension};
		}

		private static String getFileName(Context context, Uri uri) {
			String result = null;
			if (uri.getScheme().equals("content")) {
				Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
				try {
					if (cursor != null && cursor.moveToFirst()) {
						result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
			if (result == null) {
				result = uri.getPath();
				int cut = result.lastIndexOf(File.separator);
				if (cut != -1) {
					result = result.substring(cut + 1);
				}
			}
			return result;
		}

		private static File rename(File file, String newName) {
			File newFile = new File(file.getParent(), newName);
			if (!newFile.equals(file)) {
				if (newFile.exists() && newFile.delete()) {
//					Log.d("FileUtil", "Delete old " + newName + " file");
				}
				if (file.renameTo(newFile)) {
//					Log.d("FileUtil", "Rename file to " + newName);
				}
			}
			return newFile;
		}

		private static long copy(InputStream input, OutputStream output) throws IOException {
			long count = 0;
			int n;
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			while (EOF != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
				count += n;
			}
			return count;
		}
	}*/
}
