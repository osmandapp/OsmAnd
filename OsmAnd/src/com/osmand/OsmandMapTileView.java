package com.osmand;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.osmand.osm.MapUtils;

public class OsmandMapTileView extends View {

	protected final int emptyTileDivisor = 4;
	protected final int maxImgCacheSize = 512;
	
	
	/**
	 * tile size of map 
	 */
	private int tileSize = 256;
	
	/**
	 * file or directory with tiles
	 */
	private File fileWithTiles;

	/**
	 * zoom level
	 */
	private int zoom = 15;
	
	// degree measurements (-180, 180)
	// долгота
	private double longitude = 27.56;
	// широта
	// degree measurements (90, -90)
	private double latitude = 53.9;
	
	/**
	 * listeners
	 */
	private List<IMapLocationListener> listeners = new ArrayList<IMapLocationListener>();
	
	
	// cached data to draw images
	private Bitmap[][] images;
	// this value is always <= 0
	private int xStartingImage = 0;
	// this value is always <= 0
	private int yStartingImage = 0;
	
	Map<String, Bitmap> cacheOfImages = new WeakHashMap<String, Bitmap>();
	
	Paint paintGrayFill;
	Paint paintWhiteFill;
	Paint paintBlack;
	
	
	public OsmandMapTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}
	
	public OsmandMapTileView(Context context) {
		super(context);
		initView();
	}
	
	public void initView(){
		paintGrayFill = new Paint();
		paintGrayFill.setColor(Color.GRAY);
		paintGrayFill.setStyle(Style.FILL);
		
		paintWhiteFill = new Paint();
		paintWhiteFill.setColor(Color.WHITE);
		paintWhiteFill.setStyle(Style.FILL);
		
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		
		prepareImage();
		setClickable(true);
	}
	
	
	private PointF startDragging = null;

	public void dragTo(PointF p){
		double dx = (startDragging.x - (double)p.x)/tileSize; 
		double dy = (startDragging.y - (double)p.y)/tileSize;
		this.latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
		this.longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
		prepareImage();
		fireMapLocationListeners(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			if(startDragging == null){
				startDragging  = new PointF(event.getX(), event.getY());
			}
		} else if(event.getAction() == MotionEvent.ACTION_UP) {
			if(startDragging != null){
				dragTo(new PointF(event.getX(), event.getY()));
				startDragging = null;
			}
		} else if(event.getAction() == MotionEvent.ACTION_MOVE) {
			if(startDragging != null){
				PointF p = new PointF(event.getX(), event.getY());
				dragTo(p);
				startDragging = p;
			}
		}
		return super.onTouchEvent(event);
	}
	
	public double getXTile(){
		return MapUtils.getTileNumberX(zoom, longitude);
	}
	
	public double getYTile(){
		return MapUtils.getTileNumberY(zoom, latitude);
	}
	
	protected void drawEmptyTile(Canvas cvs, int x, int y){
		int tileDiv = tileSize / emptyTileDivisor;
		for (int k1 = 0; k1 < emptyTileDivisor; k1++) {
			
			for (int k2 = 0; k2 < emptyTileDivisor; k2++) {
				int xk = x + tileDiv* k1;
				int yk = y + tileDiv* k2;
				if ((k1 + k2) % 2 == 0) {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintGrayFill);
				} else {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintWhiteFill);
				}
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRect(0,0, getWidth(), getHeight(), paintWhiteFill);
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				for (int j = 0; j < images[i].length; j++) {
					if (images[i][j] == null) {
						drawEmptyTile(canvas, i*tileSize+xStartingImage, j * tileSize + yStartingImage);
					} else {
						canvas.drawBitmap(images[i][j], i * tileSize + xStartingImage, j * tileSize + yStartingImage, null);
					}
				}
			}
		}
		canvas.drawCircle(getWidth()/2, getHeight()/2, 3, paintBlack);
		canvas.drawCircle(getWidth()/2, getHeight()/2, 6, paintBlack);
	}
	
 
	public Bitmap getImageFor(int x, int y) {
		String file = "/" + zoom + "/" + (x) + "/" + y + ".png";
		if (!cacheOfImages.containsKey(file) && fileWithTiles != null) {
			File en = new File(fileWithTiles, file);
			if (cacheOfImages.size() > maxImgCacheSize) {
				ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
				for (int i = 0; i < list.size(); i += 2) {
					Bitmap bmp = cacheOfImages.remove(list.get(i));
					bmp.recycle();
				}
				System.gc();
			}
			if (en.exists() && en.canRead()) {
				cacheOfImages.put(file, BitmapFactory.decodeFile(en.getAbsolutePath()));
			} else {
				cacheOfImages.put(file, null);
			}
		}

		return cacheOfImages.get(file);
	}
	

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		prepareImage();
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	// TODO async loading images (show busy cursor while it is loaded)
	public void prepareImage() {
		double xTileLeft = getXTile() - getWidth() / (2d * tileSize);
		double xTileRight = getXTile() + getWidth() / (2d * tileSize);
		double yTileUp = getYTile() - getHeight() / (2d * tileSize);
		double yTileDown = getYTile() + getHeight() / (2d * tileSize);

		xStartingImage = -(int) ((xTileLeft - Math.floor(xTileLeft)) * tileSize);
		yStartingImage = -(int) ((yTileUp - Math.floor(yTileUp)) * tileSize);

		int tileXCount = ((int) xTileRight - (int) xTileLeft + 1);
		int tileYCount = ((int) yTileDown - (int) yTileUp + 1);
		images = new Bitmap[tileXCount][tileYCount];
		for (int i = 0; i < images.length; i++) {
			for (int j = 0; j < images[i].length; j++) {
				images[i][j] = getImageFor((int) xTileLeft + i, (int) yTileUp + j);
			}
		}
		invalidate();
	}
	
	
	
	public void setZoom(int zoom){
		this.zoom = zoom;
		prepareImage();
	}
	
	public File getFileWithTiles() {
		return fileWithTiles;
	}
	
	public void setFileWithTiles(File fileWithTiles) {
		this.fileWithTiles = fileWithTiles;
		prepareImage();
	}

	public void setLatLon(double latitude, double longitude){
		this.latitude = latitude;
		this.longitude = longitude;
		prepareImage();
		fireMapLocationListeners(null);
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public int getZoom() {
		return zoom;
	}
	
	public int getTileSize() {
		return tileSize;
	}
	

	public void addMapLocationListener(IMapLocationListener l){
		listeners.add(l);
	}
	
	public void removeMapLocationListener(IMapLocationListener l){
		listeners.remove(l);
	}
	
	protected void fireMapLocationListeners(Object source){
		for(IMapLocationListener l : listeners){
			l.locationChanged(latitude, longitude, source);
		}
	}




}
