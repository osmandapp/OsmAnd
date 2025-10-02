package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_AIRPLANE;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_ATON;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_ATON_VIRTUAL;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_LANDSTATION;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_SART;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ChartPointsHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapSelectionRules;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class AisTrackerLayer extends OsmandMapLayer implements IContextMenuProvider {

	public static final int START_ZOOM = 6;
	private static final int AIS_OBJECT_LIST_COUNTER_MAX = 200;

	private final AisTrackerPlugin plugin = PluginsHelper.requirePlugin(AisTrackerPlugin.class);
	private final Paint bitmapPaint = new Paint();
	private final Map<Integer, AisObject> objects = new ConcurrentHashMap<>();
	private final Map<Integer, Bitmap> pointImages = new ConcurrentHashMap<>();

	private Timer timer;
	private AisMessageListener listener;
	private MapMarkersCollection markersCollection;
	private VectorLinesCollection vectorLinesCollection;
	private SingleSkImage aisRestImage;
	private float textScale = 1f;

	public AisTrackerLayer(@NonNull Context context) {
		super(context);

		this.bitmapPaint.setAntiAlias(true);
		this.bitmapPaint.setFilterBitmap(true);
		this.bitmapPaint.setStrokeWidth(4);
		this.bitmapPaint.setColor(Color.DKGRAY);

		AisObject.setCpaWarningTime(plugin.AIS_CPA_WARNING_TIME.get());
		AisObject.setCpaWarningDistance(plugin.AIS_CPA_WARNING_DISTANCE.get());

		initTimer();
		startNetworkListener();
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);


	}

	@Override
	public void onMapRendererChange(@Nullable MapRendererView currentMapRenderer,
			@Nullable MapRendererView newMapRenderer) {
		super.onMapRendererChange(currentMapRenderer, newMapRenderer);

		if (newMapRenderer != null) {
			markersCollection = new MapMarkersCollection();
			vectorLinesCollection = new VectorLinesCollection();
			newMapRenderer.addSymbolsProvider(markersCollection);
			newMapRenderer.addSymbolsProvider(vectorLinesCollection);

			ChartPointsHelper pointsHelper = new ChartPointsHelper(getContext());
			float density = 5;
			int pointColor = 0xFFFFFFFF;
			aisRestImage = NativeUtilities.createSkImageFromBitmap(
					pointsHelper.createXAxisPointBitmap(pointColor, density));

			for (AisObject ais : objects.values()) {
				ais.createAisRenderData(getBaseOrder(), this, bitmapPaint,
						markersCollection, vectorLinesCollection, aisRestImage);
				ais.updateAisRenderData(getTileView(), this, bitmapPaint);
			}
		}
	}

	public void setListener(@Nullable AisMessageListener listener) {
		this.listener = listener;
	}

	private void initTimer() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				removeLostAisObjects();
			}
		};
		this.timer = new Timer();
		timer.schedule(timerTask, 20000, 30000);
	}

	private void startNetworkListener() {
		int proto = plugin.AIS_NMEA_PROTOCOL.get();
		if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP) {
			this.listener = new AisMessageListener(this, plugin.AIS_NMEA_UDP_PORT.get());
		} else if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP) {
			this.listener = new AisMessageListener(this, plugin.AIS_NMEA_IP_ADDRESS.get(), plugin.AIS_NMEA_TCP_PORT.get());
		}
	}

	private void stopNetworkListener() {
		if (this.listener != null) {
			this.listener.stopListener();
			this.listener = null;
		}
	}

	/* this method restarts the TCP listeners after a "resume" event (the smartphone resumed
	 *  from sleep or from switched off state): in this case the TCP connection might be broken,
	 *  but the sockets are still (logically) open.
	 *  as additional indication of a broken TCP connection it is checked whether any AIS message
	 *  was received in the last 20 seconds  */
	public void checkTcpConnection() {
		if (listener != null) {
			if (listener.checkTcpSocket()) {
				if (((System.currentTimeMillis() - AisObject.getAndUpdateLastMessageReceived()) / 1000) > 20) {
					Log.d("AisTrackerLayer", "checkTcpConnection(): restart TCP socket");
					restartNetworkListener();
				}
			}
		}
	}

	public void restartNetworkListener() {
		stopNetworkListener();
		startNetworkListener();
	}

	@Override
	public void cleanupResources() {
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
			this.timer = null;
		}
		this.objects.clear();

		MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && markersCollection != null && vectorLinesCollection != null) {
			markersCollection.removeAllMarkers();
			vectorLinesCollection.removeAllLines();
			mapRenderer.removeSymbolsProvider(markersCollection);
			mapRenderer.removeSymbolsProvider(vectorLinesCollection);
		}

		stopNetworkListener();
	}

	private void removeLostAisObjects() {
		for (Iterator<Map.Entry<Integer, AisObject>> iterator = objects.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<Integer, AisObject> entry = iterator.next();
			if (entry.getValue().checkObjectAge()) {
				Log.d("AisTrackerLayer", "remove AIS object with MMSI " + entry.getValue().getMmsi());
				if (view.getMapRenderer() != null && markersCollection != null && vectorLinesCollection != null) {
					entry.getValue().clearAisRenderData(markersCollection, vectorLinesCollection);
				}
				iterator.remove();
			}
		}
		// aisObjectList.entrySet().removeIf(entry -> entry.getValue().checkObjectAge());
	}

	private void removeOldestAisObjectListEntry() {
		Log.d("AisTrackerLayer", "removeOldestAisObjectListEntry() called");
		long oldestTimeStamp = System.currentTimeMillis();
		AisObject oldest = null;
		for (AisObject ais : objects.values()) {
			long timeStamp = ais.getLastUpdate();
			if (timeStamp <= oldestTimeStamp) {
				oldestTimeStamp = timeStamp;
				oldest = ais;
			}
		}
		if (oldest != null) {
			Log.d("AisTrackerLayer", "remove AIS object with MMSI " + oldest.getMmsi());
			if (view.getMapRenderer() != null && markersCollection != null && vectorLinesCollection != null) {
				oldest.clearAisRenderData(markersCollection, vectorLinesCollection);
			}
			objects.remove(oldest.getMmsi(), oldest);
		}
	}

	/* add new AIS object to list, or (if already exist) update its value */
	public void updateAisObjectList(@NonNull AisObject ais) {
		int mmsi = ais.getMmsi();
		AisObject obj = objects.get(mmsi);
		if (obj == null) {
			Log.d("AisTrackerLayer", "add AIS object with MMSI " + ais.getMmsi());
			AisObject newObj = new AisObject(ais);

			if (view.getMapRenderer() != null && markersCollection != null && vectorLinesCollection != null) {
				newObj.createAisRenderData(getBaseOrder(), this, bitmapPaint,
						markersCollection, vectorLinesCollection, aisRestImage);
				newObj.updateAisRenderData(getTileView(), this, bitmapPaint);
			}

			objects.put(mmsi, newObj);

			if (objects.size() >= AIS_OBJECT_LIST_COUNTER_MAX) {
				this.removeOldestAisObjectListEntry();
			}
		} else {
			obj.set(ais);

			if (view.getMapRenderer() != null && markersCollection != null && vectorLinesCollection != null) {
				obj.updateAisRenderData(getTileView(), this, bitmapPaint);
			}
		}
	}

	@Nullable
	public Bitmap getBitmap(@DrawableRes int drawableId) {
		Bitmap bitmap = pointImages.get(drawableId);
		if (bitmap == null) {
			Drawable icon = getApplication().getUIUtilities().getIcon(drawableId);
			bitmap = AndroidUtils.drawableToBitmap(icon, textScale, true);
			pointImages.put(drawableId, bitmap);
		}
		return bitmap;
	}

	public boolean isLocationVisible(RotatedTileBox tileBox, LatLon coordinates) {
		//noinspection SimplifiableIfStatement
		if (tileBox == null || coordinates == null) {
			return false;
		}
		return tileBox.containsLatLon(coordinates);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);

		float textScale = getTextScale();
		boolean textScaleChanged = this.textScale != textScale;
		this.textScale = textScale;
		if (textScaleChanged) {
			pointImages.clear();
		}

		if (tileBox.getZoom() >= START_ZOOM && !Algorithms.isEmpty(objects)) {
			AisObject.setOwnPosition(getApplication().getLocationProvider().getLastKnownLocation());
		}

		if (view.getMapRenderer() != null) {
			for (AisObject ais : objects.values()) {
				// Calling updateAisRenderData in onPrepareBufferImage is overhead
				// but it is needed to update directional line points
				// also there is no zoom animation for directional line depending on zoom
				// TODO: SUPPORT THIS IN ENGINE
				ais.updateAisRenderData(getTileView(), this, bitmapPaint);
			}
		} else if (tileBox.getZoom() >= START_ZOOM) {
			for (AisObject ais : objects.values()) {
				if (isLocationVisible(tileBox, ais.getPosition())) {
					ais.draw(this, bitmapPaint, canvas, tileBox);
				}
			}
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		if (Algorithms.isEmpty(objects) || tileBox.getZoom() < START_ZOOM) {
			return;
		}
		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(getApplication(), tileBox.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}
		for (AisObject object : objects.values()) {
			LatLon latLon = object.getPosition();
			if (latLon != null) {
				double lat = latLon.getLatitude();
				double lon = latLon.getLongitude();

				boolean add = mapRenderer != null
						? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
						: tileBox.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
				if (add) {
					result.collect(object, this);
				}
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof AisObject) {
			LatLon pos = ((AisObject) o).getPosition();
			if (pos != null) {
				return new LatLon(pos.getLatitude(), pos.getLongitude());
			}
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof AisObject ais) {
			AisObjType objectClass = ais.getObjectClass();
			if (ais.getShipName() != null) {
				return new PointDescription("AIS object", ais.getShipName() +
						(ais.getSignalLostState() ? " (signal lost)" : ""));
			} else if (objectClass == AIS_LANDSTATION) {
				return new PointDescription("AIS object", "Land Station with MMSI " + ais.getMmsi());
			} else if (objectClass == AIS_AIRPLANE) {
				return new PointDescription("AIS object", "Airplane with MMSI " +
						ais.getMmsi() + (ais.getSignalLostState() ? " (signal lost)" : ""));
			} else if ((objectClass == AIS_ATON) || (objectClass == AIS_ATON_VIRTUAL)) {
				return new PointDescription("AIS object", "Aid to Navigation");
			} else if (objectClass == AIS_SART) {
				return new PointDescription("AIS object", "SART (Search and Rescue Transmitter)");
			}
			return new PointDescription("AIS object",
					"AIS object with MMSI " + ais.getMmsi() +
							(ais.getSignalLostState() ? " (signal lost)" : ""));
		}
		return null;
	}
}
