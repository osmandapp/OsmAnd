package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;

import net.osmand.LogUtil;
import net.osmand.data.MapTileDownloader;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.ITileSource;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
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
	
	
	public void openDialog(){
		final ITileSource mapSource = mapView.getMap();
		if(mapSource == null || mapView.isVectorDataVisible() || !mapSource.couldBeDownloadedFromInternet()){
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
		
		final MapTileDownloader instance = MapTileDownloader.getInstance();
		
		final ArrayList<IMapDownloaderCallback> previousCallbacks = 
			new ArrayList<IMapDownloaderCallback>(instance.getDownloaderCallbacks());
		instance.getDownloaderCallbacks().clear();
		instance.addDownloaderCallback(new IMapDownloaderCallback(){
			@Override
			public void tileDownloaded(DownloadRequest request) {
				progressDlg.setProgress(progressDlg.getProgress() + 1);
			}
		});
		
		Runnable r = new Runnable(){
			@Override
			public void run() {
				int requests = 0;
				int limitRequests = 50;
				try {
					ResourceManager rm = app.getResourceManager();
					for (int z = zoom; z <= zoom + progress && !cancel; z++) {
						int x1 = (int) MapUtils.getTileNumberX(z, latlonRect.left);
						int x2 = (int) MapUtils.getTileNumberX(z, latlonRect.right);
						int y1 = (int) MapUtils.getTileNumberY(z, latlonRect.top);
						int y2 = (int) MapUtils.getTileNumberY(z, latlonRect.bottom);
						for (int x = x1; x <= x2 && !cancel; x++) {
							for (int y = y1; y <= y2 && !cancel; y++) {
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
