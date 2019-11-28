package net.osmand.plus.monitoring;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.download.ui.LocalIndexesFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.download.ui.LocalIndexesFragment.ILLEGAL_FILE_NAME_CHARACTERS;

public class OnSaveCurrentTrackFragment extends BottomSheetDialogFragment {

	public static final String TAG = "OnSaveCurrentTrackBottomSheetFragment";
	public static final String SAVED_TRACKS_KEY = "saved_track_filename";

	private boolean showOnMap = true;
	private boolean openTrack = false;
	private File file;
	private String savedGpxDir = "";
	private String savedGpxName = "";
	private String newGpxName = "";

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final OsmandApplication app = requiredMyApplication();
		Bundle args = getArguments();
		if (args != null && args.containsKey(SAVED_TRACKS_KEY)) {
			ArrayList<String> savedGpxNames = args.getStringArrayList(SAVED_TRACKS_KEY);
			if (savedGpxNames != null && savedGpxNames.size() > 0) {
				String fileName = savedGpxNames.get(savedGpxNames.size() - 1);
				savedGpxDir = new File(fileName).getParent();
				savedGpxName = new File(fileName).getName();
				newGpxName = this.savedGpxName;
			}
		} else {
			dismiss();
		}

		Context ctx = requireContext();
		file = new File(app.getAppCustomization().getTracksDir(), savedGpxName + ".gpx");
		final boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final int textPrimaryColor = nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		View mainView = UiUtilities.getInflater(ctx, nightMode).inflate(R.layout.fragment_on_save_current_track, container);

		OsmandTextFieldBoxes textBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.name_text_box);
		if (nightMode) {
			textBox.setPrimaryColor(ContextCompat.getColor(app, R.color.active_color_primary_dark));
		}

		final EditText nameEditText = (EditText) mainView.findViewById(R.id.name_edit_text);
		nameEditText.setText(savedGpxName);
		nameEditText.setTextColor(ContextCompat.getColor(ctx, textPrimaryColor));
		textBox.activate(true);
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
		nameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
					doRename(false);
					return true;
				}
				return false;
			}
		});

		SwitchCompat showOnMapButton = (SwitchCompat) mainView.findViewById(R.id.btn_show_on_map);
		showOnMapButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				showOnMap = isChecked;
			}
		});
		View openTrackBtn = mainView.findViewById(R.id.open_track_button);
		UiUtilities.setupDialogButton(nightMode, openTrackBtn, DialogButtonType.SECONDARY, R.string.shared_string_open_track);
		final View showOnMapBtn = mainView.findViewById(R.id.close_button);
		UiUtilities.setupDialogButton(nightMode, showOnMapBtn, DialogButtonType.SECONDARY, R.string.shared_string_close);

		openTrackBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doRename(true);
			}
		});

		showOnMapBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doRename(false);
			}
		});
		return mainView;
	}

	private void doRename(boolean openTrack) {
		file = renameGpxFile();
		if (file != null) {
			this.openTrack = openTrack;
			dismiss();
		}
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (file != null) {
			if (showOnMap) {
				showOnMap(file, !openTrack);
			}
			FragmentActivity activity = getActivity();
			if (openTrack && activity != null) {
				AvailableGPXFragment.openTrack(activity, file);
			}
		}
	}

	private File renameGpxFile() {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return null;
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		File savedFile = new File(app.getAppCustomization().getTracksDir(), new File(savedGpxDir, savedGpxName + ".gpx").getPath());
		if (savedGpxName.equalsIgnoreCase(newGpxName)) {
			return savedFile;
		}
		if (Algorithms.isEmpty(newGpxName)) {
			Toast.makeText(app, R.string.empty_filename, Toast.LENGTH_LONG).show();
			return null;
		}
		return LocalIndexesFragment.renameGpxFile(app, savedFile, newGpxName + ".gpx", true, null);
	}

	private void showOnMap(File f, boolean animated) {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();

		GpxInfo gpxInfo = new GpxInfo();
		gpxInfo.setGpx(GPXUtilities.loadGPXFile(f));
		if (gpxInfo.gpx != null) {
			WptPt loc = gpxInfo.gpx.findPointToShow();
			if (loc != null) {
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxInfo.gpx);
				if (activity instanceof MapActivity) {
					MapActivity mapActivity = (MapActivity) activity;
					if (animated) {
						OsmandMapTileView mapView = mapActivity.getMapView();
						mapView.getAnimatedDraggingThread().startMoving(loc.lat, loc.lon, mapView.getZoom(), true);
						mapView.refreshMap();
					} else {
						mapActivity.setMapLocation(loc.lat, loc.lon);
					}
				}
			}
		}
	}

	public static void showInstance(FragmentManager fragmentManager, List<String> filenames) {
		OnSaveCurrentTrackFragment f = new OnSaveCurrentTrackFragment();
		Bundle b = new Bundle();
		b.putStringArrayList(SAVED_TRACKS_KEY, new ArrayList<>(filenames));
		f.setArguments(b);
		f.show(fragmentManager, TAG);
	}
}
