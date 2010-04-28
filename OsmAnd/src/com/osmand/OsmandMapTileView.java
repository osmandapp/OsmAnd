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
	
	public OsmandMapTileView(Context context, File fileWithTiles) {
		super(context);
		this.fileWithTiles = fileWithTiles;
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
		// TODO !!!!
//		MapMouseAdapter mouse = new MapMouseAdapter();
//		addMouseListener(mouse);
//		addMouseMotionListener(mouse);
	}
	
	
	private PointF startDragging = null;

	public void dragTo(PointF p){
		double dx = (startDragging.x - (double)p.x)/tileSize; 
		double dy = (startDragging.y - (double)p.y)/tileSize;
		double lat = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
		double lon = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
		setLatLon(lat, lon);
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
				fireMapLocationListeners();
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
				if ((k1 + k2) % 2 == 0) {
					cvs.drawRect(x, y, x + tileDiv, y + tileDiv, paintGrayFill);
				} else {
					cvs.drawRect(x, y, x + tileDiv, y + tileDiv, paintWhiteFill);
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
		if (!cacheOfImages.containsKey(file)) {
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
	}

	public void setLatLon(double latitude, double longitude){
		this.latitude = latitude;
		this.longitude = longitude;
		prepareImage();
		fireMapLocationListeners();
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
	

	public void addMapLocationListener(IMapLocationListener l){
		listeners.add(l);
	}
	
	public void removeMapLocationListener(IMapLocationListener l){
		listeners.remove(l);
	}
	
	protected void fireMapLocationListeners(){
		for(IMapLocationListener l : listeners){
			l.locationChanged(latitude, longitude);
		}
	}
	
//	
//	@Override
//	protected void processKeyEvent(KeyEvent e) {
//		boolean processed = false;
//		if (e.getID() == KeyEvent.KEY_RELEASED) {
//			if (e.getKeyCode() == 37) {
//				// LEFT button
//				longitude = MapUtils.getLongitudeFromTile(zoom, getXTile()-0.5); 
//				processed = true;
//			} else if (e.getKeyCode() == 39) {
//				// RIGHT button
//				longitude = MapUtils.getLongitudeFromTile(zoom, getXTile()+0.5);
//				processed = true;
//			} else if (e.getKeyCode() == 38) {
//				// UP button
//				latitude = MapUtils.getLatitudeFromTile(zoom, getYTile()-0.5);
//				processed = true;
//			} else if (e.getKeyCode() == 40) {
//				// DOWN button
//				latitude = MapUtils.getLatitudeFromTile(zoom, getYTile()+0.5);
//				processed = true;
//			}
//		}
//		if(e.getID() == KeyEvent.KEY_TYPED){
//			if(e.getKeyChar() == '+'){
//				zoom ++;
//				processed = true;
//			} else if(e.getKeyChar() == '-'){
//				zoom --;
//				processed = true;
//			}
//		}
//		
//		if(processed){
//			e.consume();
//			prepareImage();
//			fireMapLocationListeners();
//		}
//		super.processKeyEvent(e);
//	}
//	
//	
//	public class MapMouseAdapter extends MouseAdapter {
//		private Point startDragging = null;
//		
//		@Override
//		public void mouseClicked(MouseEvent e) {
//			if(e.getButton() == MouseEvent.BUTTON1){
//				requestFocus();
//			}
//		}
//		
//		public void dragTo(Point p){
//			double dx = (startDragging.x - (double)p.x)/tileSize; 
//			double dy = (startDragging.y - (double)p.y)/tileSize;
//			double lat = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
//			double lon = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
//			setLatLon(lat, lon);
//		}
//		
//		@Override
//		public void mouseDragged(MouseEvent e) {
//			if(startDragging != null){
//				if(Math.abs(e.getPoint().x - startDragging.x) +  Math.abs(e.getPoint().y - startDragging.y) >= 8){
//					dragTo(e.getPoint());
//					startDragging = e.getPoint();
//				}
//			}
//		}
//		
//		@Override
//		public void mousePressed(MouseEvent e) {
//			if(e.getButton() == MouseEvent.BUTTON3){
//				if(startDragging == null){
//					startDragging  = e.getPoint();
//				}
//			}
//		}
//		@Override
//		public void mouseReleased(MouseEvent e) {
//			if(e.getButton() == MouseEvent.BUTTON3){
//				if(startDragging != null){
//					dragTo(e.getPoint());
//					fireMapLocationListeners();
//					startDragging = null;
//				}
//			}
//			super.mouseReleased(e);
//		}
//
//	}


}
