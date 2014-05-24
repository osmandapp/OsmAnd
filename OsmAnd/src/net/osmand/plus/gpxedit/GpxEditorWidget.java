package net.osmand.plus.gpxedit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author Koen Rabaey
 */
public class GpxEditorWidget extends TextInfoWidget {

	private final MapActivity _mapActivity;

	//survive screen orientation changes
	private final GpxEditorStates _states;

	private GPXUtilities.GPXFile _loadedGpx;

	private MapWidgetRegistry.MapWidgetRegInfo _topWidget;

	//to hold the controls when in stated mode
	private Set<MapWidgetRegistry.MapWidgetRegInfo> _otherControls = new HashSet<MapWidgetRegistry.MapWidgetRegInfo>();

	public GpxEditorWidget(final MapActivity ctx, final GpxEditorStates states, final Paint textPaint, final Paint subtextPaint, MapWidgetRegistry.MapWidgetRegInfo topWidget) {
		super(ctx, 0, textPaint, subtextPaint);
		_mapActivity = ctx;
		_states = states;
		_topWidget = topWidget;
		setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final List<GpxSelectionHelper.SelectedGpxFile> selectedGPXFiles = _mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
				createDialog(selectedGPXFiles.isEmpty() ? null : selectedGPXFiles.get(0).getGpxFile()).show();
			}
		});
		setImageDrawable(ctx.getResources().getDrawable(R.drawable.ic_action_polygom_dark));
		refreshDisplay(GpxEditorStates.Mode.START);
	}

	public GPXUtilities.GPXFile getLoadedGpx() {
		return _loadedGpx;
	}

	private void startEditingHelp(MapActivity ctx) {
		final OsmandSettings.CommonPreference<Boolean> pref = _mapActivity.getMyApplication().getSettings().registerBooleanPreference("gpx_edit_help", true);
		pref.makeGlobal();
		if (pref.get()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(R.string.gpx_edit_help);
			builder.setNegativeButton(R.string.default_buttons_do_not_show_again, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					pref.set(false);
				}
			});
			builder.setPositiveButton(R.string.default_buttons_ok, null);

			builder.show();
		}

	}

	private void loadGpx() {
		GpxUiHelper.selectGPXFile(_mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {

			@Override
			public boolean processResult(final GPXUtilities.GPXFile[] result) {
				if (result != null && result.length > 0) {
					_mapActivity.getMyApplication().getSelectedGpxHelper().selectGpxFile(result[0], true, false);
					useGpxFile(result[0]);
					return true;
				}
				return false;
			}
		});
	}

	private void useGpxFile(GPXUtilities.GPXFile result) {
		_loadedGpx = result;
		final GpxEditorModel state = _states.createNew();
		final GPXUtilities.WptPt show = state.resetWith(result);
		if (show != null) {
			showMap(state);
		}
		_states.push(state);
		refreshDisplay(GpxEditorStates.Mode.EDIT_ROUTE);
	}

	private void newGpx() {
		final AlertDialog.Builder b = new AlertDialog.Builder(_mapActivity);

		final LinearLayout ll = (LinearLayout) LayoutInflater.from(_mapActivity).inflate(R.layout.gpx_edit_new, null);
		final EditText fileNameText = (EditText) ll.findViewById(R.id.gpx_edit_new_name);

		b.setView(ll);
		b.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final String newName = fileNameText.getText().toString();
				GPXUtilities.GPXFile loadedGpx = new GPXUtilities.GPXFile();
				loadedGpx.path = newName;
				GpxEditorWidget.this._loadedGpx = loadedGpx;
				startEditingHelp(_mapActivity);
				refreshDisplay(GpxEditorStates.Mode.EDIT_ROUTE);
			}
		});
		b.setNegativeButton(R.string.default_buttons_cancel, null);
		b.show();
	}

	private void saveGpx() {
		final AlertDialog.Builder b = new AlertDialog.Builder(_mapActivity);
		final File dir = _mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);

		final LinearLayout ll = (LinearLayout) LayoutInflater.from(_mapActivity).inflate(R.layout.gpx_edit_save, null);

		final EditText fileNameText = (EditText) ll.findViewById(R.id.gpx_edit_save_file_name);
		final EditText authorText = (EditText) ll.findViewById(R.id.gpx_edit_save_author);
		final TextView warningText = (TextView) ll.findViewById(R.id.gpx_edit_save_warning);

		if (_loadedGpx != null) {
			fileNameText.setText(new File(_loadedGpx.path).getName());
			authorText.setText(_loadedGpx.author);
		}
		fileNameText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				boolean e = false;
				try {
					e = new File(dir, s.toString()).exists() || new File(dir, s.toString() + ".gpx").exists();
				} catch (Exception e1) {
					//ignore
				}
				if (e) {
					warningText.setText(R.string.file_with_name_already_exists);
				} else {
					warningText.setText("");
				}
			}
		});
		b.setView(ll);
		b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String newName = fileNameText.getText().toString();
				if (!newName.endsWith(".gpx")) {
					newName += ".gpx";
				}
				saveGpx(newName, authorText.getText().toString());
			}
		});
		b.setNegativeButton(R.string.default_buttons_cancel, null);
		b.show();
	}

	private void saveGpx(final String fileNameSave, final String author) {
		new AsyncTask<Void, Void, String>() {
			private ProgressDialog dlg;
			private File toSave;

			@Override
			protected String doInBackground(Void... params) {
				toSave = new File(_mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR), fileNameSave);

				final GPXUtilities.GPXFile gpxFile = _states.current().asGpx(_loadedGpx);
				gpxFile.author = author;
				if (_loadedGpx != null) {
					_mapActivity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpxFile, true, false);
				}
				return GPXUtilities.writeGpxFile(toSave, gpxFile, _mapActivity.getMyApplication());
			}

			@Override
			protected void onPreExecute() {
				dlg = new ProgressDialog(_mapActivity);
				dlg.setMessage(_mapActivity.getString(R.string.saving_gpx_tracks));
				dlg.show();
			}

			@Override
			protected void onPostExecute(String warning) {
				if (warning == null) {
					AccessibleToast.makeText(_mapActivity,
							MessageFormat.format(_mapActivity.getString(R.string.gpx_saved_sucessfully), toSave.getAbsolutePath()),
							Toast.LENGTH_LONG).show();
					resetToStart();
				} else {
					AccessibleToast.makeText(_mapActivity, warning, Toast.LENGTH_LONG).show();
				}
				if (dlg != null && dlg.isShowing()) {
					dlg.dismiss();
				}
			}
		}.execute();
	}

	private void resetToStart() {
		_states.clear();
		_loadedGpx = null;
		refreshDisplay(GpxEditorStates.Mode.START);
	}

	/**
	 * Updates the text shown on the widget in the map
	 */
	public void refreshDisplay(final GpxEditorStates.Mode mode) {
		if (mode != _states.getMode()) {
			if (_states.getMode() == GpxEditorStates.Mode.START) {
				//start editing

				//save the current controls
				final Set<MapWidgetRegistry.MapWidgetRegInfo> topWidgets = _mapActivity.getMapLayers().getMapInfoLayer().getMapInfoControls().getTopWidgets();
				_otherControls.addAll(topWidgets);

				//only show gpx edit control
				topWidgets.clear();
				topWidgets.add(_topWidget);
				_mapActivity.getMapLayers().getMapInfoLayer().recreateControls();
			} else if (mode == GpxEditorStates.Mode.START) {
				//stop editing

				//show the saved controls
				final Set<MapWidgetRegistry.MapWidgetRegInfo> topWidgets = _mapActivity.getMapLayers().getMapInfoLayer().getMapInfoControls().getTopWidgets();
				topWidgets.clear();
				topWidgets.addAll(_otherControls);
				_mapActivity.getMapLayers().getMapInfoLayer().recreateControls();
			}
			_states.setMode(mode);

		}
		final GpxEditorModel current = _states.current();
		float dst;

		switch (_states.getMode()) {
			case START:
				setText(_mapActivity.getString(R.string.gpx_edit_start), null);
				break;
			case EDIT_POI:
				setText(_mapActivity.getString(R.string.gpx_edit_track_favourite_short), null);
				break;
			case EDIT_ROUTE:
				dst = current == null ? 0F : current.getRouteDistance();
				setText(dst);
				break;
			case EDIT_TRACK:
				dst = current == null ? 0F : current.getTrackDistance();
				setText(dst);
				break;
			case DELETE:
				setText(_mapActivity.getString(R.string.gpx_edit_delete), null);
				break;
		}
		setImageDrawable(_mapActivity.getResources().getDrawable(getResIcon(_states.getMode())));
	}

	private void setText(float dst) {
		final String ds = OsmAndFormatter.getFormattedDistance(dst, _mapActivity.getMyApplication());
		final int ls = ds.lastIndexOf(' ');
		if (ls == -1) {
			setText(ds, null);
		} else {
			setText(ds.substring(0, ls), ds.substring(ls + 1));
		}
	}

	private int getResIcon(final GpxEditorStates.Mode mode) {
		final boolean dark = !isLightActionBar();
		int icon;
		switch (mode) {
			case START:
				icon = dark ? R.drawable.ic_action_polygom_dark : R.drawable.ic_action_polygom_light;
				break;
			case EDIT_TRACK:
				icon = R.drawable.widget_gpx_edit_track;
				break;
			case EDIT_ROUTE:
				icon = R.drawable.widget_gpx_edit_route;
				break;
			case EDIT_POI:
				icon = R.drawable.widget_gpx_edit_favourite;
				break;
			case DELETE:
				icon = dark ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_delete_light;
				break;
			default:
				icon = 0;
				break;
		}
		return icon;
	}

	private AlertDialog createDialog(final GPXUtilities.GPXFile displayGpx) {
		final ContextMenuAdapter adapter = new ContextMenuAdapter(_mapActivity);

		switch (_states.getMode()) {
			case START:
				if (displayGpx != null) {
					adapter.item(R.string.gpx_edit_current_gpx).icons(R.drawable.ic_action_polygom_dark, R.drawable.ic_action_polygom_light).reg();
				}
				adapter.item(R.string.gpx_edit_load_gpx).icons(R.drawable.ic_action_grefresh_dark, R.drawable.ic_action_grefresh_light).reg();
				adapter.item(R.string.gpx_edit_new_gpx).icons(R.drawable.ic_action_plus_dark, R.drawable.ic_action_plus_light).reg();
				break;
			default:
				adapter.item(R.string.gpx_edit_save_gpx).icons(R.drawable.ic_action_gsave_dark, R.drawable.ic_action_gsave_light).reg();
				adapter.item(R.string.gpx_edit_reset).icons(R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_light).reg();
				break;
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(_mapActivity);
		final ListAdapter listAdapter;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			listAdapter =
					adapter.createListAdapter(_mapActivity, R.layout.list_menu_item, _mapActivity.getMyApplication().getSettings().isLightContentMenu());
		} else {
			listAdapter =
					adapter.createListAdapter(_mapActivity, R.layout.list_menu_item_native, _mapActivity.getMyApplication().getSettings().isLightContentMenu());
		}
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				int id = adapter.getItemId(which);
				ContextMenuAdapter.OnContextMenuClick click = adapter.getClickAdapter(which);
				if (click != null) {
					click.onContextMenuClick(id, which, false, dialog);
				} else if (id == R.string.gpx_edit_new_gpx) {
					newGpx();
				} else if (id == R.string.gpx_edit_load_gpx) {
					loadGpx();
				} else if (id == R.string.gpx_edit_current_gpx) {
					useGpxFile(displayGpx);
				} else if (id == R.string.gpx_edit_reset) {
					resetToStart();
				} else if (id == R.string.gpx_edit_save_gpx) {
					saveGpx();
				}
				_mapActivity.getMapView().refreshMap();
				refreshDisplay(_states.getMode());
			}
		});
		return builder.create();
	}

	private boolean isLightActionBar() {
		return _mapActivity.getMyApplication().getSettings().isLightActionBar();
	}

	public void showMap(final GpxEditorModel state) {

		//move to point
		final OsmandMapTileView mapView = _mapActivity.getMapView();

		QuadRect box = state.getBoundingBox();
		if (box == null) return;

		final RotatedTileBox tb = new RotatedTileBox(mapView.getCurrentRotatedTileBox());
		tb.setPixelDimensions(3 * tb.getPixWidth() / 4, 3 * tb.getPixHeight() / 4);

		final double clat = box.bottom / 2 + box.top / 2;
		final double clon = box.left / 2 + box.right / 2;
		tb.setLatLonCenter(clat, clon);
		while (tb.getZoom() >= 7 && (!tb.containsLatLon(box.top, box.left) || !tb.containsLatLon(box.bottom, box.right))) {
			tb.setZoom(tb.getZoom() - 1);
		}

		mapView.getAnimatedDraggingThread().startMoving(clat, clon, tb.getZoom(), true);
	}
}