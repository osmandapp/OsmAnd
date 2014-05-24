package net.osmand.plus.gpxedit;


import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.UpdateableWidget;

import java.io.File;

/**
 * Author Koen Rabaey
 */
public class GpxEditorTopWidget extends LinearLayout implements UpdateableWidget, View.OnClickListener {
	private GpxEditorStates _states;
	private GpxEditorWidget _widget;
	private MapActivity _mapActivity;
	private GpxEditorLayer _layer;

	public GpxEditorTopWidget(MapActivity context) {
		this(context, null);
	}

	public GpxEditorTopWidget(MapActivity context, AttributeSet attrs) {
		super(context, attrs);
		_mapActivity = context;
		LayoutInflater.from(getContext()).inflate(R.layout.gpx_editor_top_view, this, true);
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.FILL_VERTICAL);
	}

	public void init(final GpxEditorStates states, final GpxEditorWidget widget, final GpxEditorLayer gpxEditorLayer) {
		_states = states;
		_widget = widget;
		_layer = gpxEditorLayer;

		findViewById(R.id.widget_gpx_edit_undo).setOnClickListener(this);
		findViewById(R.id.widget_gpx_edit_redo).setOnClickListener(this);
		findViewById(R.id.widget_gpx_edit_poi).setOnClickListener(this);
		findViewById(R.id.widget_gpx_edit_route).setOnClickListener(this);
		findViewById(R.id.widget_gpx_edit_track).setOnClickListener(this);
		findViewById(R.id.widget_gpx_edit_delete).setOnClickListener(this);
		findViewById(R.id.widget_gpx_edit_favourite).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.widget_gpx_edit_undo:
				onUndoClick();
				break;
			case R.id.widget_gpx_edit_redo:
				onRedoClick();
				break;
			case R.id.widget_gpx_edit_track:
				onTrackClick();
				break;
			case R.id.widget_gpx_edit_poi:
				onPoiClick();
				break;
			case R.id.widget_gpx_edit_route:
				onRouteClick();
				break;
			case R.id.widget_gpx_edit_delete:
				onDeleteClick();
				break;
			case R.id.widget_gpx_edit_favourite:
				onFavouriteClick();
				break;
		}
	}

	@Override
	public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
		final boolean start = _states.getMode() == GpxEditorStates.Mode.START;
		final boolean delete = _states.getMode() == GpxEditorStates.Mode.DELETE;
		final boolean hasTrack = _states.hasCurrent() && !_states.current().getTrackPoints().isEmpty();

		findViewById(R.id.widget_gpx_edit_undo).setVisibility(!start && _states.canUndo() ? VISIBLE : INVISIBLE);
		findViewById(R.id.widget_gpx_edit_redo).setVisibility(!start && _states.canRedo() ? VISIBLE : INVISIBLE);
		findViewById(R.id.widget_gpx_edit_route).setVisibility(!start ? VISIBLE : INVISIBLE);
		findViewById(R.id.widget_gpx_edit_poi).setVisibility(!start ? VISIBLE : INVISIBLE);
		findViewById(R.id.widget_gpx_edit_track).setVisibility(!start && hasTrack ? VISIBLE : INVISIBLE);
		findViewById(R.id.widget_gpx_edit_delete).setVisibility(!start && !delete && _states.hasCurrent() && !_states.current().isEmpty() ? VISIBLE : INVISIBLE);
		findViewById(R.id.widget_gpx_edit_favourite).setVisibility(!start && !delete ? VISIBLE : INVISIBLE);

		final GPXUtilities.GPXFile loadedGpx = _widget.getLoadedGpx();
		final String name = loadedGpx == null ? "" : new File(loadedGpx.path).getName();
		((TextView) findViewById(R.id.gpx_edit_gpx_file_name)).setText(name);

		return false;
	}

	private void onRedoClick() {
		_states.redo();
		_widget.refreshDisplay(_states.getMode());
//		GpxEditorUtil.createToast(_mapActivity, R.drawable.widget_gpx_edit_redo, _mapActivity.getString(R.string.gpx_edit_redo), Toast.LENGTH_SHORT).show();
	}

	private void onUndoClick() {
		_states.undo();
		_widget.refreshDisplay(_states.getMode());
//		GpxEditorUtil.createToast(_mapActivity, R.drawable.widget_gpx_edit_undo, _mapActivity.getString(R.string.gpx_edit_undo), Toast.LENGTH_SHORT).show();
	}

	private void onTrackClick() {
		if (_states.getMode() != GpxEditorStates.Mode.EDIT_TRACK) {
			_widget.refreshDisplay(GpxEditorStates.Mode.EDIT_TRACK);
			GpxEditorUtil.createToast(_mapActivity, R.drawable.gpx_edit_track, _mapActivity.getString(R.string.gpx_edit_track), Toast.LENGTH_SHORT).show();
		}
	}

	private void onRouteClick() {
		if (_states.getMode() != GpxEditorStates.Mode.EDIT_ROUTE) {
			_widget.refreshDisplay(GpxEditorStates.Mode.EDIT_ROUTE);
			GpxEditorUtil.createToast(_mapActivity, R.drawable.gpx_edit_route, _mapActivity.getString(R.string.gpx_edit_route), Toast.LENGTH_SHORT).show();
		}
	}

	private void onPoiClick() {
		if (_states.getMode() != GpxEditorStates.Mode.EDIT_POI) {
			_widget.refreshDisplay(GpxEditorStates.Mode.EDIT_POI);
			GpxEditorUtil.createToast(_mapActivity, R.drawable.gpx_edit_favourite, _mapActivity.getString(R.string.gpx_edit_track_favourite), Toast.LENGTH_SHORT).show();
		}
	}

	private void onDeleteClick() {
		if (_states.getMode() != GpxEditorStates.Mode.DELETE) {
			_widget.refreshDisplay(GpxEditorStates.Mode.DELETE);
			GpxEditorUtil.createToast(_mapActivity, R.drawable.ic_action_delete_light, _mapActivity.getString(R.string.gpx_edit_delete_long), Toast.LENGTH_SHORT).show();
		}
	}

	private void onFavouriteClick() {
		_layer.selectFavourite();
	}
}