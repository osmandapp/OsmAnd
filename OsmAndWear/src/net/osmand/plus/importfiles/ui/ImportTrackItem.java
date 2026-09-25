package net.osmand.plus.importfiles.ui;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.myplaces.tracks.TrackBitmapDrawer;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import java.util.ArrayList;
import java.util.List;

public class ImportTrackItem {

	public final int index;
	public final String name;
	public final SelectedGpxFile selectedGpxFile;
	public final List<WptPt> selectedPoints = new ArrayList<>();
	public final List<WptPt> suggestedPoints = new ArrayList<>();

	public Bitmap bitmap;
	public TrackBitmapDrawer bitmapDrawer;

	public ImportTrackItem(@NonNull SelectedGpxFile selectedGpxFile, @NonNull String name, int index) {
		this.name = name;
		this.index = index;
		this.selectedGpxFile = selectedGpxFile;
	}

	@NonNull
	@Override
	public String toString() {
		return name + " " + index;
	}
}
