package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.data.MapTileDownloader;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.ITileSource;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadTilesDialog {

	
	private final static Log log = LogUtil.getLog(DownloadTilesDialog.class); 
	private final Context ctx;
	private final OsmandApplication app;
	private final OsmandMapTileView mapView;

	public DownloadTilesDialog(Context ctx, OsmandApplication app, OsmandMapTileView mapView){
		this.ctx = ctx;
		this.app = app;
		this.mapView = mapView;
	}

	public class TileCoords {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + x;
			result = prime * result + y;
			result = prime * result + z;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TileCoords other = (TileCoords) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			if (z != other.z)
				return false;
			return true;
		}

		public TileCoords(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public TileCoords(TileCoords c) {
			this.x = c.x;
			this.y = c.y;
			this.z = c.z;
		}

		public void unzoom() {
			x /= 2;
			y /= 2;
			z--;
		}
		int x, y, z;
		private DownloadTilesDialog getOuterType() {
			return DownloadTilesDialog.this;
		}
	}
	
	public void openDialog(){
		BaseMapLayer mainLayer = mapView.getMainLayer();
		if(!(mainLayer instanceof MapTileLayer) || !((MapTileLayer) mainLayer).isVisible()){
			Toast.makeText(ctx, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
		}
		final ITileSource mapSource = ((MapTileLayer) mainLayer).getMap();
		if(mapSource == null || !mapSource.couldBeDownloadedFromInternet()){
			Toast.makeText(ctx, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
			return;
		}
		final int max = mapSource.getMaximumZoomSupported();
		final int zoom = mapView.getZoom();
		
		// calculate pixel rectangle
		Rect boundsRect = new Rect(0, 0, mapView.getWidth(), mapView.getHeight());
		float tileX = (float) MapUtils.getTileNumberX(zoom, mapView.getLongitude());
		float tileY = (float) MapUtils.getTileNumberY(zoom, mapView.getLatitude());
		float w = mapView.getCenterPointX();
		float h = mapView.getCenterPointY();
		RectF tilesRect = new RectF();
		final RectF latlonRect = new RectF();
		mapView.calculateTileRectangle(boundsRect, w, h, tileX, tileY, tilesRect);
		
		latlonRect.top = (float) MapUtils.getLatitudeFromTile(zoom, tilesRect.top);
		latlonRect.left = (float) MapUtils.getLongitudeFromTile(zoom, tilesRect.left);
		latlonRect.bottom = (float) MapUtils.getLatitudeFromTile(zoom, tilesRect.bottom);
		latlonRect.right = (float) MapUtils.getLongitudeFromTile(zoom, tilesRect.right);
		
		Builder builder = new AlertDialog.Builder(ctx);
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.download_tiles, null);
		
		
		
		((TextView)view.findViewById(R.id.MinZoom)).setText(zoom+""); //$NON-NLS-1$
		((TextView)view.findViewById(R.id.MaxZoom)).setText(max+""); //$NON-NLS-1$
		final SeekBar seekBar = (SeekBar) view.findViewById(R.id.ZoomToDownload);
		seekBar.setMax(max - zoom);
		seekBar.setProgress((max - zoom) / 2);
		
		final TextView downloadText = ((TextView) view.findViewById(R.id.DownloadDescription));
		final String template = ctx.getString(R.string.tiles_to_download_estimated_size);
		
		
		updateLabel(zoom, latlonRect, downloadText, template, seekBar.getProgress());
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateLabel(zoom, latlonRect, downloadText, template, progress); 
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
		});
		
		builder.setPositiveButton(R.string.download_files, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				run(zoom, seekBar.getProgress(), latlonRect, mapSource);
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setView(view);
		builder.show();
	}
	
	private volatile boolean cancel = false;
	
	public void run(final int zoom, final int progress, final RectF latlonRect, final ITileSource map){
		cancel = false;
		int numberTiles = 0;
		for (int z = zoom; z <= progress + zoom; z++) {
			int x1 = (int) MapUtils.getTileNumberX(z, latlonRect.left);
			int x2 = (int) MapUtils.getTileNumberX(z, latlonRect.right);
			int y1 = (int) MapUtils.getTileNumberY(z, latlonRect.top);
			int y2 = (int) MapUtils.getTileNumberY(z, latlonRect.bottom);
			numberTiles += (x2 - x1 + 1) * (y2 - y1 + 1);
		}
		final ProgressDialog progressDlg = new ProgressDialog(ctx);
		progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDlg.setMessage(ctx.getString(R.string.downloading));
		progressDlg.setCancelable(true);
		progressDlg.setMax(numberTiles);
		progressDlg.setOnCancelListener(new DialogInterface.OnCancelListener(){

			@Override
			public void onCancel(DialogInterface dialog) {
				cancel = true;
			}
		});
		
		final MapTileDownloader instance = MapTileDownloader.getInstance(Version.getFullVersion(ctx));
		
		final ArrayList<IMapDownloaderCallback> previousCallbacks = 
			new ArrayList<IMapDownloaderCallback>(instance.getDownloaderCallbacks());
		instance.getDownloaderCallbacks().clear();
		instance.addDownloaderCallback(new IMapDownloaderCallback(){
			@Override
			public void tileDownloaded(DownloadRequest request) {
				if (request != null) {
					progressDlg.setProgress(progressDlg.getProgress() + 1);
				}
			}
		});
		
		Runnable r = new Runnable(){
			@Override
			public void run() {
				int requests = 0;
				int limitRequests = 50;
				try {
					ResourceManager rm = app.getResourceManager();
					Set<TileCoords> excludedTiles = new HashSet<TileCoords>();
					for (int z = zoom; z <= zoom + progress && !cancel; z++) {
						int x1 = (int) MapUtils.getTileNumberX(z, latlonRect.left);
						int x2 = (int) MapUtils.getTileNumberX(z, latlonRect.right);
						int y1 = (int) MapUtils.getTileNumberY(z, latlonRect.top);
						int y2 = (int) MapUtils.getTileNumberY(z, latlonRect.bottom);
						for (int x = x1; x <= x2 && !cancel; x++) {
							for (int y = y1; y <= y2 && !cancel; y++) {

								// skip tiles containing nothing of interest
								TileCoords c = new TileCoords(x, y, z);
								if (checkIfExcluded(excludedTiles, c))
									continue;

								String tileId = rm.calculateTileId(map, x, y, z);
								if (rm.tileExistOnFileSystem(tileId, map, x, y, z)) {
									progressDlg.setProgress(progressDlg.getProgress() + 1);
								} else {
									rm.getTileImageForMapSync(tileId, map, x, y, z, true);
									requests++;
								}
								if (!cancel) {
									if (requests >= limitRequests) {
										requests = 0;

										while (instance.isSomethingBeingDownloaded()) {
											try {
												Thread.sleep(500);
											} catch (InterruptedException e) {
												throw new IllegalArgumentException(e);
											}
										}
									}
								}
							}
						}
						while (instance.isSomethingBeingDownloaded()) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								throw new IllegalArgumentException(e);
							}
						}
						// scan tiles, remember tiles containing only identical pixels
						for (int x = x1; x <= x2 && !cancel; x++) {
							for (int y = y1; y <= y2 && !cancel; y++) {
								String tileId = rm.calculateTileId(map, x, y, z);
								Bitmap tile = rm.getUnditheredImageTile(tileId);
								if (tile != null) {
									int flags = scanTile(tile);
									for (int qx = 0; qx < 2; qx++) {
										for (int qy = 0; qy < 2; qy++) {
											if ((flags & (1 << (qx * 2 + qy))) != 0) {
												TileCoords c = new TileCoords(x * 2 + qx, y * 2 + qy, z + 1);
												excludedTiles.add(c);
											}
										}
									}
								}
							}
						}
						
					}
					if(cancel){
						instance.refuseAllPreviousRequests();
					} else {
						while (instance.isSomethingBeingDownloaded()) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								throw new IllegalArgumentException(e);
							}
						}
					}
					mapView.refreshMap();
				} catch (Exception e) {
					log.error("Exception while downloading tiles ", e); //$NON-NLS-1$
					instance.refuseAllPreviousRequests();
				} finally {
					instance.getDownloaderCallbacks().clear();
					instance.getDownloaderCallbacks().addAll(previousCallbacks);
					app.getResourceManager().reloadTilesFromFS();
				}
				progressDlg.dismiss();
			}
			
		};
		
		
		
		new Thread(r, "Downloading tiles").start(); //$NON-NLS-1$
		progressDlg.show();
	}

	private boolean checkIfExcluded(Set<TileCoords> excludedTiles, TileCoords c) {
		TileCoords c2 = new TileCoords(c);
		while (c2.z > 0) {
			if (excludedTiles.contains(c2))
				return true;
			c2.unzoom();
		}
		return false;
	}

	private int scanTile(Bitmap tile) {
		int flags = 0x0f;

		// skip checking for uneven dimensions
		if (tile.getWidth() % 2 != 0 || tile.getHeight() % 2 != 0)
			return 0x00;

		int subW = tile.getWidth() / 2;
		int subH = tile.getHeight() / 2;
		int pixels[] = new int[subW * subH];
		for (int qx = 0; qx < 2; qx++) {
			for (int qy = 0; qy < 2; qy++) {
				boolean diff = false;
				tile.getPixels(pixels, 0, subW, qx * subW, qy * subH, subW, subH);
				for (int y = 0; !diff && y < subH; y++) {
					for (int x = 0; !diff && x < subW; x++) {
						if (pixels[y * subW + x] != pixels[0])
							diff = true;
					}
				}
				if (diff)
					flags &= ~(1 << (qx * 2 + qy));
			}
		}

		return flags;
	}

	private void updateLabel(final int zoom, final RectF latlonRect, final TextView downloadText, final String template, int progress) {
		int numberTiles = 0;
		for (int z = zoom; z <= progress + zoom; z++) {
			int x1 = (int) MapUtils.getTileNumberX(z, latlonRect.left);
			int x2 = (int) MapUtils.getTileNumberX(z, latlonRect.right);
			int y1 = (int) MapUtils.getTileNumberY(z, latlonRect.top);
			int y2 = (int) MapUtils.getTileNumberY(z, latlonRect.bottom);
			numberTiles += (x2 - x1 + 1) * (y2 - y1 + 1);
		}
		downloadText.setText(MessageFormat.format(template, (progress + zoom)+"", //$NON-NLS-1$ 
				numberTiles, (double)numberTiles*12/1000));
	}
}
