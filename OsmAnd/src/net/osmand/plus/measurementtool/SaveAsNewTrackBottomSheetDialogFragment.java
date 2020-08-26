package net.osmand.plus.measurementtool;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
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

import org.apache.commons.logging.Log;

public class SaveAsNewTrackBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SaveAsNewTrackBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SaveAsNewTrackBottomSheetDialogFragment.class);
	public static final String SHOW_ON_MAP_KEY = "show_on_map_key";
	public static final String SIMPLIFIED_TRACK_KEY = "simplified_track_key";
	public static final String FOLDER_NAME_KEY = "folder_name_key";

	boolean showOnMap;
	boolean simplifiedTrack;
	String fileName;
	String folderName;
	boolean rightButtonEnabled = true;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		if (savedInstanceState != null) {
			showOnMap = savedInstanceState.getBoolean(SHOW_ON_MAP_KEY);
			simplifiedTrack = savedInstanceState.getBoolean(SIMPLIFIED_TRACK_KEY);
			folderName = savedInstanceState.getString(FOLDER_NAME_KEY);
		}

		items.add(new TitleItem(getString(R.string.shared_string_save_as_gpx)));

		View editNameView = View.inflate(UiUtilities.getThemedContext(app, nightMode),
				R.layout.track_name_edit_text, null);
		final TextInputLayout nameTextBox = editNameView.findViewById(R.id.name_text_box);
		nameTextBox.setBoxBackgroundColorResource(R.color.material_text_input_layout_bg);
		nameTextBox.setHint(getString(R.string.file_name));
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
				checkEmptyName(s, nameTextBox);
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

		int activeColorRes = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int backgroundColor = AndroidUtils.getColorFromAttr(UiUtilities.getThemedContext(app, nightMode),
				R.attr.activity_background_color);
		GradientDrawable background = (GradientDrawable) AppCompatResources.getDrawable(app,
				R.drawable.bg_select_group_button_outline);
		if (background != null) {
			background = (GradientDrawable) background.mutate();
			background.setStroke(0, Color.TRANSPARENT);
			background.setColor(backgroundColor);
		}
		final BottomSheetItemWithCompoundButton[] simplifiedTrackItem = new BottomSheetItemWithCompoundButton[1];
		simplifiedTrackItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(simplifiedTrack)
				.setCompoundButtonColorId(activeColorRes)
				.setDescription(getString(R.string.simplified_track_description))
				.setBackground(background)
				.setTitle(getString(R.string.simplified_track))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						simplifiedTrack = !simplifiedTrack;
						simplifiedTrackItem[0].setChecked(simplifiedTrack);
					}
				})
				.create();
		items.add(simplifiedTrackItem[0]);

		items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.content_padding)));

		background = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
		if (background != null) {
			background = (GradientDrawable) background.mutate();
			background.setStroke(app.getResources().getDimensionPixelSize(R.dimen.map_button_stroke), backgroundColor);
		}
		final BottomSheetItemWithCompoundButton[] showOnMapItem = new BottomSheetItemWithCompoundButton[1];
		showOnMapItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColorId(activeColorRes)
				.setChecked(showOnMap)
				.setBackground(background)
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_and_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOnMap = !showOnMap;
						showOnMapItem[0].setChecked(showOnMap);
					}
				})
				.create();
		items.add(showOnMapItem[0]);

		items.add(new DividerSpaceItem(app, contentPaddingSmall));
	}

	private FolderListAdapter.FolderListAdapterListener createFolderSelectListener() {
		return new FolderListAdapter.FolderListAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				folderName = item;
			}
		};
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(SHOW_ON_MAP_KEY, showOnMap);
		outState.putBoolean(SIMPLIFIED_TRACK_KEY, simplifiedTrack);
		outState.putString(FOLDER_NAME_KEY, folderName);
		super.onSaveInstanceState(outState);
	}

	public static void showInstance(@NonNull FragmentManager fm, @Nullable Fragment targetFragment, String fileName) {
		try {
			if (!fm.isStateSaved()) {
				SaveAsNewTrackBottomSheetDialogFragment fragment = new SaveAsNewTrackBottomSheetDialogFragment();
				fragment.setTargetFragment(targetFragment, 0);
				fragment.fileName = fileName;
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
		}
		dismiss();
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return rightButtonEnabled;
	}

	private void checkEmptyName(Editable name, TextInputLayout nameCaption) {
		if (name.toString().trim().isEmpty()) {
			nameCaption.setError(getString(R.string.empty_filename));
			rightButtonEnabled = false;
		} else {
			nameCaption.setError(null);
			rightButtonEnabled = true;
		}
		updateBottomButtons();
	}

	interface SaveAsNewTrackFragmentListener {

		void onSaveAsNewTrack(String folderName, String fileName, boolean showOnMap, boolean simplifiedTrack);

	}
}
