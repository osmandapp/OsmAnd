package net.osmand.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.osmand.data.DataTileManager;
import net.osmand.osm.Entity;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.Way;


public class MapPointsLayer implements MapPanelLayer {

	private MapPanel map;
	
	// special points to draw
	private DataTileManager<? extends Entity> points;
	
	
	private Color color = Color.black;
	private int size = 3;
	private String tagToShow = null;
	
	private Map<Point, String> pointsToDraw = new LinkedHashMap<Point, String>();
	private List<LineObject> linesToDraw = new ArrayList<LineObject>();

	private Font whiteFont;
	
	private static class LineObject {
		Way w;
		Line2D line;
		boolean middle;
		public LineObject(Way w, Line2D line, boolean middle) {
			super();
			this.w = w;
			this.line = line;
			this.middle = middle;
		}
		
	}
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(MapPanel map) {
		this.map = map;
	}
	
	public void setColor(Color color){
		this.color = color;
	}
	
	public void setPointSize(int size){
		this.size  = size;
	}
	
	public void setTagToShow(String tag) {
		this.tagToShow = tag;
	}
	
	

	@Override
	public void paintLayer(Graphics2D g) {
		g.setColor(color);
		if (whiteFont == null) {
			whiteFont = g.getFont().deriveFont(15).deriveFont(Font.BOLD);
		}
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		// draw user points
		for (Point p : pointsToDraw.keySet()) {
			g.drawOval(p.x, p.y, size, size);
			g.fillOval(p.x, p.y, size, size);
			if(tagToShow != null && pointsToDraw.get(p) != null){
				g.drawString(pointsToDraw.get(p), p.x, p.y);
			}
		}
		
		// draw user points
		int[] xPoints = new int[4];
		int[] yPoints = new int[4];
		for (LineObject e : linesToDraw) {
			Line2D p = e.line;
			Way w = e.w;
			g.setColor(color);
			String name = null;
			boolean white = false;
			if(w != null) {
				if (e.middle) {
					name = w.getTag("name");
				}
				white = "white".equalsIgnoreCase(w.getTag("color"));
				if(white){
					g.setColor(Color.gray);
				}
			}
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
			if(name != null && map.getZoom() >= 16) {
				Font prevFont = g.getFont();
				Color prevColor = g.getColor();
				AffineTransform prev = g.getTransform();

				double flt = Math.atan2(p.getX2() - p.getX1(), p.getY2() - p.getY1());
				
				AffineTransform ps = new AffineTransform(prev);
				ps.translate((p.getX2() + p.getX1()) / 2, (int)(p.getY2() + p.getY1()) / 2);
				if(flt < Math.PI && flt > 0) {
					ps.rotate(p.getX2() - p.getX1(), p.getY2() - p.getY1());
				} else {
					ps.rotate(-(p.getX2() - p.getX1()), -(p.getY2() - p.getY1()));
				}
				g.setTransform(ps);
				
				g.setFont(whiteFont);
				g.setColor(Color.white);
				float c = 1.3f;
				g.scale(c, c);
				g.drawString(name, (int)(-15), (int) (-10/c));
				g.scale(1/c, 1/c);
				
				if(white) {
					g.setColor(Color.lightGray);
				} else {
					g.setColor(Color.DARK_GRAY);
				}
				g.drawString(name, -15, -10);
				
				g.setColor(prevColor);
				g.setTransform(prev);
				g.setFont(prevFont);
			}
		}		
	}

	@Override
	public void prepareToDraw() {
		if (points != null) {
			double xTileLeft = map.getXTile() - map.getCenterPointX() / map.getTileSize();
			double xTileRight = map.getXTile() + map.getCenterPointX() / map.getTileSize();
			double yTileUp = map.getYTile() - map.getCenterPointY() / map.getTileSize();
			double yTileDown = map.getYTile() + map.getCenterPointY() / map.getTileSize();
			
			double latDown = MapUtils.getLatitudeFromTile(map.getZoom(), yTileDown);
			double longDown = MapUtils.getLongitudeFromTile(map.getZoom(), xTileRight);
			double latUp = MapUtils.getLatitudeFromTile(map.getZoom(), yTileUp);
			double longUp = MapUtils.getLongitudeFromTile(map.getZoom(), xTileLeft);
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
							int pixX = (int) (MapUtils.getPixelShiftX(map.getZoom(), n.getLongitude(), map.getLongitude(), map.getTileSize()) + map.getCenterPointX());
							int pixY = (int) (MapUtils.getPixelShiftY(map.getZoom(), n.getLatitude(), map.getLatitude(), map.getTileSize()) + map.getCenterPointY());
							if (i > 0) {
								linesToDraw.add(new LineObject((Way) e, new Line2D.Float(pixX, pixY, prevPixX, prevPixY), i == nodes.size() / 2));
							}
							prevPixX = pixX;
							prevPixY = pixY;
						}
					}
					
				} else if(e instanceof Node){
					Node n = (Node) e;
					int pixX = (int) (MapUtils.getPixelShiftX(map.getZoom(), n.getLongitude(), map.getLongitude(), map.getTileSize()) + map.getCenterPointX());
					int pixY = (int) (MapUtils.getPixelShiftY(map.getZoom(), n.getLatitude(), map.getLatitude(), map.getTileSize()) + map.getCenterPointY());
					if (pixX >= 0 && pixY >= 0) {
						pointsToDraw.put(new Point(pixX, pixY), n.getTag(tagToShow));
					}
				} else {
				} 
			}
		}		
	}
	
	public DataTileManager<? extends Entity> getPoints() {
		return points;
	}
	
	public void setPoints(DataTileManager<? extends Entity> points) {
		this.points = points;
	}

}
