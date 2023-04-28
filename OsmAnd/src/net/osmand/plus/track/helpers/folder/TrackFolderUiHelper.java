package net.osmand.plus.track.helpers.folder;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.OsmAndFormatter;

public class TrackFolderUiHelper {

	public static String getFolderDescription(@NonNull Context ctx, @NonNull TrackFolder trackFolder) {
		String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_comma);
		String lastUpdateTime = formatLastUpdateTime(ctx, trackFolder);
		String tracksCount = formatTracksCount(ctx, trackFolder);
		return String.format(pattern, lastUpdateTime, tracksCount);
	}

	private static String formatLastUpdateTime(@NonNull Context ctx, @NonNull TrackFolder trackFolder) {
		return OsmAndFormatter.getFormattedDate(ctx, trackFolder.getLastModified());
	}

	private static String formatTracksCount(@NonNull Context ctx, @NonNull TrackFolder trackFolder) {
		String pattern = ctx.getString(R.string.n_tracks);
		return String.format(pattern, trackFolder.getTotalTracksCount());
	}

}
