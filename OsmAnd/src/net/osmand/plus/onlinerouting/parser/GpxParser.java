package net.osmand.plus.onlinerouting.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class GpxParser extends ResponseParser {

	@Override
	@Nullable
	public OnlineRoutingResponse parseResponse(@NonNull String content,
	                                           @NonNull OsmandApplication app,
	                                           boolean leftSideNavigation) {
		GPXFile gpxFile = parseGpx(content);
		return gpxFile != null ? new OnlineRoutingResponse(parseGpx(content)) : null;
	}

	@Override
	public boolean isResultOk(@NonNull StringBuilder errorMessage,
	                          @NonNull String content) {
		return parseGpx(content) != null;
	}

	private GPXFile parseGpx(@NonNull String content) {
		InputStream gpxStream = null;
		try {
			gpxStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
			return GPXUtilities.loadGPXFile(gpxStream);
		} catch (UnsupportedEncodingException e) {
			LOG.debug("Error when parsing GPX from server response: " + e.getMessage());
		}
		return null;
	}

}
