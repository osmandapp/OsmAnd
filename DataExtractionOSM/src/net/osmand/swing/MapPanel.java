package net.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.MapCreatorVersion;
import net.osmand.data.DataTileManager;
import net.osmand.data.MapTileDownloader;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;


public class MapPanel extends JPanel implements IMapDownloaderCallback {
	
	private static final long serialVersionUID = 1L;
	
	private static final int EXPAND_X = 100;
	private static final int EXPAND_Y = 100;
	
	protected static final Log log = LogUtil.getLog(MapPanel.class);
	public static final int divNonLoadedImage = 16;
	
	private static Map<String, TileSourceTemplate> getCommonTemplates(File dir){
		final List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
		Map<String, TileSourceTemplate> map = new LinkedHashMap<String, TileSourceTemplate>();
		for(TileSourceTemplate t : list){
			map.put(t.getName(), t);
		}
		if (!dir.isDirectory()) {
			return map;
		}
		for(File f : dir.listFiles()){
			if(f.isDirectory()){
				if(map.containsKey(f.getName())){
					if(TileSourceManager.isTileSourceMetaInfoExist(f)){
						map.put(f.getName(), TileSourceManager.createTileSourceTemplate(f));
					} else {
						try {
							TileSourceManager.createMetaInfoFile(f, map.get(f.getName()), false);
						} catch (IOException e) {
						}
					}
				} else {
					map.put(f.getName(), TileSourceManager.createTileSourceTemplate(f));
				}
				
			}
		}
		return map;
	}

	
	public static JMenu getMenuToChooseSource(final MapPanel panel){
		final JMenu tiles = new JMenu(Messages.getString("MapPanel.SOURCE.OF.TILES")); //$NON-NLS-1$
		final JMenu downloadedMenu = new JMenu("Additional"); //$NON-NLS-1$
		final File tilesDirectory = DataExtractionSettings.getSettings().getTilesDirectory();
		Map<String, TileSourceTemplate> udf = getCommonTemplates(tilesDirectory);
		final List<TileSourceTemplate> downloaded = TileSourceManager.downloadTileSourceTemplates(MapCreatorVersion.APP_VERSION);
		final Map<TileSourceTemplate, JCheckBoxMenuItem> items = new IdentityHashMap<TileSourceTemplate, JCheckBoxMenuItem>();
		
		tiles.add(downloadedMenu);
		for(final TileSourceTemplate l : udf.values()){
			if(l == null){
				continue;
			}
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(l.getName());
			tiles.add(menuItem);
			items.put(l, menuItem);
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (final Map.Entry<TileSourceTemplate, JCheckBoxMenuItem> es : items.entrySet()) {
						es.getValue().setSelected(l.equals(es.getKey()));
					}
					File dir = new File(tilesDirectory, l.getName());
					try {
						dir.mkdirs();
						TileSourceManager.createMetaInfoFile(dir, l, false);
					} catch (IOException e1) {
					}
					panel.setMapName(l);
				}
			});
		}
		
		if (downloaded != null) {
			for (final TileSourceTemplate l : downloaded) {
				if (l == null) {
					continue;
				}
				JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(l.getName());
				downloadedMenu.add(menuItem);
				items.put(l, menuItem);
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for (final Map.Entry<TileSourceTemplate, JCheckBoxMenuItem> es : items.entrySet()) {
							es.getValue().setSelected(l.equals(es.getKey()));
						}
						File dir = new File(tilesDirectory, l.getName());
						try {
							dir.mkdirs();
							TileSourceManager.createMetaInfoFile(dir, l, true);
						} catch (IOException e1) {
						}
						panel.setMapName(l);
					}
				});
			}
		}

		for (final Map.Entry<TileSourceTemplate, JCheckBoxMenuItem> em : items.entrySet()) {
			if(Algoritms.objectEquals(panel.getMap(), em.getKey())){
				em.getValue().setSelected(true);
			}
		}

		tiles.addSeparator();
		tiles.add(createNewTileSourceAction(panel, tiles, items));
		return tiles;
	}


	private static AbstractAction createNewTileSourceAction(final MapPanel panel, final JMenu tiles,
			final Map<TileSourceTemplate, JCheckBoxMenuItem> items) {
		return new AbstractAction(Messages.getString("MapPanel.NEW.TILE.SRC")){ //$NON-NLS-1$
			private static final long serialVersionUID = -8286622335859339130L;

			@Override
			public void actionPerformed(ActionEvent e) {
				NewTileSourceDialog dlg = new NewTileSourceDialog(panel);
				dlg.showDialog();
				final TileSourceTemplate l = dlg.getTileSourceTemplate();
				if(l != null){
					JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(l.getName());
					tiles.add(menuItem);
					items.put(l, menuItem);
					menuItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							for (final Map.Entry<TileSourceTemplate, JCheckBoxMenuItem> es : items.entrySet()) {
								es.getValue().setSelected(l.equals(es.getKey()));
							}
							panel.setMapName(l);
						}
					});
					for (final Map.Entry<TileSourceTemplate, JCheckBoxMenuItem> es : items.entrySet()) {
						es.getValue().setSelected(l.equals(es.getKey()));
					}
					panel.setMapName(l);
				}
			}
		};
	}
	

	public static void main(String[] args) throws IOException {
		showMainWindow(512, 512, null);
	}


	public static MapPanel showMainWindow(int wx, int hy, NativeSwingRendering nativeLib) {
		JFrame frame = new JFrame(Messages.getString("MapPanel.MAP.VIEW")); //$NON-NLS-1$
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final MapPanel panel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
		panel.nativeLibRendering = nativeLib;
//		panel.longitude = longitude;
//		panel.latitude = latitude;
//		panel.zoom = zoom;
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
	    frame.setSize(wx, hy);
	    frame.setVisible(true);
	    return panel;
	}

	private File tilesLocation = null;
	
	// name of source map 
	private ITileSource map = TileSourceManager.getMapnikSource();
	
	private NativeSwingRendering nativeLibRendering;
	private NativeRendererRunnable lastAddedRunnable;
	private Image nativeRenderingImg;
	private LatLon nativeLatLon;
	private int nativeZoom;
	
	private ThreadPoolExecutor nativeRenderer = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, 
			new ArrayBlockingQueue<Runnable>(1));
	
	// zoom level
	private int zoom = 1;
	
	// degree measurements (-180, 180)
	private double longitude;
	// degree measurements (90, -90)
	private double latitude;
	
	private List<IMapLocationListener> listeners = new ArrayList<IMapLocationListener>();
	
	private MapSelectionArea selectionArea = new MapSelectionArea();
	
	private List<MapPanelLayer> layers = new ArrayList<MapPanelLayer>();
	
	// cached data to draw image
	private Image[][] images;
	private int xStartingImage = 0;
	private int yStartingImage = 0;
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance(MapCreatorVersion.APP_MAP_CREATOR_VERSION);
	Map<String, Image> cache = new HashMap<String, Image>();

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
		if(map != null){
			if(zoom > map.getMaximumZoomSupported()){
				zoom = map.getMaximumZoomSupported();
			}
			if(zoom < map.getMinimumZoomSupported()){
				zoom = map.getMinimumZoomSupported();
			}
		}
		
		popupMenu = new JPopupMenu();
		downloader.addDownloaderCallback(this);
		setFocusable(true);
		addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent e) {
				prepareImage();
			}
		});
		setOpaque(false);
		MapMouseAdapter mouse = new MapMouseAdapter();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
		
		initDefaultLayers();
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
	
	public double getCenterPointX() {
		return getWidth() / 2;
	}


	public int getMapYForPoint(double latitude){
		double tileY = MapUtils.getTileNumberY(zoom, latitude);
		return (int) ((tileY - getYTile()) * getTileSize() + getCenterPointY());
	}

	public double getCenterPointY() {
		return getHeight() / 2;
	}

	public NativeSwingRendering getNativeLibrary() {
		return nativeLibRendering;
	}
	public void setNativeLibrary( NativeSwingRendering nl) {
		nativeLibRendering = nl;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if(nativeLibRendering != null) {
			// TODO : 1. zoom scale 2. extend margin (these are center positions)
			if (zoom == nativeZoom) {
				int shx = (int) ((-MapUtils.getTileNumberX(zoom, longitude) + MapUtils.getTileNumberX(zoom, nativeLatLon.getLongitude())) * getTileSize());
				int shy = (int) ((-MapUtils.getTileNumberY(zoom, latitude) + MapUtils.getTileNumberY(zoom, nativeLatLon.getLatitude())) * getTileSize());
				shx -= EXPAND_X;
				shy -= EXPAND_Y;
				g.drawImage(nativeRenderingImg, shx, shy, this);
			}
		} else if (images != null) {
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
		for(MapPanelLayer l : layers){
			l.paintLayer((Graphics2D) g);
		}
		
		if(selectionArea.isVisible()){
			g.setColor(new Color(0, 0, 230, 50));
			Rectangle r = selectionArea.getSelectedArea();
			g.fillRect(r.x, r.y, r.width, r.height);
		}
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
		return map.getName() +"/"+zoom+"/"+(x) +"/"+y+ext+".tile"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
					log.info("Before running gc on map tiles. Total Memory : " + (Runtime.getRuntime().totalMemory() >> 20) + " Mb. Used memory : "  //$NON-NLS-1$ //$NON-NLS-2$
						+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + " Mb"); //$NON-NLS-1$
				}
				System.gc();
				if(log.isInfoEnabled()){
					log.info("After running gc on map tiles. Total Memory : " + (Runtime.getRuntime().totalMemory() >> 20) + " Mb. Used memory : "  //$NON-NLS-1$ //$NON-NLS-2$
						+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + " Mb"); //$NON-NLS-1$
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
						log.error("Eror reading png " + x + " " + y + " zoom : " + zoom, e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		if(request == null){
			prepareRasterImage(false);
			return;	
		}
		int tileSize = getTileSize();
		double xTileLeft = getXTile() - getSize().width / (2d * tileSize);
		double yTileUp = getYTile() - getSize().height / (2d * tileSize);
		int i = request.xTile - (int)xTileLeft;
		int j = request.yTile - (int)yTileUp;
		if (request.zoom == this.zoom && (i >= 0 && i < images.length) && (j >= 0 && j < images[i].length)) {
			try {
				images[i][j] = getImageFor(request.xTile, request.yTile, zoom, false);
				repaint();
			} catch (IOException e) {
				log.error("Eror reading png " + request.xTile + " " + request.yTile + " zoom : " + zoom, e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

		}
	}
	
	public void prepareImage(){
		if(nativeLibRendering != null) {
			prepareNativeImage();
			
		} else {
			prepareRasterImage(DataExtractionSettings.getSettings().useInternetToLoadImages());
		}
	}
	
	private synchronized void prepareNativeImage() {
		NativeRendererRunnable runnable = new NativeRendererRunnable(getWidth(), getHeight());
		if(lastAddedRunnable == null || !lastAddedRunnable.contains(runnable)) {
			lastAddedRunnable = runnable;
			nativeRenderer.getQueue().clear();
			nativeRenderer.execute(runnable);
		}
		for(MapPanelLayer l : layers){
			l.prepareToDraw();
		}
		repaint();
	}


	private void prepareRasterImage(boolean loadNecessaryImages){
		try {
			int tileSize = getTileSize();
			if (images != null) {
				for (int i = 0; i < images.length; i++) {
					for (int j = 0; j < images[i].length; j++) {
						// dispose 
					}
				}
			}
			double xTileLeft = getXTile() - getCenterPointX() / tileSize;
			double xTileRight = getXTile() + getCenterPointX() / tileSize;
			double yTileUp = getYTile() - getCenterPointY() / tileSize;
			double yTileDown = getYTile() + getCenterPointY() / tileSize;
		    
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
			
			for(MapPanelLayer l : layers){
				l.prepareToDraw();
			}
			repaint();
		} catch (IOException e) {
			log.error("Eror reading png preparing images"); //$NON-NLS-1$
		}
	}
	
	
	public int getMaximumZoomSupported(){
		if(nativeLibRendering != null) {
			return 21;
		}
		if (map == null) {
			return 18;
		}
		return map.getMaximumZoomSupported();
	}
	
	public int getMinimumZoomSupported(){
		if(nativeLibRendering != null || map == null) {
			return 1;
		}
		return map.getMinimumZoomSupported();
	}
	
	public void setZoom(int zoom){
		if(map != null && (zoom > getMaximumZoomSupported() || zoom < getMinimumZoomSupported())){
			return;
		}
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
	
	public MapSelectionArea getSelectionArea() {
		return selectionArea;
	}
	
	public ITileSource getMap(){
		return map;
	}
	
	public void setMapName(ITileSource map){
		if(!map.couldBeDownloadedFromInternet()){
			JOptionPane.showMessageDialog(this, "That map is not downloadable from internet");
		}
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
	
	public List<MapPanelLayer> getLayers() {
		return layers;
	}
	
	protected void initDefaultLayers() {
		addLayer(new MapInformationLayer());
		addLayer(new MapRouterLayer());
		addLayer(new MapPointsLayer());
		addLayer(new MapClusterLayer());
//		addLayer(new CoastlinesLayer());
	}
	
	public void addLayer(MapPanelLayer l){
		l.initLayer(this);
		layers.add(l);
	}
	
	public void addLayer(int ind, MapPanelLayer l){
		l.initLayer(this);
		layers.add(ind, l);
	}
	
	public boolean removeLayer(MapPanelLayer l){
		return layers.remove(l);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends MapPanelLayer> T getLayer(Class<T> cl){
		for(MapPanelLayer l : layers){
			if(cl.isInstance(l)){
				return (T) l;
			}
		}
		return null;
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
				if(zoom < getMaximumZoomSupported()){
					zoom ++;
					processed = true;
				}
			} else if(e.getKeyChar() == '-'){
				if(zoom > getMinimumZoomSupported()){
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
		return getLayer(MapPointsLayer.class).getPoints();
	}
	
	public void setPoints(DataTileManager<? extends Entity> points) {
		getLayer(MapPointsLayer.class).setPoints(points);
		prepareImage();
	}
	
	public Point getPopupMenuPoint(){
		return popupMenuPoint;
	}
	
	public JPopupMenu getPopupMenu() {
		return popupMenu;
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
			getLayer(MapInformationLayer.class).setAreaButtonVisible(isVisible());
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
			if(willBePopupShown && (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)){
				popupMenuPoint = new Point(e.getX(), e.getY());
				popupMenu.show(MapPanel.this, e.getX(), e.getY());
				willBePopupShown = false;
			}			
			super.mouseReleased(e);
		}

	}
	
	private class NativeRendererRunnable implements Runnable {
		private int sleft;
		private int sright;
		private int stop;
		private int sbottom;
		private int z;
		private LatLon latLon;
		private final int cf;

		public NativeRendererRunnable(int w, int h) {
			int tileSize = getTileSize();
			latLon = new LatLon(latitude, longitude);
			this.z = zoom;
			cf = (1 << (31 - z)) / tileSize;
			sleft = MapUtils.get31TileNumberX(latLon.getLongitude()) - (w / 2) * cf; 
			sright = MapUtils.get31TileNumberX(latLon.getLongitude()) + (w / 2) * cf;
			stop = MapUtils.get31TileNumberY(latLon.getLatitude()) - (h / 2) * cf;
			sbottom = MapUtils.get31TileNumberY(latLon.getLatitude()) + (h / 2) * cf;
		}
		
		public boolean contains(NativeRendererRunnable r) {
			if(r.sright > sright + EXPAND_X * cf || 
					r.sleft < sleft - EXPAND_X * cf || 
				r.stop < stop - EXPAND_Y * cf || 
				r.sbottom > sbottom + EXPAND_Y * cf) {
				return false;
			}
			if(r.z != z){
				return false;
			}
			return true;
		}
		
		
		
		@Override
		public void run() {
			if (nativeRenderer.getQueue().isEmpty()) {
				try {
					nativeRenderingImg = nativeLibRendering.renderImage(sleft - EXPAND_X * cf, 
							sright + EXPAND_X * cf, stop - EXPAND_Y * cf, sbottom + EXPAND_Y * cf, z);
					nativeLatLon = latLon;
					nativeZoom = z;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				}
				repaint();
			}
		}
	}

}
