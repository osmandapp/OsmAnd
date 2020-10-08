package net.osmand.plus.measurementtool;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.HorizontalRecyclerBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.measurementtool.adapter.FolderListAdapter;

import org.apache.commons.logging.Log;

import java.io.File;

public class SaveAsNewTrackBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SaveAsNewTrackBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SaveAsNewTrackBottomSheetDialogFragment.class);
	public static final String SHOW_ON_MAP_KEY = "show_on_map_key";
	public static final String SIMPLIFIED_TRACK_KEY = "simplified_track_key";
	public static final String FOLDER_NAME_KEY = "folder_name_key";
	public static final String FILE_NAME_KEY = "file_name_key";
	public static final String SOURCE_FILE_NAME_KEY = "source_file_name_key";
	public static final String SOURCE_FOLDER_NAME_KEY = "source_folder_name_key";
	public static final String SHOW_SIMPLIFIED_BUTTON_KEY = "show_simplified_button_key";

	private boolean showOnMap;
	private boolean simplifiedTrack;
	private String fileName;
	private String sourceFileName;
	private String sourceFolderName;
	private String folderName;
	private boolean rightButtonEnabled = true;
	private boolean showSimplifiedButton = true;
	private TextInputLayout nameTextBox;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		int highlightColorId = nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light;
		if (savedInstanceState != null) {
			showOnMap = savedInstanceState.getBoolean(SHOW_ON_MAP_KEY);
			simplifiedTrack = savedInstanceState.getBoolean(SIMPLIFIED_TRACK_KEY);
			folderName = savedInstanceState.getString(FOLDER_NAME_KEY);
			fileName = savedInstanceState.getString(FILE_NAME_KEY);
			sourceFileName = savedInstanceState.getString(SOURCE_FILE_NAME_KEY);
			sourceFolderName = savedInstanceState.getString(SOURCE_FOLDER_NAME_KEY);
			showSimplifiedButton = savedInstanceState.getBoolean(SHOW_SIMPLIFIED_BUTTON_KEY);
		} else {
			folderName = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getName();
		}

		items.add(new TitleItem(getString(R.string.save_as_new_track)));

		View editNameView = View.inflate(UiUtilities.getThemedContext(app, nightMode),
				R.layout.track_name_edit_text, null);
		nameTextBox = editNameView.findViewById(R.id.name_text_box);
		nameTextBox.setBoxBackgroundColorResource(highlightColorId);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_file_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat
				.getColor(app, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		TextInputEditText nameText = editNameView.findViewById(R.id.name_edit_text);
		nameText.setText(fileName);
		nameText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				updateFileNameFromEditText(s.toString());
			}
		});
		BaseBottomSheetItem editFileName = new BaseBottomSheetItem.Builder()
				.setCustomView(editNameView)
				.create();
		this.items.add(editFileName);

		int contentPaddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		int contentPaddingHalf = app.getResources().getDimensionPixelSize(R.dimen.content_padding_half);

		items.add(new DividerSpaceItem(app, contentPaddingSmall));

		FolderListAdapter adapter = new FolderListAdapter(app, nightMode, folderName);
		if (adapter.getItemCount() > 0) {
			adapter.setListener(createFolderSelectListener());
			View view = View.inflate(UiUtilities.getThemedContext(app, nightMode), R.layout.bottom_sheet_item_recyclerview,
					null);
			View recyclerView = view.findViewById(R.id.recycler_view);
			recyclerView.setPadding(contentPaddingHalf, 0, contentPaddingHalf, 0);
			BaseBottomSheetItem scrollItem = new HorizontalRecyclerBottomSheetItem.Builder()
					.setAdapter(adapter)
					.setCustomView(view)
					.create();
			this.items.add(scrollItem);

			items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.dialog_content_margin)));
		}

		int activeColorRes = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;

		if (showSimplifiedButton) {
			final BottomSheetItemWithCompoundButton[] simplifiedTrackItem = new BottomSheetItemWithCompoundButton[1];
			simplifiedTrackItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(simplifiedTrack)
					.setCompoundButtonColorId(activeColorRes)
					.setDescription(getSimplifiedTrackDescription())
					.setBackground(getBackground(simplifiedTrack))
					.setTitle(getString(R.string.simplified_track))
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							simplifiedTrack = !simplifiedTrack;
							simplifiedTrackItem[0].setChecked(simplifiedTrack);
							AndroidUtils.setBackground(simplifiedTrackItem[0].getView(), getBackground(simplifiedTrack));
							simplifiedTrackItem[0].setDescription(getSimplifiedTrackDescription());
						}
					})
					.create();
			items.add(simplifiedTrackItem[0]);

			items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.content_padding)));
		}

		final BottomSheetItemWithCompoundButton[] showOnMapItem = new BottomSheetItemWithCompoundButton[1];
		showOnMapItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColorId(activeColorRes)
				.setChecked(showOnMap)
				.setBackground(getBackground(showOnMap))
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOnMap = !showOnMap;
						showOnMapItem[0].setChecked(showOnMap);
						AndroidUtils.setBackground(showOnMapItem[0].getView(), getBackground(showOnMap));
					}
				})
				.create();
		items.add(showOnMapItem[0]);

		items.add(new DividerSpaceItem(app, contentPaddingSmall));
	}

	private String getSimplifiedTrackDescription() {
		return simplifiedTrack ? getString(R.string.simplified_track_description) : "";
	}

	private Drawable getBackground(boolean checked) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			GradientDrawable background = (GradientDrawable) AppCompatResources.getDrawable(app,
					R.drawable.bg_select_group_button_outline);
			if (background != null) {
				int highlightColor = ContextCompat.getColor(app,nightMode ?
						R.color.list_background_color_dark : R.color.activity_background_color_light);
				int strokedColor = AndroidUtils.getColorFromAttr(UiUtilities.getThemedContext(app, nightMode),
						R.attr.stroked_buttons_and_links_outline);
				background = (GradientDrawable) background.mutate();
				if (checked) {
					background.setStroke(0, Color.TRANSPARENT);
					background.setColor(highlightColor);
				} else {
					background.setStroke(app.getResources().getDimensionPixelSize(R.dimen.map_button_stroke), strokedColor);
				}
			}
			return background;
		}
		return null;
	}

	private FolderListAdapter.FolderListAdapterListener createFolderSelectListener() {
		return new FolderListAdapter.FolderListAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				folderName = item;
				EditText editText = nameTextBox.getEditText();
				if (editText != null) {
					updateFileNameFromEditText(editText.getText().toString());
				}
			}
		};
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(SHOW_ON_MAP_KEY, showOnMap);
		outState.putBoolean(SIMPLIFIED_TRACK_KEY, simplifiedTrack);
		outState.putString(FOLDER_NAME_KEY, folderName);
		outState.putString(FILE_NAME_KEY, fileName);
		outState.putString(SOURCE_FILE_NAME_KEY, sourceFileName);
		outState.putString(SOURCE_FOLDER_NAME_KEY, sourceFolderName);
		outState.putBoolean(SHOW_SIMPLIFIED_BUTTON_KEY, showSimplifiedButton);
		super.onSaveInstanceState(outState);
	}

	public static void showInstance(@NonNull FragmentManager fm, @Nullable Fragment targetFragment, String folderName,
	                                String fileName, boolean showSimplifiedButton, boolean showOnMap) {
		try {
			if (!fm.isStateSaved()) {
				SaveAsNewTrackBottomSheetDialogFragment fragment = new SaveAsNewTrackBottomSheetDialogFragment();
				fragment.setTargetFragment(targetFragment, 0);
				fragment.fileName = fileName;
				fragment.sourceFileName = fileName;
				fragment.sourceFolderName = folderName;
				fragment.showSimplifiedButton = showSimplifiedButton;
				fragment.showOnMap = showOnMap;
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof SaveAsNewTrackFragmentListener) {
			((SaveAsNewTrackFragmentListener) targetFragment).onSaveAsNewTrack(folderName, fileName, showOnMap,
					simplifiedTrack);
		} else {
			renameFile();
		}
		dismiss();
	}

	private void renameFile() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			File source = getFile(app, sourceFolderName, sourceFileName);
			File dest = getFile(app, folderName, fileName);
			if (!source.equals(dest)) {
				if (dest.exists()) {
					Toast.makeText(app, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
				} else {
					if (source.renameTo(dest)) {
						app.getGpxDbHelper().rename(source, dest);
					} else {
						Toast.makeText(app, R.string.file_can_not_be_moved, Toast.LENGTH_LONG).show();
					}
				}
			}
			GPXUtilities.GPXFile gpxFile = GPXUtilities.loadGPXFile(dest);
			if (gpxFile.error != null) {
				return;
			}
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, showOnMap, false);
		}
	}

	private File getFile(OsmandApplication app, String folderName, String fileName) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		File source = dir;
		if (folderName != null && !dir.getName().equals(folderName)) {
			source = new File(dir, folderName);
		}
		source = new File(source, fileName + IndexConstants.GPX_FILE_EXT);
		return source;
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return rightButtonEnabled;
	}

	private void updateFileNameFromEditText(String name) {
		rightButtonEnabled = false;
		String text = name.trim();
		if (text.isEmpty()) {
			nameTextBox.setError(getString(R.string.empty_filename));
		} else if (isFileExist(name)) {
			nameTextBox.setError(getString(R.string.file_with_name_already_exist));
		} else {
			nameTextBox.setError(null);
			fileName = text;
			rightButtonEnabled = true;
		}
		updateBottomButtons();
	}

	private boolean isFileExist(String name) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			File file = getFile(app, folderName, name);
			return file.exists();
		}
		return false;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light;
	}

	public interface SaveAsNewTrackFragmentListener {

		void onSaveAsNewTrack(String folderName, String fileName, boolean showOnMap, boolean simplifiedTrack);

	}
}
