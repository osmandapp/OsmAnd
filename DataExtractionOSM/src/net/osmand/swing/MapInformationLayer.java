package net.osmand.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

public class MapInformationLayer implements MapPanelLayer {

	private MapPanel map;
	
	private JLabel gpsLocation;
	private JButton areaButton;
	

	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(final MapPanel map) {
		this.map = map;
		BoxLayout layout = new BoxLayout(map, BoxLayout.LINE_AXIS);
		map.setLayout(layout);
		map.setBorder(BorderFactory.createEmptyBorder(2, 10, 10, 10));
		
		gpsLocation = new JLabel();
		gpsLocation.setOpaque(false);
		updateLocationLabel();
		
		JButton zoomIn = new JButton("+"); //$NON-NLS-1$
		JButton zoomOut = new JButton("-"); //$NON-NLS-1$
		areaButton = new JButton();
		areaButton.setAction(new AbstractAction(Messages.getString("MapInformationLayer.PRELOAD.AREA")){ //$NON-NLS-1$
			private static final long serialVersionUID = -5512220294374994021L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new TileBundleDownloadDialog(map, map).showDialog();
			}
			
		});
		zoomIn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				map.setZoom(map.getZoom() + 1);
			}
		});
		zoomOut.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				map.setZoom(map.getZoom() - 1);
			}
		});
		
		map.add(gpsLocation);
		map.add(Box.createHorizontalGlue());
		map.add(areaButton);
		map.add(zoomIn);
		map.add(zoomOut);
		gpsLocation.setAlignmentY(Component.TOP_ALIGNMENT);
		areaButton.setVisible(false);
		areaButton.setAlignmentY(Component.TOP_ALIGNMENT);
		zoomOut.setAlignmentY(Component.TOP_ALIGNMENT);
		zoomIn.setAlignmentY(Component.TOP_ALIGNMENT);

	}
	
	public void setAreaButtonVisible(boolean b){
		areaButton.setVisible(b);
	}
	
	public void setAreaActionHandler(Action a){
		areaButton.setAction(a);
	}
	
	private void updateLocationLabel(){
		double latitude = map.getLatitude();
		double longitude = map.getLongitude();
		int zoom = map.getZoom();
		gpsLocation.setText(MessageFormat.format("Lat : {0,number,#.####}, lon : {1,number,#.####}, zoom : {2}", latitude, longitude, zoom)); //$NON-NLS-1$
	}
	@Override
	public void prepareToDraw() {
		updateLocationLabel();
		
	}

	@Override
	public void paintLayer(Graphics g) {
		g.setColor(Color.black);
		g.fillOval((int)map.getCenterPointX() - 2,(int) map.getCenterPointY() - 2, 4, 4);
		g.drawOval((int)map.getCenterPointX() - 2,(int) map.getCenterPointY() - 2, 4, 4);
		g.drawOval((int)map.getCenterPointX() - 5,(int) map.getCenterPointY()- 5, 10, 10);
		
	}
	

}
