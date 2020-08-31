package net.osmand.plus.server;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.webkit.MimeTypeMap;
import androidx.core.util.Pair;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.data.FavouritePoint;
import net.osmand.data.GeometryTile;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.server.map.LayersDraw;
import net.osmand.plus.server.map.MapTileMiniLayer;
import net.osmand.plus.server.map.OsmandMapMiniLayer;
import net.osmand.plus.server.map.OsmandMapTileMiniView;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ApiRouter {
	private OsmandApplication androidContext;
	public OsmandApplication getAndroidContext() {
		return androidContext;
	}

	private final String FOLDER_NAME = "server";
	private Gson gson = new Gson();
	private Map<String, ApiEndpoint> endpoints = new HashMap<>();
	public static MapActivity mapActivity;

	public ApiRouter(){
		initRoutes();
	}

	private void initRoutes() {
		ApiEndpoint favorites = new ApiEndpoint();
		favorites.uri = "/favorites";
		favorites.apiCall = new ApiEndpoint.ApiCall(){
			@Override
			public NanoHTTPD.Response call(NanoHTTPD.IHTTPSession session) {
				return newFixedLengthResponse(getFavoritesJson());
			}
		};
		endpoints.put(favorites.uri,favorites);

		final ApiEndpoint tile = new ApiEndpoint();
		tile.uri = "/tile";
		tile.apiCall = new ApiEndpoint.ApiCall(){
			@Override
			public NanoHTTPD.Response call(NanoHTTPD.IHTTPSession session) {
				try{
					ITileSource map = TileSourceManager.getMapillaryVectorSource();
					Bitmap bitmap = Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);//mapActivity.getMapView().currentCanvas;
					//OsmandMapTileView tileView = new OsmandMapTileView(mapActivity,300,300);
					OsmandMapTileMiniView tileView = new OsmandMapTileMiniView(androidContext,512,512);
					Canvas canvas = new Canvas(bitmap);
					LayersDraw.createLayers(androidContext,canvas, tileView);
					Paint p = new Paint();
					p.setStyle(Paint.Style.FILL_AND_STROKE);
					p.setColor(Color.BLACK);
					//canvas.drawBitmap(bitmap ,0, 0, null);
					boolean nightMode = androidContext.getDaynightHelper().isNightMode();
					OsmandMapMiniLayer.DrawSettings drawSettings = new OsmandMapMiniLayer.DrawSettings(nightMode, false);
					tileView.refreshMapInternal(drawSettings);
					tileView.refreshMap();
					MapTileMiniLayer mapTileLayer = new MapTileMiniLayer(true);
					//mapView.addLayer(mapTileLayer, 0.0f);
					//mapTileLayer.drawTileMap(canvas,tileView.getCurrentRotatedTileBox());
					tileView.drawOverMap(canvas,tileView.getCurrentRotatedTileBox(),drawSettings);
					//androidContext.getApplicationContext().get
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					//bitmap = tileView.currentCanvas;
					final QuadRect tilesRect = tileView.getCurrentRotatedTileBox().getTileBounds();
					int left = (int) Math.floor(tilesRect.left);
					int top = (int) Math.floor(tilesRect.top);
					int width = (int) Math.ceil(tilesRect.right - left);
					int height = (int) Math.ceil(tilesRect.bottom - top);
					int dzoom = 1;
					int div = (int) Math.pow(2.0, dzoom);

					ResourceManager mgr = androidContext.getResourceManager();
					int tileX = (left ) / div;
					int tileY = (top) / div;
					String tileId = mgr.calculateTileId(map, tileX, tileY, 14);
					//androidContext.getResourceManager().
					//		getMapTileDownloader().
					//String imgTileId = mgr.calculateTileId(map, tileX / div, tileY / div, nzoom - kzoom);
					Map<String, Pair<QuadPointDouble, GeometryTile>> tiles = new HashMap<>();
					//bitmap = null;
					boolean first = true;
					//while (bitmap == null){
//						bitmap = androidContext.getResourceManager().getBitmapTilesCache().getTileForMapAsync(
//								tileId,map,tileX,tileY,14,first
//						);
//						first = false;
					//}
					canvas.drawLine(0,0,canvas.getWidth(),canvas.getHeight(), p);
					//bitmap = tileView.currentCanvas;
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

					byte[] byteArray = stream.toByteArray();
					ByteArrayInputStream str = new ByteArrayInputStream(byteArray);

					return newFixedLengthResponse(
							NanoHTTPD.Response.Status.OK,
							"image/png",
							str,
							str.available());
				}
				catch (Exception e){
					e.printStackTrace();
				}
				return ErrorResponses.response500;
			}
		};
		endpoints.put(tile.uri,tile);
	}

	public void setAndroidContext(OsmandApplication androidContext) {
		this.androidContext = androidContext;
	}

	public NanoHTTPD.Response route(NanoHTTPD.IHTTPSession session) {
		Log.d("SERVER", "URI: " + session.getUri());
		String uri = session.getUri();
		if (uri.equals("/")) return getStatic("/go.html");
		if (uri.contains("/scripts/") ||
				uri.contains("/images/") ||
				uri.contains("/css/") ||
				uri.contains("/fonts/") ||
				uri.contains("/favicon.ico")
		) return getStatic(uri);
		if (isApiUrl(uri)){
			return routeApi(session);
		}
		else {
			return routeContent(session);
		}
	}

	private NanoHTTPD.Response routeApi(NanoHTTPD.IHTTPSession session) {
		String uri = session.getUri();
		ApiEndpoint endpoint = endpoints.get(uri);
		if (endpoint != null){
			return endpoint.apiCall.call(session);
		}
		return ErrorResponses.response404;
	}

	private boolean isApiUrl(String uri) {
		for (String endpoint : endpoints.keySet()){
			if (endpoint.equals(uri)) return true;
		}
		return false;
	}

	private NanoHTTPD.Response routeContent(NanoHTTPD.IHTTPSession session) {
		String url = session.getUri();
		//add index page
		String responseText = getHtmlPage(url);
		if (responseText != null) {
			return newFixedLengthResponse(responseText);
		} else {
			return ErrorResponses.response404;
		}
	}

	public NanoHTTPD.Response getStatic(String uri) {
		InputStream is = null;
		String mimeType = parseMimeType(uri);
		if (androidContext != null) {
			try {
				is = androidContext.getAssets().open(FOLDER_NAME + uri);
				if (is.available() == 0) {
					return ErrorResponses.response404;
				}
				return newFixedLengthResponse(
						NanoHTTPD.Response.Status.OK,
						mimeType,
						is,
						is.available());
			} catch (IOException e) {
				return ErrorResponses.response404;
			}
		}
		return ErrorResponses.response500;
	}

	private String parseMimeType(String url) {
		String type = null;
		if (url.endsWith(".js")) return "text/javascript";
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	private String readHTMLFromFile(String filename) {
		StringBuilder sb = new StringBuilder();
		try {
			InputStream is = androidContext.getAssets().open(FOLDER_NAME + filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String str;
			while ((str = br.readLine()) != null) {
				sb.append(str);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return sb.toString();
	}

	public String getHtmlPage(String name) {
		String responseText = "";
		if (androidContext != null) {
			responseText = readHTMLFromFile(name);
		}
		if (responseText == null) {
			return null;
		}
		return responseText;
	}

	private String getFavoritesJson() {
		List<FavouritePoint> points = androidContext.getFavorites().getFavouritePoints();
		StringBuilder text = new StringBuilder();
		for (FavouritePoint p : points) {
			String json = jsonFromFavorite(p);
			text.append(json);
			text.append(",");
		}
		return "[" + text.substring(0,text.length()-1) + "]";
	}

	private String jsonFromFavorite(FavouritePoint favouritePoint) {
		return gson.toJson(favouritePoint);
	}

	static class ErrorResponses {
		static NanoHTTPD.Response response404 =
				newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND,
						NanoHTTPD.MIME_PLAINTEXT, "404 Not Found");

		static NanoHTTPD.Response response500 =
				newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
						NanoHTTPD.MIME_PLAINTEXT, "500 Internal Server Error");
	}
}