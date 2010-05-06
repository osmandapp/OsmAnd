package com.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;

import com.osmand.DefaultLauncherConstants;
import com.osmand.IMapLocationListener;
import com.osmand.LogUtil;
import com.osmand.MapTileDownloader;
import com.osmand.MapTileDownloader.DownloadRequest;
import com.osmand.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.data.DataTileManager;
import com.osmand.map.ITileSource;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class MapPanel extends JPanel implements IMapDownloaderCallback {
	
	private static final long serialVersionUID = 1L;
	
	protected static final Log log = LogUtil.getLog(MapPanel.class);

	public static Menu getMenuToChooseSource(final MapPanel panel){
		Menu tiles = new Menu("Source tile");
		List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
		for(final TileSourceTemplate l : list){
			MenuItem menuItem = new MenuItem(l.getName());
			menuItem.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					panel.setMapName(l);
				}
				
			});
			tiles.add(menuItem);
		}
		
		return tiles;
	}
	

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Tree of choose");
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	    frame.addWindowListener(new OsmExtractionUI.ExitListener());
	    Container content = frame.getContentPane();
	    
	    MapPanel panel = new MapPanel(new File(DefaultLauncherConstants.pathToDirWithTiles));
	    
	    content.add(panel, BorderLayout.CENTER);

	    MenuBar bar = new MenuBar();
	    bar.add(getMenuToChooseSource(panel));
	    frame.setMenuBar(bar);
	    frame.setSize(512, 512);
	    frame.setVisible(true);

	}

	private File tilesLocation = null;
	
	// name of source map 
	private ITileSource map = DefaultLauncherConstants.MAP_defaultTileSource;
	

	// special points to draw
	private DataTileManager<LatLon> points;
	
	// zoom level
	private int zoom = DefaultLauncherConstants.MAP_startMapZoom;
	
	// degree measurements (-180, 180)
	// долгота
	private double longitude = DefaultLauncherConstants.MAP_startMapLongitude;
	// широта
	// degree measurements (90, -90)
	private double latitude = DefaultLauncherConstants.MAP_startMapLatitude;
	
	private List<IMapLocationListener> listeners = new ArrayList<IMapLocationListener>();
	
	// cached data to draw image
	private Image[][] images;
	private int xStartingImage = 0;
	private int yStartingImage = 0;
	private List<Point> pointsToDraw = new ArrayList<Point>();
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance();
	Map<String, Image> cache = new HashMap<String, Image>();
	
	
	
	public MapPanel(File fileWithTiles) {
		tilesLocation = fileWithTiles;
		downloader.setDownloaderCallback(this);
		setFocusable(true);
		addComponentListener(new ComponentAdapter(){
			public void componentResized(ComponentEvent e) {
				prepareImage();
			}
		});
		MapMouseAdapter mouse = new MapMouseAdapter();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	
	public double getXTile(){
		return MapUtils.getTileNumberX(zoom, longitude);
	}
	
	public double getYTile(){
		return MapUtils.getTileNumberY(zoom, latitude);
	}
	
	public int getTileSize(){
		return map == null ?  256 : map.getTileSize();
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				for (int j = 0; j < images[i].length; j++) {
					if (images[i][j] == null) {
						int div = DefaultLauncherConstants.MAP_divNonLoadedImage;
						int tileDiv = getTileSize() / div;
						for (int k1 = 0; k1 < div; k1++) {
							for (int k2 = 0; k2 < div; k2++) {
								if ((k1 + k2) % 2 == 0) {
									g.setColor(Color.gray);
								} else {
									g.setColor(Color.white);
								}
								g.fillRect(i * getTileSize() + xStartingImage + k1 * tileDiv, j * getTileSize() + yStartingImage + k2
										* tileDiv, tileDiv, tileDiv);

							}
						}
					} else {
						g.drawImage(images[i][j], i * getTileSize() + xStartingImage, j * getTileSize() + yStartingImage, this);
					}
				}
			}
		}
		g.setColor(Color.black);
		// draw user points
		for (Point p : pointsToDraw) {
			g.drawOval(p.x, p.y, 3, 3);
			g.fillOval(p.x, p.y, 3, 3);
		}

		String s = MessageFormat.format("Lat : {0}, lon : {1}, zoom : {2}", latitude, longitude, zoom);
		g.drawString(s, 5, 20);

		g.fillOval(getWidth() / 2 - 2, getHeight() / 2 - 2, 4, 4);
		g.drawOval(getWidth() / 2 - 2, getHeight() / 2 - 2, 4, 4);
		g.drawOval(getWidth() / 2 - 5, getHeight() / 2 - 5, 10, 10);
	}
	
	

	
	public File getTilesLocation() {
		return tilesLocation;
	}
	
	public void setTilesLocation(File tilesLocation) {
		this.tilesLocation = tilesLocation;
		prepareImage();
	}
	
	 
	public String getFileForImage (int x, int y, int zoom, String ext){
		return map.getName() +"/"+zoom+"/"+(x) +"/"+y+ext+".tile";
	}
	
	public Image getImageFor(int x, int y, int zoom, boolean loadIfNeeded) throws IOException{
		if(map == null){
			return null;
		}
		String file = getFileForImage(x, y, zoom, map.getTileFormat());
		if(cache.get(file) == null){
			File en = new File(tilesLocation, file);
			if(cache.size() > 1000){
				ArrayList<String> list = new ArrayList<String>(cache.keySet());
				for(int i=0; i<list.size(); i+=2){
					cache.remove(list.get(i));
				}
			}
			if (!downloader.isFileCurrentlyDownloaded(en)) {
				if (en.exists()) {
					long time = System.currentTimeMillis();
					try {
						cache.put(file, ImageIO.read(en));
						if (log.isDebugEnabled()) {
							log.debug("Loaded file : " + file + " " + -(time - System.currentTimeMillis()) + " ms");
						}
					} catch (IIOException e) {
						log.error("Eror reading png " + x + " " + y + " zoom : " + zoom, e);
					}
				} 
				if(loadIfNeeded && cache.get(file) == null){
					String urlToLoad = map.getUrlToLoad(x, y, zoom);
					if (urlToLoad != null) {
						downloader.requestToDownload(new DownloadRequest(urlToLoad, en, x, y, zoom));
					}
				}
			}
		}
		
		return cache.get(file);
	}
	
	@Override
	public void tileDownloaded(DownloadRequest request) {
		int tileSize = getTileSize();
		double xTileLeft = getXTile() - getSize().width / (2d * tileSize);
		double yTileUp = getYTile() - getSize().height / (2d * tileSize);
		int i = request.xTile - (int)xTileLeft;
		int j = request.yTile - (int)yTileUp;
		if(request.zoom == this.zoom && 
				(i >=0 && i<images.length) && (j>=0 && j< images[i].length)){
			try {
				images[i][j] = getImageFor(request.xTile, request.yTile, zoom, false);
				repaint();
			} catch (IOException e) {
				log.error("Eror reading png " + request.xTile +" " + request.yTile + " zoom : " + zoom, e);
			}
			
		}
	}
	
	public void prepareImage(){
		prepareImage(DefaultLauncherConstants.loadMissingImages);
	}
	
	public void prepareImage(boolean loadNecessaryImages){
		try {
			int tileSize = getTileSize();
			if (images != null) {
				for (int i = 0; i < images.length; i++) {
					for (int j = 0; j < images[i].length; j++) {
						// dispose 
					}
				}
			}
			
			double xTileLeft = getXTile() - getSize().width / (2d * tileSize);
			double xTileRight = getXTile() + getSize().width / (2d * tileSize);
			double yTileUp = getYTile() - getSize().height / (2d * tileSize);
			double yTileDown = getYTile() + getSize().height / (2d * tileSize);
		    
			xStartingImage = -(int) ((xTileLeft - Math.floor(xTileLeft)) * tileSize);
			yStartingImage = -(int) ((yTileUp - Math.floor(yTileUp)) * tileSize);

			if(loadNecessaryImages){
				downloader.refuseAllPreviousRequests();
			}
			int tileXCount = ((int) xTileRight - (int) xTileLeft + 1);
			int tileYCount = ((int) yTileDown - (int) yTileUp + 1);
			images = new BufferedImage[tileXCount][tileYCount];
			for (int i = 0; i < images.length; i++) {
				for (int j = 0; j < images[i].length; j++) {
					int x= (int) xTileLeft + i;
					int y = (int) yTileUp + j;
					images[i][j] = getImageFor(x, y, zoom, loadNecessaryImages);
				}
			}
			
			if (points != null) {
				double latDown = MapUtils.getLatitudeFromTile(zoom, yTileDown);
				double longDown = MapUtils.getLongitudeFromTile(zoom, xTileRight);
				double latUp = MapUtils.getLatitudeFromTile(zoom, yTileUp);
				double longUp = MapUtils.getLongitudeFromTile(zoom, xTileLeft);
				List<LatLon> objects = points.getObjects(latUp, longUp, latDown, longDown);
				pointsToDraw.clear();
				for (LatLon n : objects) {
					int pixX = MapUtils.getPixelShiftX(zoom, n.getLongitude(), this.longitude, tileSize) + getWidth() / 2;
					int pixY = MapUtils.getPixelShiftY(zoom, n.getLatitude(), this.latitude, tileSize) + getHeight() / 2;
					if (pixX >= 0 && pixY >= 0) {
						pointsToDraw.add(new Point(pixX, pixY));
					}
				}
			}
			
			repaint();
		} catch (IOException e) {
			log.error("Eror reading png preparing images");
		}
	}
	
	
	
	public void setZoom(int zoom){
		this.zoom = zoom;
		prepareImage();
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
	
	public ITileSource getMap(){
		return map;
	}
	
	public void setMapName(ITileSource map){
		this.map = map;
		if(map.getMaximumZoomSupported() < this.zoom){
			zoom = map.getMaximumZoomSupported();
		}
		if(map.getMinimumZoomSupported() > this.zoom){
			zoom = map.getMinimumZoomSupported();
		}
		prepareImage();
	}
	
	public void addMapLocationListener(IMapLocationListener l){
		listeners.add(l);
	}
	
	public void removeMapLocationListener(IMapLocationListener l){
		listeners.remove(l);
	}
	
	protected void fireMapLocationListeners(){
		for(IMapLocationListener l : listeners){
			l.locationChanged(latitude, longitude, null);
		}
	}
	
	
	@Override
	protected void processKeyEvent(KeyEvent e) {
		boolean processed = false;
		if (e.getID() == KeyEvent.KEY_RELEASED) {
			if (e.getKeyCode() == 37) {
				// LEFT button
				longitude = MapUtils.getLongitudeFromTile(zoom, getXTile()-0.5); 
				processed = true;
			} else if (e.getKeyCode() == 39) {
				// RIGHT button
				longitude = MapUtils.getLongitudeFromTile(zoom, getXTile()+0.5);
				processed = true;
			} else if (e.getKeyCode() == 38) {
				// UP button
				latitude = MapUtils.getLatitudeFromTile(zoom, getYTile()-0.5);
				processed = true;
			} else if (e.getKeyCode() == 40) {
				// DOWN button
				latitude = MapUtils.getLatitudeFromTile(zoom, getYTile()+0.5);
				processed = true;
			}
		}
		if(e.getID() == KeyEvent.KEY_TYPED){
			if(e.getKeyChar() == '+'){
				if(zoom < map.getMaximumZoomSupported()){
					zoom ++;
					processed = true;
				}
			} else if(e.getKeyChar() == '-'){
				if(zoom > map.getMinimumZoomSupported()){
					zoom --;
					processed = true;
				}
			}
		}
		
		if(processed){
			e.consume();
			prepareImage();
			fireMapLocationListeners();
		}
		super.processKeyEvent(e);
	}
	
	public DataTileManager<LatLon> getPoints() {
		return points;
	}
	
	public void setPoints(DataTileManager<LatLon> points) {
		this.points = points;
	}
	
	public class MapMouseAdapter extends MouseAdapter {
		private Point startDragging = null;
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1){
				requestFocus();
			}
		}
		
		public void dragTo(Point p){
			double dx = (startDragging.x - (double)p.x)/getTileSize(); 
			double dy = (startDragging.y - (double)p.y)/getTileSize();
			double lat = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
			double lon = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
			setLatLon(lat, lon);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if(startDragging != null){
				if(Math.abs(e.getPoint().x - startDragging.x) +  Math.abs(e.getPoint().y - startDragging.y) >= 8){
					dragTo(e.getPoint());
					startDragging = e.getPoint();
				}
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON3){
				if(startDragging == null){
					startDragging  = e.getPoint();
				}
			}
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON3){
				if(startDragging != null){
					dragTo(e.getPoint());
					fireMapLocationListeners();
					startDragging = null;
				}
			}
			super.mouseReleased(e);
		}

	}

}
