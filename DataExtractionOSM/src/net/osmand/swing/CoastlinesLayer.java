package net.osmand.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import net.osmand.data.preparation.BasemapProcessor;
import net.osmand.data.preparation.MapZooms;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;


public class CoastlinesLayer implements MapPanelLayer {

	private MapPanel map;
	
	private BasemapProcessor basemapProcessor;

	private ArrayList<Polygon> lands = new ArrayList<Polygon>();
	private ArrayList<Polygon> waters = new ArrayList<Polygon>();
	
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(MapPanel map) {
		this.map = map;
		basemapProcessor = new BasemapProcessor(null, MapZooms.getDefault(), MapRenderingTypes.getDefault(), 0);
		fillPopupMenuWithActions(map.getPopupMenu());
	}
	

	@Override
	public void paintLayer(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		
		g.setColor(new Color(0, 0, 255, 120));
		for(Polygon w : waters) {
			g.drawPolygon(w);
			g.fillPolygon(w);
		}
		g.setColor(new Color(0, 255, 0, 120));
		for(Polygon l : lands) {
			g.drawPolygon(l);
			g.fillPolygon(l);
		}
	}
	
	public Polygon getPolygon(int tileX, int tileY, int z) {
		int pixX = (int) (MapUtils.getPixelShiftX(map.getZoom(), MapUtils.getLongitudeFromTile(z, tileX), map.getLongitude(),
				map.getTileSize()) + map.getCenterPointX());
		int pixY = (int) (MapUtils.getPixelShiftY(map.getZoom(), MapUtils.getLatitudeFromTile(z, tileY), map.getLatitude(),
				map.getTileSize()) + map.getCenterPointY());
		int pixsX = (int) (MapUtils.getPixelShiftX(map.getZoom(), MapUtils.getLongitudeFromTile(z, tileX + 1), map.getLongitude(),
				map.getTileSize()) + map.getCenterPointX());
		int pixsY = (int) (MapUtils.getPixelShiftY(map.getZoom(), MapUtils.getLatitudeFromTile(z, tileY + 1), map.getLatitude(),
				map.getTileSize()) + map.getCenterPointY());
		return new Polygon(new int[] { pixX, pixsX, pixsX, pixX, pixX }, new int[] { pixY, pixY, pixsY, pixsY, pixY }, 5);
	}
	
	public void fillPopupMenuWithActions(JPopupMenu menu) {
		Action print = new AbstractAction("Print tile coastline info") {
			private static final long serialVersionUID = 507156107455281238L;

			@Override
			public void actionPerformed(ActionEvent e) {
				Point popupMenuPoint = map.getPopupMenuPoint();
				double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
				double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
				int yTile = (int) (map.getYTile() + fy);
				int xTile = (int) (map.getXTile() + fx);
				System.out.println("!Tile x=" + xTile + " y=" + yTile + " z=" + map.getZoom());
				if (basemapProcessor.isWaterTile(xTile, yTile, map.getZoom())) {
					System.out.println("!Water Tile x=" + xTile + " y=" + yTile + " z=" + map.getZoom());
				}
				if (basemapProcessor.isLandTile(xTile, yTile, map.getZoom())) {
					System.out.println("!Land  Tile x=" + xTile + " y=" + yTile + " z=" + map.getZoom());
				}
				for (int z = map.getZoom(); z <= basemapProcessor.getTileZoomLevel(); z++) {
					System.out.println("Zoom " + z);
					for (int x = xTile * 1 << (z - map.getZoom()); x < (xTile + 1) * (1 << (z - map.getZoom())); x++) {
						for (int y = yTile * 1 << (z - map.getZoom()); y < (yTile + 1) * (1 << (z - map.getZoom())); y++) {
							if (basemapProcessor.isWaterTile(x, y, z)) {
								System.out.println("Water tile x=" + x + " y=" + y + " z=" + z);
							}
							if (basemapProcessor.isLandTile(x, y, z)) {
								System.out.println("Land  tile x=" + x + " y=" + y + " z=" + z);
							}
						}
					}
				}
			}
		};
		menu.add(print);
	}

	@Override
	public void prepareToDraw() {
		double xTileLeft = map.getXTile() - map.getCenterPointX() / map.getTileSize();
		double xTileRight = map.getXTile() + map.getCenterPointX() / map.getTileSize();
		double yTileUp = map.getYTile() - map.getCenterPointY() / map.getTileSize();
		double yTileDown = map.getYTile() + map.getCenterPointY() / map.getTileSize();

//		double latDown = MapUtils.getLatitudeFromTile(map.getZoom(), yTileDown);
//		double longDown = MapUtils.getLongitudeFromTile(map.getZoom(), xTileRight);
//		double latUp = MapUtils.getLatitudeFromTile(map.getZoom(), yTileUp);
//		double longUp = MapUtils.getLongitudeFromTile(map.getZoom(), xTileLeft);
		waters.clear();
		lands.clear();
		if(map.getZoom() > 5) {
			for (int k = 0; k < 5; k++) {
				int tLeft = (int) (Math.floor(xTileLeft) * (1 << k));
				int tRight = (int) (Math.ceil(xTileRight) * (1 << k));
				int tTop = (int) (Math.floor(yTileUp) * (1 << k));
				int tBottom = (int) (Math.ceil(yTileDown) * (1 << k));
				int z = map.getZoom() + k;
				for (int i = tLeft; i <= tRight; i++) {
					for (int j = tTop; j <= tBottom; j++) {
						if (basemapProcessor.isWaterTile(i, j, z)) {
							if(k == 0 || !basemapProcessor.isWaterTile(i/2, j/2, z-1)) {
								waters.add(getPolygon(i, j, z));
							}
						}
						if (basemapProcessor.isLandTile(i, j, z)) {
							if(k == 0 || !basemapProcessor.isLandTile(i/2, j/2, z-1)) {
								lands.add(getPolygon(i, j, z));
							}
						}
					}
				}
			}
			
		}
	}
	
}
