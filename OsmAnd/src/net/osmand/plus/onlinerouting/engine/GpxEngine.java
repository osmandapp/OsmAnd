package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.onlinerouting.engine.EngineType.GPX_TYPE;

public class GpxEngine extends OnlineRoutingEngine {

	public GpxEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return GPX_TYPE;
	}

	@Override
	@NonNull
	public String getTitle() {
		return "GPX";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "GPX";
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb,
	                           @NonNull List<LatLon> path) {
		sb.append("?");
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append("point=")
					.append(point.getLatitude())
					.append(',')
					.append(point.getLongitude());
			if (i < path.size() - 1) {
				sb.append('&');
			}
		}
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "";
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {

	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		params.add(EngineParameter.CUSTOM_URL);
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new GpxEngine(params);
	}

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
		InputStream gpxStream;
		try {
			gpxStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
			return GPXUtilities.loadGPXFile(gpxStream);
		} catch (UnsupportedEncodingException e) {
			LOG.debug("Error when parsing GPX from server response: " + e.getMessage());
		}
		return null;
	}
}
