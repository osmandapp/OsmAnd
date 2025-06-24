package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.utils.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

public class SaveGPXBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SaveGPXBottomSheetFragment";

	private static final Log LOG = PlatformUtil.getLog(SaveGPXBottomSheet.class);

	private static final String FILE_PATH_KEY = "file_key";
	private static final String OPEN_TRACK_ATTR = "open_track";
	private static final String SHOW_ON_MAP_ATTR = "show_on_map";

	private OsmandApplication app;

	private File file;
	private String newGpxName = "";
	private String initialGpxName = "";

	private boolean openTrack;
	private boolean showOnMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = requiredMyApplication();

		Bundle args = getArguments();
		if (args != null && args.containsKey(FILE_PATH_KEY)) {
			file = new File(args.getString(FILE_PATH_KEY));
			initialGpxName = Algorithms.getFileNameWithoutExtension(file);
			newGpxName = initialGpxName;
		} else {
			dismiss();
		}
		if (savedInstanceState != null) {
			openTrack = savedInstanceState.getBoolean(OPEN_TRACK_ATTR);
			showOnMap = savedInstanceState.getBoolean(SHOW_ON_MAP_ATTR);
		} else {
			showOnMap = app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null;
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = requireContext();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int textPrimaryColor = ColorUtilities.getPrimaryTextColorId(nightMode);
		View view = UiUtilities.getInflater(ctx, nightMode).inflate(R.layout.save_gpx_fragment, null);

		OsmandTextFieldBoxes textBox = view.findViewById(R.id.name_text_box);
		if (nightMode) {
			textBox.setPrimaryColor(ContextCompat.getColor(app, R.color.active_color_primary_dark));
		}
		int iconColor = ColorUtilities.getDefaultIconColorId(nightMode);
		textBox.setClearButton(getIcon(R.drawable.ic_action_remove_circle, iconColor));

		EditText nameEditText = view.findViewById(R.id.name_edit_text);
		nameEditText.setText(initialGpxName);
		nameEditText.setTextColor(ContextCompat.getColor(ctx, textPrimaryColor));
		nameEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				newGpxName = s.toString();
			}

			@Override
			public void afterTextChanged(Editable s) {
				Editable text = nameEditText.getText();
				if (text.length() >= 1) {
					if (ILLEGAL_FILE_NAME_CHARACTERS.matcher(text).find()) {
						nameEditText.setError(app.getString(R.string.file_name_containes_illegal_char));
					}
				}
			}
		});
		nameEditText.setOnEditorActionListener((v, actionId, event) -> {
			if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
					|| (actionId == EditorInfo.IME_ACTION_DONE)) {
				finish();
				return true;
			}
			return false;
		});

		nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					nameEditText.setSelection(nameEditText.getText().length());
					AndroidUtils.showSoftKeyboard(activity, nameEditText);
				}
			}
		});

		SwitchCompat showOnMapButton = view.findViewById(R.id.btn_show_on_map);
		showOnMapButton.setChecked(showOnMap);
		showOnMapButton.setOnCheckedChangeListener((buttonView, isChecked) -> showOnMap = !showOnMap);

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(titleItem);
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_open_track;
	}

	@Override
	protected void onRightBottomButtonClick() {
		openTrack = true;
		finish();
	}

	@Override
	protected void onDismissButtonClickAction() {
		finish();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(OPEN_TRACK_ATTR, openTrack);
		outState.putBoolean(SHOW_ON_MAP_ATTR, showOnMap);
	}

	private void finish() {
		if (processFileName()) {
			dismiss();
		}
	}

	private boolean processFileName() {
		if (Algorithms.isBlank(newGpxName)) {
			app.showToastMessage(R.string.empty_filename);
			return false;
		}
		if (!initialGpxName.equalsIgnoreCase(newGpxName)) {
			File dest = FileUtils.renameGpxFile(app, file, newGpxName + IndexConstants.GPX_FILE_EXT, true, null);
			if (dest != null) {
				file = dest;
			} else {
				return false;
			}
		}
		return file != null;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (file == null || activity == null || activity.isChangingConfigurations()) {
			return;
		}
		if (openTrack) {
			TrackMenuFragment.openTrack(activity, file, null);
			return;
		}
		if (showOnMap) {
			showOnMap(activity, file);
		}
	}

	private void showOnMap(@NonNull FragmentActivity activity, @NonNull File file) {
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
		if (selectedGpxFile != null) {
			moveMap(activity, selectedGpxFile.getGpxFile());
		} else {
			GpxFileLoaderTask.loadGpxFile(file, activity, gpxFile -> {
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
				moveMap(activity, gpxFile);
				return true;
			});
		}
	}

	private void moveMap(@NonNull FragmentActivity activity, @NonNull GpxFile gpxFile) {
		WptPt point = gpxFile.findPointToShow();
		if (point != null) {
			if (AndroidUtils.isActivityNotDestroyed(activity) && activity instanceof MapActivity) {
				OsmandMapTileView mapView = app.getOsmandMap().getMapView();
				mapView.getAnimatedDraggingThread().startMoving(point.getLat(), point.getLon());
				mapView.refreshMap();
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull File file) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(FILE_PATH_KEY, file.getAbsolutePath());

			SaveGPXBottomSheet fragment = new SaveGPXBottomSheet();
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}
}