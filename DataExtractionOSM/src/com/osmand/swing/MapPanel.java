package com.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;

import com.osmand.LogUtil;
import com.osmand.data.DataTileManager;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.IMapLocationListener;
import com.osmand.map.ITileSource;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.Way;

public class MapPanel extends JPanel implements IMapDownloaderCallback {
	
	private static final long serialVersionUID = 1L;
	
	protected static final Log log = LogUtil.getLog(MapPanel.class);
	public static final int divNonLoadedImage = 16;

	public static JMenu getMenuToChooseSource(final MapPanel panel){
		final JMenu tiles = new JMenu("Source of tiles");
		final List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
		for(final TileSourceTemplate l : list){
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(l.getName());
			menuItem.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					for(int i=0; i<tiles.getItemCount(); i++){
						if(list.get(i).equals(l)){
							((JCheckBoxMenuItem)tiles.getItem(i)).setSelected(true);
						} else {
							((JCheckBoxMenuItem)tiles.getItem(i)).setSelected(false);
						}
					}
					panel.setMapName(l);
				}
				
			});
			if(l.equals(TileSourceManager.getMapnikSource())){
				menuItem.setSelected(true);
				panel.setMapName(l);
			}
			tiles.add(menuItem);
		}
		
		return tiles;
	}
	

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Map view");
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final MapPanel panel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
	    frame.addWindowListener(new WindowAdapter(){
	    	@Override
	    	public void windowClosing(WindowEvent e) {
	    		DataExtractionSettings settings = DataExtractionSettings.getSettings();
				settings.saveDefaultLocation(panel.getLatitude(), panel.getLongitude());
				settings.saveDefaultZoom(panel.getZoom());
	    		System.exit(0);
	    	}
	    });
	    Container content = frame.getContentPane();
	    content.add(panel, BorderLayout.CENTER);

	    JMenuBar bar = new JMenuBar();
	    bar.add(getMenuToChooseSource(panel));
	    frame.setJMenuBar(bar);
	    frame.setSize(512, 512);
	    frame.setVisible(true);

	}

	private File tilesLocation = null;
	
	// name of source map 
	private ITileSource map;
	

	// special points to draw
	private DataTileManager<? extends Entity> points;
	
	// zoom level
	private int zoom = 1;
	
	// degree measurements (-180, 180)
	// долгота
	private double longitude;
	// широта
	// degree measurements (90, -90)
	private double latitude;
	
	private List<IMapLocationListener> listeners = new ArrayList<IMapLocationListener>();
	
	private MapSelectionArea selectionArea = new MapSelectionArea();
	
	private LatLon startRoute;
	private LatLon endRoute;
	
	
	// cached data to draw image
	private Image[][] images;
	private int xStartingImage = 0;
	private int yStartingImage = 0;
	private List<Point> pointsToDraw = new ArrayList<Point>();
	private List<Line2D> linesToDraw = new ArrayList<Line2D>();
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance();
	Map<String, Image> cache = new HashMap<String, Image>();

	private JLabel gpsLocation;

	private JButton areaButton;
	
	private JPopupMenu popupMenu;
	private Point popupMenuPoint;
	
	private boolean willBePopupShown = false;
	
	
	
	public MapPanel(File fileWithTiles) {
		ImageIO.setUseCache(false);
		
		tilesLocation = fileWithTiles;
		LatLon defaultLocation = DataExtractionSettings.getSettings().getDefaultLocation();
		latitude = defaultLocation.getLatitude();
		longitude = defaultLocation.getLongitude();
		zoom = DataExtractionSettings.getSettings().getDefaultZoom();
		
		
		addControls();
		
		downloader.addDownloaderCallback(this);
		setFocusable(true);
		addComponentListener(new ComponentAdapter(){
			public void componentResized(ComponentEvent e) {
				prepareImage();
			}
		});
		setOpaque(false);
		MapMouseAdapter mouse = new MapMouseAdapter();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
		
		
	}
	
	@Override
	public void setVisible(boolean flag) {
		super.setVisible(flag);
		if(!flag){
			downloader.removeDownloaderCallback(this);
 		} else {
 			downloader.addDownloaderCallback(this);
 		}
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
	
	
	public int getMapXForPoint(double longitude){
		double tileX = MapUtils.getTileNumberX(zoom, longitude);
		return (int) ((tileX - getXTile()) * getTileSize() + getCenterPointX());
	}
	private double getCenterPointX() {
		return getWidth() / 2;
	}


	public int getMapYForPoint(double latitude){
		double tileY = MapUtils.getTileNumberY(zoom, latitude);
		return (int) ((tileY - getYTile()) * getTileSize() + getCenterPointY());
	}

	private double getCenterPointY() {
		return getHeight() / 2;
	}


	@Override
	protected void paintComponent(Graphics g) {
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				for (int j = 0; j < images[i].length; j++) {
					if (images[i][j] == null) {
						int div = divNonLoadedImage;
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
		g.setColor(Color.green);
		if(startRoute != null){
			int x = getMapXForPoint(startRoute.getLongitude());
			int y = getMapYForPoint(startRoute.getLatitude());
			g.drawOval(x, y, 12, 12);
			g.fillOval(x, y, 12, 12);
		}
		g.setColor(Color.red);
		if(endRoute != null){
			int x = getMapXForPoint(endRoute.getLongitude());
			int y = getMapYForPoint(endRoute.getLatitude());
			g.drawOval(x, y, 12, 12);
			g.fillOval(x, y, 12, 12);
		}
		g.setColor(Color.black);
		// draw user points
		int[] xPoints = new int[4];
		int[] yPoints = new int[4];
		for (Line2D p : linesToDraw) {
			AffineTransform transform = new AffineTransform();
			transform.translate(p.getX1(), p.getY1());
			transform.rotate(p.getX2() - p.getX1(), p.getY2() - p.getY1());
			xPoints[1] = xPoints[0] = 0;
			xPoints[2] = xPoints[3] = (int) Math.sqrt((p.getX2() - p.getX1())*(p.getX2() - p.getX1()) + 
					(p.getY2() - p.getY1())*(p.getY2() - p.getY1())) +1;
			yPoints[3] = yPoints[0] = 0;
			yPoints[2] = yPoints[1] = 2;
			for(int i=0; i< 4; i++){
				Point2D po = transform.transform(new Point(xPoints[i], yPoints[i]), null);
				xPoints[i] = (int) po.getX();
				yPoints[i] = (int) po.getY();
			}
			g.drawPolygon(xPoints, yPoints, 4);
			g.fillPolygon(xPoints, yPoints, 4);
		}
		
		if(selectionArea.isVisible()){
			g.setColor(new Color(0, 0, 230, 50));
			Rectangle r = selectionArea.getSelectedArea();
			g.fillRect(r.x, r.y, r.width, r.height);
		}
		
		
		g.setColor(Color.black);
		g.fillOval(getWidth() / 2 - 2, getHeight() / 2 - 2, 4, 4);
		g.drawOval(getWidth() / 2 - 2, getHeight() / 2 - 2, 4, 4);
		g.drawOval(getWidth() / 2 - 5, getHeight() / 2 - 5, 10, 10);
		
		super.paintComponent(g);
	}
	
	

	
	public File getTilesLocation() {
		return tilesLocation;
	}
	
	public void setTilesLocation(File tilesLocation) {
		this.tilesLocation = tilesLocation;
		cache.clear();
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
			if(cache.size() > 100){
				ArrayList<String> list = new ArrayList<String>(cache.keySet());
				for(int i=0; i<list.size(); i+=2){
					Image remove = cache.remove(list.get(i));
					remove.flush();
				}
				if(log.isInfoEnabled()){
					log.info("Before running gc on map tiles. Total Memory : " + (Runtime.getRuntime().totalMemory() >> 20) + " Mb. Used memory : " 
						+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + " Mb");
				}
				System.gc();
				if(log.isInfoEnabled()){
					log.info("After running gc on map tiles. Total Memory : " + (Runtime.getRuntime().totalMemory() >> 20) + " Mb. Used memory : " 
						+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + " Mb");
				}
			}
			if (!downloader.isFileCurrentlyDownloaded(en)) {
				if (en.exists()) {
//					long time = System.currentTimeMillis();
					try {
						cache.put(file, ImageIO.read(en));
//						if (log.isDebugEnabled()) {
//							log.debug("Loaded file : " + file + " " + (System.currentTimeMillis() - time) + " ms");
//						}
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
	
	public void setAreaActionHandler(Action a){
		areaButton.setAction(a);
	}
	
	@Override
	public void tileDownloaded(DownloadRequest request) {
		if(request == null){
			prepareImage(false);
			return;	
		}
		int tileSize = getTileSize();
		double xTileLeft = getXTile() - getSize().width / (2d * tileSize);
		double yTileUp = getYTile() - getSize().height / (2d * tileSize);
		int i = request.xTile - (int)xTileLeft;
		int j = request.yTile - (int)yTileUp;
		if (request.zoom == this.zoom && (i >= 0 && i < images.length) && (j >= 0 && j < images[i].length)) {
			try {
				System.out.println();
				images[i][j] = getImageFor(request.xTile, request.yTile, zoom, false);
				repaint();
			} catch (IOException e) {
				log.error("Eror reading png " + request.xTile + " " + request.yTile + " zoom : " + zoom, e);
			}

		}
	}
	
	public void prepareImage(){
		prepareImage(DataExtractionSettings.getSettings().useInternetToLoadImages());
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
				List<? extends Entity> objects = points.getObjects(latUp, longUp, latDown, longDown);
				pointsToDraw.clear();
				linesToDraw.clear();
				for (Entity e : objects) {
					if(e instanceof Way){
						List<Node> nodes = ((Way)e).getNodes();
						if (nodes.size() > 1) {
							int prevPixX = 0;
							int prevPixY = 0;
							for (int i = 0; i < nodes.size(); i++) {
								Node n = nodes.get(i);
								int pixX = MapUtils.getPixelShiftX(zoom, n.getLongitude(), this.longitude, tileSize) + getWidth() / 2;
								int pixY = MapUtils.getPixelShiftY(zoom, n.getLatitude(), this.latitude, tileSize) + getHeight() / 2;
								if (i > 0) {
									linesToDraw.add(new Line2D.Float(pixX, pixY, prevPixX, prevPixY));
								}
								prevPixX = pixX;
								prevPixY = pixY;
							}
						}
						
					} else if(e instanceof Node){
						Node n = (Node) e;
						int pixX = MapUtils.getPixelShiftX(zoom, n.getLongitude(), this.longitude, tileSize) + getWidth() / 2;
						int pixY = MapUtils.getPixelShiftY(zoom, n.getLatitude(), this.latitude, tileSize) + getHeight() / 2;
						if (pixX >= 0 && pixY >= 0) {
							pointsToDraw.add(new Point(pixX, pixY));
						}
					} else {
					} 
				}
			}
			
			repaint();
		} catch (IOException e) {
			log.error("Eror reading png preparing images");
		}
	}
	
	
	
	public void setZoom(int zoom){
		if(map != null && (zoom > map.getMaximumZoomSupported() || zoom < map.getMinimumZoomSupported())){
			return;
		}
		this.zoom = zoom;
		updateLocationLabel();
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
	
	public MapSelectionArea getSelectionArea() {
		return selectionArea;
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
	
	private void updateLocationLabel(){
		gpsLocation.setText(MessageFormat.format("Lat : {0,number,#.####}, lon : {1,number,#.####}, zoom : {2}", latitude, longitude, zoom));
	}
	
	protected void fireMapLocationListeners(){
		updateLocationLabel();
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
			if(e.getKeyChar() == '+' || e.getKeyChar() == '=' ){
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
	
	public DataTileManager<? extends Entity> getPoints() {
		return points;
	}
	
	public void setPoints(DataTileManager<? extends Entity> points) {
		this.points = points;
		prepareImage();
	}
	
	public void addControls(){
		BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
		setLayout(layout);
		setBorder(BorderFactory.createEmptyBorder(2, 10, 10, 10));
		
		gpsLocation = new JLabel();
		gpsLocation.setOpaque(false);
		updateLocationLabel();
		
		JButton zoomIn = new JButton("+");
		JButton zoomOut = new JButton("-");
		areaButton = new JButton();
		areaButton.setAction(new AbstractAction("Preload area"){
			private static final long serialVersionUID = -5512220294374994021L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new TileBundleDownloadDialog(MapPanel.this, MapPanel.this).showDialog();
			}
			
		});
		zoomIn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setZoom(getZoom() + 1);
			}
		});
		zoomOut.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setZoom(getZoom() - 1);
			}
		});
		
		add(gpsLocation);
		add(Box.createHorizontalGlue());
		add(areaButton);
		add(zoomIn);
		add(zoomOut);
		gpsLocation.setAlignmentY(Component.TOP_ALIGNMENT);
		areaButton.setVisible(false);
		areaButton.setAlignmentY(Component.TOP_ALIGNMENT);
		zoomOut.setAlignmentY(Component.TOP_ALIGNMENT);
		zoomIn.setAlignmentY(Component.TOP_ALIGNMENT);
		popupMenu = new JPopupMenu();
		fillPopupMenuWithActions(popupMenu);
	}
	
	public void fillPopupMenuWithActions(JPopupMenu menu) {
		Action start = new AbstractAction("Mark start point") {
			private static final long serialVersionUID = 507156107455281238L;

			public void actionPerformed(ActionEvent e) {
				double fy = (popupMenuPoint.y - getCenterPointY()) / getTileSize();
				double fx = (popupMenuPoint.x - getCenterPointX()) / getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + fx);
				startRoute = new LatLon(latitude, longitude);
				repaint();
			}
		};
		menu.add(start);
		Action end= new AbstractAction("Mark end point") {
			private static final long serialVersionUID = 4446789424902471319L;

			public void actionPerformed(ActionEvent e) {
				double fy = (popupMenuPoint.y - getCenterPointY()) / getTileSize();
				double fx = (popupMenuPoint.x - getCenterPointX()) / getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + fx);
				endRoute = new LatLon(latitude, longitude);
				repaint();
			}
		};
		menu.add(end);
		Action route = new AbstractAction("Calculate route") {
			private static final long serialVersionUID = 507156107455281238L;

			public void actionPerformed(ActionEvent e) {
				List<Way> ways = RoutingHelper.route(startRoute, endRoute);
				DataTileManager<Way> points = new DataTileManager<Way>();
				points.setZoom(11);
				for(Way w : ways){
					LatLon n = w.getLatLon();
					points.registerObject(n.getLatitude(), n.getLongitude(), w);
				}
				MapPanel.this.points = points;
				prepareImage();
			}
		};
		menu.add(route);

	}
	
	public class MapSelectionArea {
		
		private double lat1;
		private double lon1;
		private double lat2;
		private double lon2;
		
		
		public double getLat1() {
			return lat1;
		}
		
		public double getLat2() {
			return lat2;
		}
		
		public double getLon1() {
			return lon1;
		}
		
		public double getLon2() {
			return lon2;
		}
		
		public Rectangle getSelectedArea(){
			Rectangle r = new Rectangle();
			r.x = getWidth() / 2 + MapUtils.getPixelShiftX(zoom, lon1, getLongitude(), getTileSize());
			r.y = getHeight() / 2 + MapUtils.getPixelShiftY(zoom, lat1, getLatitude(), getTileSize());
			r.width = getWidth() / 2 + MapUtils.getPixelShiftX(zoom, lon2, getLongitude(), getTileSize()) - r.x;
			r.height = getHeight() / 2 + MapUtils.getPixelShiftY(zoom, lat2, getLatitude(), getTileSize()) - r.y;
			return r;
		}
		
		public boolean isVisible(){
			if(lat1 == lat2 || lon1 == lon2){
				return false;
			}
			Rectangle area = getSelectedArea();
			return area.width > 4 && area.height > 4;
		}
		
		
		public void setSelectedArea(int x1, int y1, int x2, int y2){
			int rx1 = Math.min(x1, x2);
			int rx2 = Math.max(x1, x2);
			int ry1 = Math.min(y1, y2);
			int ry2 = Math.max(y1, y2);
			int zoom = getZoom();
			double xTile = getXTile();
			double yTile = getYTile();
			int wid = getWidth();
			int h = getHeight();
			int tileSize = getTileSize();
			
			double xTile1 = xTile - (wid / 2 - rx1) / ((double)tileSize);
			double yTile1 = yTile - (h / 2 - ry1) / ((double)tileSize);
			double xTile2 = xTile - (wid / 2 - rx2) / ((double)tileSize);
			double yTile2 = yTile - (h / 2 - ry2) / ((double)tileSize);
			lat1 = MapUtils.getLatitudeFromTile(zoom, yTile1);
			lat2 = MapUtils.getLatitudeFromTile(zoom, yTile2);
			lon1 = MapUtils.getLongitudeFromTile(zoom, xTile1);
			lon2 = MapUtils.getLongitudeFromTile(zoom, xTile2);
			areaButton.setVisible(isVisible());
		}
		
	}
	
	public class MapMouseAdapter extends MouseAdapter {
		private Point startDragging = null;
		private Point startSelecting = null;
		
		@Override
		public void mouseClicked(MouseEvent e) {
			requestFocus();
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
			willBePopupShown = false;
			if(startDragging != null){
				if(Math.abs(e.getPoint().x - startDragging.x) +  Math.abs(e.getPoint().y - startDragging.y) >= 8){
					dragTo(e.getPoint());
					startDragging = e.getPoint();
				}
			}
			if(startSelecting != null){
				selectionArea.setSelectedArea(startSelecting.x, startSelecting.y, e.getPoint().x, e.getPoint().y);
				updateUI();
			}
		}
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(e.getWheelRotation() < 0){
				setZoom(getZoom() + 1);
			} else if(e.getWheelRotation() > 0) {
				setZoom(getZoom() - 1);
			}
			super.mouseWheelMoved(e);
		}
		@Override
		public void mousePressed(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON3){
				if(startDragging == null){
					startDragging  = e.getPoint();
				}
			} else if(e.getButton() == MouseEvent.BUTTON1){
				startSelecting = e.getPoint();
			}
			willBePopupShown = true;
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
			if(e.getButton() == MouseEvent.BUTTON1){
				if(startSelecting != null){
					selectionArea.setSelectedArea(startSelecting.x, startSelecting.y, e.getPoint().x, e.getPoint().y);
					startSelecting = null;
				}
			}
			
			// possible bug if popup neither button1|| button3
			if(willBePopupShown && e.isPopupTrigger()){
				popupMenuPoint = new Point(e.getX(), e.getY());
				popupMenu.show(MapPanel.this, e.getX(), e.getY());
				
				willBePopupShown = false;
			}			
			super.mouseReleased(e);
		}

	}

}
