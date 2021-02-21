package net.osmand.plus.onlinerouting.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteDirectionInfo;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.util.List;

public abstract class ResponseParser {

	protected static final Log LOG = PlatformUtil.getLog(ResponseParser.class);

	@Nullable
	public abstract OnlineRoutingResponse parseResponse(@NonNull String content,
	                                                    @NonNull OsmandApplication app,
	                                                    boolean leftSideNavigation) throws JSONException;

	public abstract boolean isResultOk(@NonNull StringBuilder errorMessage,
	                                   @NonNull String content) throws JSONException;

	public static ResponseParser emptyParser() {
		return new ResponseParser() {
			@Nullable
			@Override
			public OnlineRoutingResponse parseResponse(@NonNull String content,
			                                           @NonNull OsmandApplication app,
			                                           boolean leftSideNavigation) {
				return null;
			}

			@Override
			public boolean isResultOk(@NonNull StringBuilder errorMessage,
			                          @NonNull String content) {
				return false;
			}
		};
	}

	public static class OnlineRoutingResponse {

		private List<Location> route;
		private List<RouteDirectionInfo> directions;
		private GPXFile gpxFile;

		// constructor for JSON responses
		public OnlineRoutingResponse(List<Location> route, List<RouteDirectionInfo> directions) {
			this.route = route;
			this.directions = directions;
		}

		// constructor for GPX responses
		public OnlineRoutingResponse(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
		}

		public List<Location> getRoute() {
			return route;
		}

		public List<RouteDirectionInfo> getDirections() {
			return directions;
		}

		public GPXFile getGpxFile() {
			return gpxFile;
		}
	}

}
