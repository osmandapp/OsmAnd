package net.osmand.plus.measurementtool;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.plus.measurementtool.adapter.FolderListAdapter.VIEW_TYPE_ADD;
import static net.osmand.plus.measurementtool.adapter.FolderListAdapter.getFolders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.HorizontalRecyclerBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.measurementtool.adapter.FolderListAdapter;
import net.osmand.plus.measurementtool.adapter.FolderListAdapter.FolderListAdapterListener;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SaveAsNewTrackBottomSheetDialogFragment extends MenuBottomSheetDialogFragment
		implements OnTrackFileMoveListener, OnTrackFolderAddListener {

	public static final String TAG = SaveAsNewTrackBottomSheetDialogFragment.class.getSimpleName();
	public static final String SHOW_ON_MAP_KEY = "show_on_map_key";
	public static final String SIMPLIFIED_TRACK_KEY = "simplified_track_key";
	public static final String DEST_FOLDER_PATH_KEY = "dest_folder_path_key";
	public static final String DEST_FILE_NAME_KEY = "dest_file_name_key";
	public static final String SHOW_SIMPLIFIED_BUTTON_KEY = "show_simplified_button_key";

	private OsmandApplication app;

	private FolderListAdapter adapter;
	private TextInputLayout nameTextBox;
	private RecyclerView recyclerView;

	private String destFileName;
	private String folderPath;
	private boolean showOnMap;
	private boolean simplifiedTrack;
	private boolean rightButtonEnabled = true;
	private boolean showSimplifiedButton = true;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();

		Context themedCtx = UiUtilities.getThemedContext(requireContext(), nightMode);
		int highlightColorId = nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light;
		if (savedInstanceState != null) {
			showOnMap = savedInstanceState.getBoolean(SHOW_ON_MAP_KEY);
			simplifiedTrack = savedInstanceState.getBoolean(SIMPLIFIED_TRACK_KEY);
			folderPath = savedInstanceState.getString(DEST_FOLDER_PATH_KEY);
			destFileName = savedInstanceState.getString(DEST_FILE_NAME_KEY);
			showSimplifiedButton = savedInstanceState.getBoolean(SHOW_SIMPLIFIED_BUTTON_KEY);
		}
		if (Algorithms.isEmpty(folderPath)) {
			folderPath = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
		}

		items.add(new TitleItem(getString(R.string.save_as_new_track)));

		View editNameView = View.inflate(themedCtx, R.layout.track_name_edit_text, null);
		nameTextBox = editNameView.findViewById(R.id.name_text_box);
		nameTextBox.setBoxBackgroundColorResource(highlightColorId);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_file_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ColorUtilities.getSecondaryTextColor(app, nightMode));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		TextInputEditText nameText = editNameView.findViewById(R.id.name_edit_text);
		nameText.setText(destFileName);
		nameText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				updateFileNameFromEditText(s.toString());
			}
		});
		BaseBottomSheetItem editFileName = new BaseBottomSheetItem.Builder()
				.setCustomView(editNameView)
				.create();
		this.items.add(editFileName);

		updateFileNameFromEditText(destFileName);

		int contentPaddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		int contentPaddingHalf = app.getResources().getDimensionPixelSize(R.dimen.content_padding_half);

		items.add(new DividerSpaceItem(app, contentPaddingSmall));

		View selectFolderView = View.inflate(themedCtx, R.layout.select_folder_row, null);
		selectFolderView.findViewById(R.id.select_folder_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				File dest = getFile(folderPath, destFileName);
				FragmentManager manager = activity.getSupportFragmentManager();
				MoveGpxFileBottomSheet.showInstance(manager, dest, null,
						SaveAsNewTrackBottomSheetDialogFragment.this, usedOnMap, true);
			}
		});
		BaseBottomSheetItem selectFolderItem = new BaseBottomSheetItem.Builder()
				.setCustomView(selectFolderView)
				.create();
		items.add(selectFolderItem);

		adapter = new FolderListAdapter(app, folderPath, nightMode);
		adapter.setItems(getAdapterItems());
		if (adapter.getItemCount() > 0) {
			adapter.setListener(createFolderSelectListener());
			View view = View.inflate(themedCtx, R.layout.bottom_sheet_item_recyclerview, null);
			recyclerView = view.findViewById(R.id.recycler_view);
			recyclerView.setPadding(contentPaddingHalf, 0, contentPaddingHalf, 0);
			BaseBottomSheetItem scrollItem = new HorizontalRecyclerBottomSheetItem.Builder()
					.setAdapter(adapter)
					.setCustomView(view)
					.create();
			this.items.add(scrollItem);

			items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.dialog_content_margin)));
		}

		int activeColorRes = ColorUtilities.getActiveColorId(nightMode);

		if (showSimplifiedButton) {
			BottomSheetItemWithCompoundButton[] simplifiedTrackItem = new BottomSheetItemWithCompoundButton[1];
			simplifiedTrackItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(simplifiedTrack)
					.setCompoundButtonColorId(activeColorRes)
					.setDescription(getSimplifiedTrackDescription())
					.setBackground(UiUtilities.getStrokedBackgroundForCompoundButton(app, R.color.activity_background_color_light,
							R.color.list_background_color_dark, simplifiedTrack, nightMode))
					.setTitle(getString(R.string.simplified_track))
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
					.setOnClickListener(v -> {
						simplifiedTrack = !simplifiedTrack;
						simplifiedTrackItem[0].setChecked(simplifiedTrack);
						AndroidUtils.setBackground(simplifiedTrackItem[0].getView(), UiUtilities.getStrokedBackgroundForCompoundButton(app, R.color.activity_background_color_light, R.color.list_background_color_dark, simplifiedTrack, nightMode));
						simplifiedTrackItem[0].setDescription(getSimplifiedTrackDescription());
					})
					.create();
			items.add(simplifiedTrackItem[0]);

			items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.content_padding)));
		}

		BottomSheetItemWithCompoundButton[] showOnMapItem = new BottomSheetItemWithCompoundButton[1];
		showOnMapItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColorId(activeColorRes)
				.setChecked(showOnMap)
				.setBackground(UiUtilities.getStrokedBackgroundForCompoundButton(app, R.color.activity_background_color_light,
						R.color.list_background_color_dark, showOnMap, nightMode))
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
				.setOnClickListener(v -> {
					showOnMap = !showOnMap;
					showOnMapItem[0].setChecked(showOnMap);
					AndroidUtils.setBackground(showOnMapItem[0].getView(),
							UiUtilities.getStrokedBackgroundForCompoundButton(app, R.color.activity_background_color_light,
									R.color.list_background_color_dark, showOnMap, nightMode));
				})
				.create();
		items.add(showOnMapItem[0]);

		items.add(new DividerSpaceItem(app, contentPaddingSmall));
	}

	@NonNull
	private List<Object> getAdapterItems() {
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<Object> items = new ArrayList<>(getFolders(gpxDir));
		items.add(VIEW_TYPE_ADD);
		return items;
	}

	@NonNull
	private String getSimplifiedTrackDescription() {
		return simplifiedTrack ? getString(R.string.simplified_track_description) : "";
	}

	@NonNull
	private FolderListAdapterListener createFolderSelectListener() {
		return new FolderListAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				folderPath = item;
				EditText editText = nameTextBox.getEditText();
				if (editText != null) {
					updateFileNameFromEditText(editText.getText().toString());
				}
			}

			@Override
			public void onAddNewItemSelected() {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AddNewTrackFolderBottomSheet.showInstance(activity.getSupportFragmentManager(),
							null, null, SaveAsNewTrackBottomSheetDialogFragment.this, usedOnMap);
				}
			}
		};
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(SHOW_ON_MAP_KEY, showOnMap);
		outState.putBoolean(SIMPLIFIED_TRACK_KEY, simplifiedTrack);
		outState.putString(DEST_FOLDER_PATH_KEY, folderPath);
		outState.putString(DEST_FILE_NAME_KEY, destFileName);
		outState.putBoolean(SHOW_SIMPLIFIED_BUTTON_KEY, showSimplifiedButton);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof SaveAsNewTrackFragmentListener) {
			((SaveAsNewTrackFragmentListener) targetFragment)
					.onSaveAsNewTrack(folderPath, destFileName, showOnMap, simplifiedTrack);
		}
		dismiss();
	}

	@NonNull
	private File getFile(@NonNull String folderName, @NonNull String fileName) {
		File dir = new File(folderName);
		return new File(dir, fileName + GPX_FILE_EXT);
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
			nameTextBox.setErrorEnabled(false);
			rightButtonEnabled = true;
		}
		destFileName = text;
		updateBottomButtons();
	}

	private boolean isFileExist(String name) {
		return getFile(folderPath, name).exists();
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		File destFolder = dest.getParentFile();
		if (destFolder != null) {
			folderPath = destFolder.getAbsolutePath();
			boolean newFolder = destFolder.mkdirs();
			List<Object> items = getAdapterItems();
			if (newFolder) {
				adapter.setItems(items);
			}
			adapter.setSelectedItem(folderPath);
			adapter.notifyDataSetChanged();

			int position = items.indexOf(folderPath);
			if (position != -1) {
				recyclerView.scrollToPosition(position);
			}
			updateFileNameFromEditText(Algorithms.getFileNameWithoutExtension(dest.getName()));
		}
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light;
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		File file = getFile(folderPath, destFileName);
		File destFolder = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), folderName);
		this.onFileMove(file, new File(destFolder, file.getName()));
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull String destFileName,
	                                @Nullable Fragment target,
	                                boolean showSimplifiedButton,
	                                boolean showOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SaveAsNewTrackBottomSheetDialogFragment fragment = new SaveAsNewTrackBottomSheetDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.destFileName = destFileName;
			fragment.showSimplifiedButton = showSimplifiedButton;
			fragment.showOnMap = showOnMap;
			fragment.show(manager, TAG);
		}
	}

	public interface SaveAsNewTrackFragmentListener {

		void onSaveAsNewTrack(@NonNull String folderPath, @NonNull String fileName, boolean showOnMap, boolean simplifiedTrack);

	}
}