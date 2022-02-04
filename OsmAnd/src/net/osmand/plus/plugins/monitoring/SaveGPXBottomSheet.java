package net.osmand.plus.plugins.monitoring;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static net.osmand.plus.utils.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;

public class SaveGPXBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SaveGPXBottomSheetFragment";

	private static final Log LOG = PlatformUtil.getLog(SaveGPXBottomSheet.class);

	private static final String KEY_FILE_NAME = "file_name";

	private boolean openTrack = false;
	private File savedGpxFile;
	private String initialGpxName = "";
	private String newGpxName = "";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = requiredMyApplication();
		Bundle args = getArguments();
		if (args != null && args.containsKey(KEY_FILE_NAME)) {
			String fileName = args.getString(KEY_FILE_NAME);
			savedGpxFile = new File(fileName);
			initialGpxName = Algorithms.getFileNameWithoutExtension(savedGpxFile);
			newGpxName = initialGpxName;
		} else {
			dismiss();
		}

		Context ctx = requireContext();
		final boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final int textPrimaryColor = ColorUtilities.getPrimaryTextColorId(nightMode);
		View mainView = UiUtilities.getInflater(ctx, nightMode).inflate(R.layout.save_gpx_fragment, null);

		OsmandTextFieldBoxes textBox = mainView.findViewById(R.id.name_text_box);
		if (nightMode) {
			textBox.setPrimaryColor(ContextCompat.getColor(app, R.color.active_color_primary_dark));
		}
		int iconColor = ColorUtilities.getDefaultIconColorId(nightMode);
		textBox.setClearButton(getIcon(R.drawable.ic_action_remove_circle, iconColor));

		final EditText nameEditText = mainView.findViewById(R.id.name_edit_text);
		nameEditText.setText(initialGpxName);
		nameEditText.setTextColor(ContextCompat.getColor(ctx, textPrimaryColor));
		nameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

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
				doRename(false);
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

		SwitchCompat showOnMapButton = mainView.findViewById(R.id.btn_show_on_map);
		showOnMapButton.setChecked(app.getSettings().SHOW_SAVED_TRACK_REMEMBER.get());
		showOnMapButton.setOnCheckedChangeListener((buttonView, isChecked) ->
				app.getSettings().SHOW_SAVED_TRACK_REMEMBER.set(isChecked));

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(titleItem);
	}

	private void doRename(boolean openTrack) {
		savedGpxFile = tryRenameGpxFile();
		if (savedGpxFile != null) {
			this.openTrack = openTrack;
			dismiss();
		}
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.SECONDARY;
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
		doRename(true);
	}

	@Override
	protected void onDismissButtonClickAction() {
		doRename(false);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (savedGpxFile != null && activity != null) {
			boolean showTrack = requiredMyApplication().getSettings().SHOW_SAVED_TRACK_REMEMBER.get();
			if (showTrack) {
				showOnMap(savedGpxFile, !openTrack);
			}
			if (openTrack) {
				TrackMenuFragment.openTrack(activity, savedGpxFile, null);
			}
		}
	}

	@Nullable
	private File tryRenameGpxFile() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return null;
		} else if (initialGpxName.equalsIgnoreCase(newGpxName)) {
			return savedGpxFile;
		} else if (Algorithms.isBlank(newGpxName)) {
			Toast.makeText(app, R.string.empty_filename, Toast.LENGTH_LONG).show();
			return null;
		}
		return FileUtils.renameGpxFile(app, savedGpxFile, newGpxName + IndexConstants.GPX_FILE_EXT,
				true, null);
	}

	private void showOnMap(File f, boolean animated) {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();

		AvailableGPXFragment.GpxInfo gpxInfo = new AvailableGPXFragment.GpxInfo();
		gpxInfo.setGpx(GPXUtilities.loadGPXFile(f));
		if (gpxInfo.gpx != null) {
			GPXUtilities.WptPt loc = gpxInfo.gpx.findPointToShow();
			if (loc != null) {
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxInfo.gpx);
				if (activity instanceof MapActivity) {
					MapActivity mapActivity = (MapActivity) activity;
					if (animated) {
						OsmandMapTileView mapView = mapActivity.getMapView();
						mapView.getAnimatedDraggingThread().startMoving(loc.lat, loc.lon, mapView.getZoom(), true);
						mapView.refreshMap();
					} else {
						app.getOsmandMap().setMapLocation(loc.lat, loc.lon);
					}
				}
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull String fileName) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SaveGPXBottomSheet fragment = new SaveGPXBottomSheet();
			Bundle args = new Bundle();
			args.putString(KEY_FILE_NAME, fileName);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}
}