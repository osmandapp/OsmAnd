package com.osmand;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.osmand.DataExtraction.ExitListener;

public class MapPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	public interface IMapLocationListener {
		void locationChanged(double newLatitude, double newLongitude);
	}

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Tree of choose");
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	    frame.addWindowListener(new ExitListener());
	    Container content = frame.getContentPane();
	    
//	    MapPanel panel = new MapPanel(new ZipFile(Constants.pathToTestDataDir  + "MinskTiles.zipp"));
	    MapPanel panel = new MapPanel(new File(Constants.pathToTestDataDir  + "MinskTiles"));
	    
	    content.add(panel, BorderLayout.CENTER);
	    
	    frame.setSize(512, 512);
	    frame.setVisible(true);

	}
	
	private final int tileSize = 256;
	private BufferedImage[][] images;
	private int xStartingImage = 0;
	private int yStartingImage = 0;
	
	private final File fileWithTiles;
	private int zoom = 15;
	
	// degree measurements (-180, 180)
	// долгота
	private double longitude = 27.56;
	// широта
	// degree measurements (90, -90)
	private double latitude = 53.9;
	
	private List<IMapLocationListener> listeners = new ArrayList<IMapLocationListener>();
	
	private String map = "Mapnik";
	
	public MapPanel(File fileWithTiles) {
		this.fileWithTiles = fileWithTiles;
		initUI();
	}

	
	public double getXTile(){
		return NodeUtil.getTileNumberX(zoom, longitude);
	}
	
	public double getYTile(){
		return NodeUtil.getTileNumberY(zoom, latitude);
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		System.out.println("draw");
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				for (int j = 0; j < images[i].length; j++) {
					if(images[i][j] == null){
						if((i+j + (int)getXTile() + (int)getYTile()) % 2 == 0){
							g.setColor(Color.gray);
						} else {
							g.setColor(Color.white);
						}
						g.fillRect(i * tileSize+xStartingImage, j * tileSize + yStartingImage, tileSize, tileSize);
					} else {
						g.drawImage(images[i][j], i * tileSize+xStartingImage, j * tileSize + yStartingImage, this);
					}
				}
			}
		}
		g.setColor(Color.black);
		
		g.fillOval(getWidth()/2 - 2, getHeight()/2 -2, 4, 4);
		g.drawOval(getWidth()/2 - 2, getHeight()/2 -2, 4, 4);
		g.drawOval(getWidth()/2 - 5, getHeight()/2 -5, 10, 10);
	}
	
	public String getFile(int x, int y){
		return map +"/"+zoom+"/"+(x) +"/"+y+".png";
	}
	
	Map<String, BufferedImage> cache = new HashMap<String, BufferedImage>(); 
	public BufferedImage getImageFor(int x, int y) throws IOException{
		String file = getFile(x, y);
		if(!cache.containsKey(file)){
//			ZipEntry en = fileWithTiles.getEntry(file);
			File en = new File(fileWithTiles, file);
			if(cache.size() > 1000){
				ArrayList<String> list = new ArrayList<String>(cache.keySet());
				for(int i=0; i<list.size(); i+=2){
					cache.remove(list.get(i));
				}
			}
//			if(en != null){
			if(en.exists()){
//				cache.put(file, ImageIO.read(fileWithTiles.getInputStream(en)));
				long time = System.currentTimeMillis();
				cache.put(file, ImageIO.read(en));
				System.out.println("Loaded " + (System.currentTimeMillis() - time));
			} else {
				cache.put(file, null);
			}
		}
		
		return cache.get(file);
	}
	
	
	// TODO async loading images (show busy cursor while it is loaded)
	public void prepareImage(){
		try {
			
			if (images != null) {
				for (int i = 0; i < images.length; i++) {
					for (int j = 0; j < images[i].length; j++) {
						// dispose 
					}
				}
			}
			double xTile = getXTile();
			double yTile = getYTile();
			double leftX = (getSize().width/2d - (xTile - Math.floor(xTile)) *tileSize)/tileSize;
			double leftY = (getSize().height/2d - (yTile - Math.floor(yTile)) *tileSize)/tileSize;
			
			int xStartInd = (int) (Math.floor(xTile) - Math.ceil(leftX));
			int yStartInd = (int) (Math.floor(yTile) - Math.ceil(leftY));
			
			xStartingImage = (int) ((leftX - Math.ceil(leftX))*tileSize);
			yStartingImage = (int) ((leftY - Math.ceil(leftY))*tileSize);
			
			int tileXCount = (int) Math.ceil((getSize().width - xStartingImage)/ (double)tileSize );
			int tileYCount = (int) Math.ceil((getSize().height- yStartingImage)/ (double)tileSize );
			images = new BufferedImage[tileXCount][tileYCount];
			for(int i=0; i<images.length; i++){
				for(int j=0; j<images[i].length; j++){
					images[i][j]= getImageFor(xStartInd + i, yStartInd + j);
				}
			}
			repaint();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private void initUI() {
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
	
	public String getMap(){
		return map;
	}
	
	public void setMapName(String map){
		this.map = map;
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
			l.locationChanged(latitude, longitude);
		}
	}
	
	
	@Override
	protected void processKeyEvent(KeyEvent e) {
		boolean processed = false;
		if (e.getID() == KeyEvent.KEY_RELEASED) {

			if (e.getKeyCode() == 37) {
				longitude = NodeUtil.getLongitudeFromTile(zoom, getXTile()-0.5); 
				processed = true;
			} else if (e.getKeyCode() == 39) {
				longitude = NodeUtil.getLongitudeFromTile(zoom, getXTile()+0.5);
				processed = true;
			} else if (e.getKeyCode() == 38) {
				latitude = NodeUtil.getLatitudeFromTile(zoom, getYTile()-0.5);
				processed = true;
			} else if (e.getKeyCode() == 40) {
				latitude = NodeUtil.getLatitudeFromTile(zoom, getYTile()+0.5);
				processed = true;
			}
		}
		if(e.getID() == KeyEvent.KEY_TYPED){
			if(e.getKeyChar() == '+'){
				zoom ++;
				processed = true;
			} else if(e.getKeyChar() == '-'){
				zoom --;
				processed = true;
			}
		}
		
		if(processed){
			e.consume();
			prepareImage();
			fireMapLocationListeners();
		}
		super.processKeyEvent(e);
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
			double dx = (startDragging.x - (double)p.x)/tileSize; 
			double dy = (startDragging.y - (double)p.y)/tileSize;
			double lat = NodeUtil.getLatitudeFromTile(zoom, getYTile() + dy);
			double lon = NodeUtil.getLongitudeFromTile(zoom, getXTile() + dx);
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
