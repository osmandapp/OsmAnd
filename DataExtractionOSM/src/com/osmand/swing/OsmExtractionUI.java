package com.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.DefaultMutableTreeNode;

import com.osmand.DefaultLauncherConstants;
import com.osmand.IMapLocationListener;
import com.osmand.data.Amenity;
import com.osmand.data.City;
import com.osmand.data.DataTileManager;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.City.CityType;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class OsmExtractionUI implements IMapLocationListener {
	protected City selectedCity;
	
	private MapPanel mapPanel = new MapPanel(new File(DefaultLauncherConstants.pathToDirWithTiles));
	
	private DefaultMutableTreeNode amenitiesTree;
	private JTree treePlaces;

	private final Region r; 
	
	public OsmExtractionUI(final Region r){
		this.r = r;
	}
	
	
	public void runUI(){
		JFrame frame = new JFrame("Tree of choose");
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		DefaultMutableTreeNode root = new DataExtractionTreeNode(r.getName(), r);
		amenitiesTree = new DataExtractionTreeNode("Amenities", r);
		amenitiesTree.add(new DataExtractionTreeNode("closest", r));
		root.add(amenitiesTree);
		for(CityType t : CityType.values()){
			DefaultMutableTreeNode cityTree = new DataExtractionTreeNode(t.toString(), t);
			root.add(cityTree);
			for(City ct : r.getCitiesByType(t)){
				DefaultMutableTreeNode cityNodeTree = new DataExtractionTreeNode(ct.getName(), ct);
				cityTree.add(cityNodeTree);
				
				for(Street str : ct.getStreets()){
					DefaultMutableTreeNode strTree = new DataExtractionTreeNode(str.getName(), str);
					cityNodeTree.add(strTree);
					for(Entity e : str.getBuildings()){
						DefaultMutableTreeNode building = new DataExtractionTreeNode(e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER), e);
						strTree.add(building);
						
					}
				}
			}
		}
		
		
		
	    frame.addWindowListener(new ExitListener());
	    Container content = frame.getContentPane();
	    frame.setFocusable(true);
	    
	    
	    treePlaces = new JTree(root);
	    final JList jList = new JList();
	    jList.setCellRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = 4661949460526837891L;

			@Override
	    	public Component getListCellRendererComponent(JList list,
	    			Object value, int index, boolean isSelected,
	    			boolean cellHasFocus) {
	    		super.getListCellRendererComponent(list, value, index, isSelected,
	    				cellHasFocus);
	    		if(value instanceof City){
	    			setText(((City)value).getName());
	    		}
	    		return this;
	    	}
	    });
	    
	    JSplitPane panelForTreeAndImage = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(treePlaces), mapPanel);
	    panelForTreeAndImage.setResizeWeight(0.2);
	    mapPanel.setFocusable(true);
	    mapPanel.addMapLocationListener(this);
	    
	    
	    DataTileManager<LatLon> amenitiesManager = new DataTileManager<LatLon>();
	    for(Amenity a : r.getAmenityManager().getAllObjects()){
	    	amenitiesManager.registerObject(a.getNode().getLatitude(), a.getNode().getLongitude(), a.getNode().getLatLon());
	    }
	    mapPanel.setPoints(amenitiesManager);
	    
	    
	    JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(jList), panelForTreeAndImage);
	    pane.setResizeWeight(0.2);
	    content.add(pane, BorderLayout.CENTER);
	    
	    final JLabel label = new JLabel();
	    content.add(label, BorderLayout.SOUTH);

	    JPanel panel = new JPanel(new BorderLayout());
	    final JTextField textField = new JTextField();
	    final JButton button = new JButton();
	    button.setText("Set town");

	    
	    panel.add(textField, BorderLayout.CENTER);
	    panel.add(button, BorderLayout.WEST);
	    
	    content.add(panel, BorderLayout.NORTH);
	    
	    
	    updateListCities(r, textField.getText(), jList);
	    textField.getDocument().addUndoableEditListener(new UndoableEditListener(){
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
	    		updateListCities(r, textField.getText(), jList);
			}
	    });
	    
	    button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedCity = (City)jList.getSelectedValue();
			}
	    });

	    jList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(jList.getSelectedValue() != null){
					Node node = ((City)jList.getSelectedValue()).getNode();
					String text = "Lat : " + node.getLatitude() + " Lon " + node.getLongitude();
					if(selectedCity != null){
						text += " distance " + MapUtils.getDistance(selectedCity.getNode(), node);
					}
					label.setText(text);
					mapPanel.setLatLon(node.getLatitude(), node.getLongitude());
				} else {
					String text = selectedCity == null ? "" : selectedCity.getName();
					label.setText(text);
				}
				
			}
	    	
	    });

	    treePlaces.addTreeSelectionListener(new TreeSelectionListener(){
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getPath() != null) {
 					if (e.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode) {
						Object o = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();

						if (o instanceof City) {
							City c = (City) o;
							mapPanel.setLatLon(c.getNode().getLatitude(), c.getNode().getLongitude());
							mapPanel.requestFocus();
						}

						if (o instanceof Entity) {
							Entity c = (Entity) o;
							if (c instanceof Node) {
								mapPanel.setLatLon(((Node) c).getLatitude(), ((Node) c).getLongitude());
//								mapPanel.requestFocus();
							} else {
								DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.getPath().getPathComponent(
										e.getPath().getPathCount() - 2);
								if (n.getUserObject() instanceof Street) {
									Street str = (Street) n.getUserObject();
									LatLon l = str.getLocationBuilding(c);
									mapPanel.setLatLon(l.getLatitude(), l.getLongitude());
									mapPanel.requestFocus();
								}
							}
						}
					}
				}
				
			}
	    });
	    
	    MenuBar bar = new MenuBar();
	    bar.add(MapPanel.getMenuToChooseSource(mapPanel));
	    frame.setMenuBar(bar);
	    frame.setSize(1024, 768);
	    frame.setVisible(true);
	}
	
	@Override
	public void locationChanged(final double newLatitude, final double newLongitude, Object source){
		Region reg = (Region) amenitiesTree.getUserObject();
		List<Amenity> closestAmenities = reg.getClosestAmenities(newLatitude, newLongitude);
		Collections.sort(closestAmenities, new Comparator<Amenity>(){
			@Override
			public int compare(Amenity o1, Amenity o2) {
				return Double.compare(MapUtils.getDistance(o1.getNode(), newLatitude, newLongitude), 
						MapUtils.getDistance(o2.getNode(), newLatitude, newLongitude));
			}
			
		});
		
		Map<String, List<Amenity>> filter = new TreeMap<String, List<Amenity>>();
		for(Amenity n : closestAmenities){
			String type = n.getType().toString();
			if(!filter.containsKey(type)){
				filter.put(type, new ArrayList<Amenity>());
			}
			filter.get(type).add(n);
		}
		for(int i=1; i< amenitiesTree.getChildCount(); ){
			if(!filter.containsKey(((DefaultMutableTreeNode)amenitiesTree.getChildAt(i)).getUserObject())){
				amenitiesTree.remove(i);
			} else {
				i++;
			}
		}
		
		((DefaultMutableTreeNode)amenitiesTree.getChildAt(0)).removeAllChildren();
		
		
		for(int i=0; i<15 && i < closestAmenities.size(); i++){
			Amenity n = closestAmenities.get(i);
			int dist = (int) (MapUtils.getDistance(n.getNode(), newLatitude, newLongitude));
			String str = n.getSimpleFormat() + " [" +dist+" m ]";
			((DefaultMutableTreeNode)amenitiesTree.getChildAt(0)).add(
					new DataExtractionTreeNode(str, n));
		}
		
		for(String s : filter.keySet()){
			DefaultMutableTreeNode p = null;
			for(int i=0; i< amenitiesTree.getChildCount(); i++){
				if(s.equals(((DefaultMutableTreeNode)amenitiesTree.getChildAt(i)).getUserObject())){
					p = ((DefaultMutableTreeNode)amenitiesTree.getChildAt(i));
					break;
				}
			}
			if (p == null) {
				p = new DefaultMutableTreeNode(s);
			}
			
			p.removeAllChildren();
			for (Amenity n : filter.get(s)) {
				int dist = (int) (MapUtils.getDistance(n.getNode(), newLatitude, newLongitude));
				String str = n.getSimpleFormat() + " [" + dist + " m ]";
				DataExtractionTreeNode node = new DataExtractionTreeNode(str, n);
				p.add(node);
			}
			amenitiesTree.add(p);
		}
		treePlaces.updateUI();
	}
	
	public void updateListCities(Region r, String text, JList jList){
		Collection<City> city = r.getSuggestedCities(text, 100);
		City[] names = new City[city.size()];
		int i=0;
		for(City c : city){
			names[i++] = c;
		}
		jList.setListData(names);
	}
	
	
	public static class DataExtractionTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;
		private String name;

		public DataExtractionTreeNode(String name, Object userObject){
			super(userObject);
			this.name = name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
		
	}
	public static class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent event) {
			System.exit(0);
		}
	}

}
