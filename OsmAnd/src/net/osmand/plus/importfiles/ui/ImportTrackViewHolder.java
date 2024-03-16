package net.osmand.plus.importfiles.ui;

import static net.osmand.plus.utils.ColorUtilities.getActiveColorId;
import static net.osmand.plus.utils.ColorUtilities.getDefaultIconColorId;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ui.ImportTracksAdapter.ImportTracksListener;
import net.osmand.plus.myplaces.tracks.MapBitmapDrawerListener;
import net.osmand.plus.myplaces.tracks.MapDrawParams;
import net.osmand.plus.myplaces.tracks.TrackBitmapDrawer;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GpxBlockStatisticsBuilder;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;

import java.util.List;

class ImportTrackViewHolder extends ViewHolder {

	final OsmandApplication app;
	final UiUtilities uiUtilities;
	final ImportTracksListener listener;
	final MapDrawParams drawParams;

	final TextView title;
	final TextView selectedTitle;
	final TextView trackIndex;
	final TextView pointsCounter;
	final ImageView image;
	final ImageView pointsIcon;
	final RecyclerView recyclerView;
	final View pointsRow;
	final View selectionRow;
	final CompoundButton selectedCheckBox;

	final boolean nightMode;

	ImportTrackViewHolder(@NonNull View view, @NonNull MapDrawParams drawParams,
	                      @Nullable ImportTracksListener listener, boolean nightMode) {
		super(view);
		this.app = (OsmandApplication) view.getContext().getApplicationContext();
		this.uiUtilities = app.getUIUtilities();
		this.drawParams = drawParams;
		this.listener = listener;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.title);
		image = view.findViewById(R.id.image);
		trackIndex = view.findViewById(R.id.track_index);
		recyclerView = view.findViewById(R.id.recycler_overview);

		pointsRow = view.findViewById(R.id.waypoints_row);
		pointsIcon = pointsRow.findViewById(R.id.icon);
		pointsCounter = pointsRow.findViewById(R.id.counter);

		selectionRow = view.findViewById(R.id.selected_row);
		selectedTitle = selectionRow.findViewById(R.id.title);
		selectedCheckBox = selectionRow.findViewById(R.id.compound_button);
	}

	public void bindView(@NonNull ImportTrackItem item, @NonNull List<WptPt> points, boolean selected,
	                     @Nullable MapBitmapDrawerListener drawerListener) {
		setupHeaderRow(item);
		setupPointsRow(item, points);
		setupStatisticsRow(item);
		setupSelectionRow(item, selected);
		drawTrackImage(item, drawerListener);
	}

	private void setupStatisticsRow(@NonNull ImportTrackItem item) {
		GPXTrackAnalysis analysis = item.selectedGpxFile.getTrackAnalysis(app);
		GpxBlockStatisticsBuilder builder = new GpxBlockStatisticsBuilder(app, item.selectedGpxFile, nightMode);
		builder.setBlocksView(recyclerView, false);
		builder.initStatBlocks(null, ColorUtilities.getActiveColor(app, nightMode), analysis);
	}

	private void setupHeaderRow(@NonNull ImportTrackItem item) {
		title.setText(item.name);
		trackIndex.setText(String.valueOf(item.index));
	}

	private void setupSelectionRow(@NonNull ImportTrackItem item, boolean selected) {
		selectedCheckBox.setChecked(selected);

		selectedTitle.setText(selected ? R.string.shared_string_selected : R.string.shared_string_select);
		selectionRow.setOnClickListener(v -> {
			boolean checked = !selectedCheckBox.isChecked();
			selectedCheckBox.setChecked(checked);
			selectedTitle.setText(checked ? R.string.shared_string_selected : R.string.shared_string_select);

			if (listener != null) {
				listener.onTrackItemSelected(item, checked);
			}
		});
		UiUtilities.setupCompoundButton(selectedCheckBox, nightMode, CompoundButtonType.GLOBAL);
	}

	private void setupPointsRow(@NonNull ImportTrackItem item, @NonNull List<WptPt> points) {
		pointsRow.setOnClickListener(v -> {
			if (listener != null) {
				listener.onTrackItemPointsSelected(item);
			}
		});
		int color = item.selectedPoints.isEmpty() ? getDefaultIconColorId(nightMode) : getActiveColorId(nightMode);
		pointsIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_folder, color));

		String allPoints = String.valueOf(points.size());
		String selectedPoints = String.valueOf(item.selectedPoints.size());
		pointsCounter.setText(app.getString(R.string.ltr_or_rtl_combine_via_slash, selectedPoints, allPoints));
	}

	private void drawTrackImage(@NonNull ImportTrackItem item, @Nullable MapBitmapDrawerListener listener) {
		if (item.bitmapDrawer == null) {
			GPXFile gpxFile = item.selectedGpxFile.getGpxFile();
			item.bitmapDrawer = new TrackBitmapDrawer(app, drawParams, gpxFile, null);
			item.bitmapDrawer.addListener(listener);
			item.bitmapDrawer.setDefaultTrackColor(GpxAppearanceAdapter.getTrackColor(app));
			item.bitmapDrawer.initAndDraw();
		} else {
			image.setClipToOutline(true);
			image.setImageBitmap(item.bitmap);
		}
		AndroidUiHelper.updateVisibility(image, item.bitmap != null);
	}
}