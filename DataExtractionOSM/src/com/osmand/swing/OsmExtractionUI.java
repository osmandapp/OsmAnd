package com.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.xml.sax.SAXException;

import com.osmand.Algoritms;
import com.osmand.ExceptionHandler;
import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.DataTileManager;
import com.osmand.data.MapObject;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.TransportRoute;
import com.osmand.data.TransportStop;
import com.osmand.data.City.CityType;
import com.osmand.data.index.DataIndexWriter;
import com.osmand.data.preparation.DataExtraction;
import com.osmand.map.IMapLocationListener;
import com.osmand.map.ITileSource;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.Way;
import com.osmand.osm.io.IOsmStorageFilter;
import com.osmand.osm.io.OsmBoundsFilter;
import com.osmand.osm.io.OsmStorageWriter;
import com.osmand.swing.MapPanel.MapSelectionArea;

public class OsmExtractionUI implements IMapLocationListener {

	private static final Log log = LogFactory.getLog(OsmExtractionUI.class);
	public static final String LOG_PATH  = System.getProperty("user.home")+"/Application Data/Osmand/osmand.log";
	public static OsmExtractionUI MAIN_APP;
	
	public static void main(String[] args) {
		// first of all config log
		new File(LOG_PATH).getParentFile().mkdirs();
		final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				if(!(e instanceof ThreadDeath)){
					ExceptionHandler.handle("Error in thread " + t.getName(), e);
				}
				defaultHandler.uncaughtException(t, e);
			}
		});
		
        MAIN_APP = new OsmExtractionUI(null);
        MAIN_APP.frame.setBounds(DataExtractionSettings.getSettings().getWindowBounds());
        MAIN_APP.frame.setVisible(true);
	}
	
	protected City selectedCity;
	private MapPanel mapPanel;
	
	private DataExtractionTreeNode amenitiesTree;
	private JTree treePlaces;
	private JList searchList;
	private JTextField searchTextField;
	
	private JFrame frame;
	private JLabel statusBarLabel;
	
	private Region region;
	private JButton generateDataButton;
	private JCheckBox buildPoiIndex;
	private JCheckBox buildAddressIndex;
	private JCheckBox buildTransportIndex;
	private JCheckBox normalizingStreets;
	private TreeModelListener treeModelListener;
	private JCheckBox loadingAllData;
	
		
	
	
	public OsmExtractionUI(final Region r){
		this.region = r;
		createUI();
		setRegion(r, "Region");
	}

	
	public void setRegion(Region region, String name){
		if (this.region == region) {
			return;
		}
		this.region = region;
		DefaultMutableTreeNode root = new DataExtractionTreeNode(name, region);
		if (region != null) {
			amenitiesTree = new DataExtractionTreeNode("Amenities", region);
			amenitiesTree.add(new DataExtractionTreeNode("First 15", region));
			for (AmenityType type : AmenityType.values()) {
				amenitiesTree.add(new DataExtractionTreeNode(Algoritms.capitalizeFirstLetterAndLowercase(type.toString()), type));
			}
			root.add(amenitiesTree);
			
			DataExtractionTreeNode transport = new DataExtractionTreeNode("Transport", region);
			root.add(transport);
			for(String s : region.getTransportRoutes().keySet()){
				DataExtractionTreeNode trRoute = new DataExtractionTreeNode(s, s);
				transport.add(trRoute);
				List<TransportRoute> list = region.getTransportRoutes().get(s);
				for(TransportRoute r : list){
					DataExtractionTreeNode route = new DataExtractionTreeNode(r.getRef(), r);
					trRoute.add(route);
				}
				
			}

			for (CityType t : CityType.values()) {
				DefaultMutableTreeNode cityTree = new DataExtractionTreeNode(Algoritms.capitalizeFirstLetterAndLowercase(t.toString()), t);
				root.add(cityTree);
				for (City ct : region.getCitiesByType(t)) {
					DefaultMutableTreeNode cityNodeTree = new DataExtractionTreeNode(ct.getName(), ct);
					cityTree.add(cityNodeTree);

					for (Street str : ct.getStreets()) {
						DefaultMutableTreeNode strTree = new DataExtractionTreeNode(str.getName(), str);
						cityNodeTree.add(strTree);
						for (Building b : str.getBuildings()) {
							DefaultMutableTreeNode building = new DataExtractionTreeNode(b.getName(), b);
							strTree.add(building);
						}
					}
				}
			}
		}
		
	    if (searchList != null) {
			updateListCities(region, searchTextField.getText(), searchList);
		}
		mapPanel.repaint();
		DefaultTreeModel newModel = new DefaultTreeModel(root, false);
		newModel.addTreeModelListener(treeModelListener);
		treePlaces.setModel(newModel);
		
		updateButtonsBar();
		locationChanged(mapPanel.getLatitude(), mapPanel.getLongitude(), this);
	}
        
	
	
	public void createUI(){
		frame = new JFrame("OsmAnd Map Creator");
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			log.error("Can't set look and feel", e);
		}
		
		
	    frame.addWindowListener(new ExitListener());
	    Container content = frame.getContentPane();
	    frame.setFocusable(true);
	    
	    mapPanel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
	    mapPanel.setFocusable(true);
	    mapPanel.addMapLocationListener(this);
	    
	    statusBarLabel = new JLabel();
	    content.add(statusBarLabel, BorderLayout.SOUTH);
	    File workingDir = DataExtractionSettings.getSettings().getDefaultWorkingDir();
	    statusBarLabel.setText(workingDir == null ? "<working directory unspecified>" : "Working directory : " + workingDir.getAbsolutePath());
	    
	   
	    	    
	    JSplitPane panelForTreeAndMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(createTree(content)), mapPanel);
	    panelForTreeAndMap.setResizeWeight(0.2);
	    
	    
	    
	    
	    createButtonsBar(content);
//	    createCitySearchPanel(content);
	    if(searchList != null){
	    	JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(searchList), panelForTreeAndMap);
	    	pane.setResizeWeight(0.2);
	    	content.add(pane, BorderLayout.CENTER);
	    } else {
	    	content.add(panelForTreeAndMap, BorderLayout.CENTER);
	    }
	   
	    JMenuBar bar = new JMenuBar();
	    fillMenuWithActions(bar);
	    
	    JPopupMenu popupMenu = new JPopupMenu();
	    fillPopupMenuWithActions(popupMenu);
	    treePlaces.add(popupMenu);
	    treePlaces.addMouseListener(new PopupTrigger(popupMenu));
	    
	    
	    frame.setJMenuBar(bar);
	    
	    
	}
	
	public JTree createTree(Container content) {
		treePlaces = new JTree();
		treePlaces.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Region"), false));
		treePlaces.setEditable(true);
		treePlaces.setCellEditor(new RegionCellEditor(treePlaces, (DefaultTreeCellRenderer) treePlaces.getCellRenderer()));
		treePlaces.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getPath() != null) {
					if (e.getPath().getLastPathComponent() instanceof DataExtractionTreeNode) {
						Object o = ((DataExtractionTreeNode) e.getPath().getLastPathComponent()).getModelObject();

						if (o instanceof MapObject) {
							MapObject c = (MapObject) o;
							LatLon location = c.getLocation();
							if(location != null){
								if(o instanceof Street){
									DataTileManager<Way> ways = new DataTileManager<Way>();
									for(Way w : ((Street)o).getWayNodes()){
										LatLon l = w.getLatLon();
										ways.registerObject(l.getLatitude(), l.getLongitude(), w);
									}
									mapPanel.setPoints(ways);
									mapPanel.requestFocus();
								} 
								mapPanel.setLatLon(location.getLatitude(), location.getLongitude());
								mapPanel.requestFocus();
							}
							if(o instanceof TransportRoute){
								DataTileManager<Entity> ways = new DataTileManager<Entity>();
								for(Way w : ((TransportRoute)o).getWays()){
									LatLon l = w.getLatLon();
									ways.registerObject(l.getLatitude(), l.getLongitude(), w);
								}
								for(TransportStop w : ((TransportRoute)o).getBackwardStops()){
									LatLon l = w.getLocation();
									ways.registerObject(l.getLatitude(), l.getLongitude(), 
											new Node(l.getLatitude(), l.getLongitude(), w.getId()));
								}
								for(TransportStop w : ((TransportRoute)o).getForwardStops()){
									LatLon l = w.getLocation();
									ways.registerObject(l.getLatitude(), l.getLongitude(), 
											new Node(l.getLatitude(), l.getLongitude(), w.getId()));
								}
								mapPanel.setPoints(ways);
								mapPanel.requestFocus();
							} 
							
						} else if (o instanceof Entity) {
							Entity c = (Entity) o;
							LatLon latLon = c.getLatLon();
							if (latLon != null) {
								mapPanel.setLatLon(latLon.getLatitude(), latLon.getLongitude());
								mapPanel.requestFocus();
							} 
						}
					}
				}

			}
		});
		
		treeModelListener = new TreeModelListener() {
		    public void treeNodesChanged(TreeModelEvent e) {
				Object node = e.getTreePath().getLastPathComponent();
				if(e.getChildren() != null && e.getChildren().length > 0){
					node =e.getChildren()[0];
				}
				if (node instanceof DataExtractionTreeNode) {
					DataExtractionTreeNode n = ((DataExtractionTreeNode) node);
					if (n.getModelObject() instanceof MapObject) {
						MapObject r = (MapObject) n.getModelObject();
						String newName = n.getUserObject().toString();
						if (!r.getName().equals(newName)) {
							r.setName(n.getUserObject().toString());
						}
						if (r instanceof Street && !((Street) r).isRegisteredInCity()) {
							DefaultMutableTreeNode parent = ((DefaultMutableTreeNode) n.getParent());
							parent.remove(n);
							((DefaultTreeModel) treePlaces.getModel()).nodeStructureChanged(parent);
						}
					}
				}
			}
		    public void treeNodesInserted(TreeModelEvent e) {
		    }
		    public void treeNodesRemoved(TreeModelEvent e) {
		    }
		    public void treeStructureChanged(TreeModelEvent e) {
		    }
		};
		treePlaces.getModel().addTreeModelListener(treeModelListener);
		return treePlaces;
	}
	
	
	
	protected void updateButtonsBar() {
		generateDataButton.setEnabled(region != null);
		normalizingStreets.setVisible(region == null);
		loadingAllData.setVisible(region == null);
		if(region == null && !buildAddressIndex.isEnabled()){
			buildAddressIndex.setEnabled(true);
		}
		if(region == null && !buildPoiIndex.isEnabled()){
			buildPoiIndex.setEnabled(true);
		}
		if(region == null && !buildTransportIndex.isEnabled()){
			buildTransportIndex.setEnabled(true);
		}
	}
	
	public void createButtonsBar(Container content){
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		content.add(panel, BorderLayout.NORTH);
		
		generateDataButton = new JButton();
		generateDataButton.setText("Generate data");
		generateDataButton.setToolTipText("Data with selected preferences will be generated in working directory." +
				" 	The index files will be named as region in tree. All existing data will be overwritten.");
		panel.add(generateDataButton);
		generateDataButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				generateData();
			}
		});
		
		buildPoiIndex = new JCheckBox();
		buildPoiIndex.setText("Build POI index");
		panel.add(buildPoiIndex);
		buildPoiIndex.setSelected(true);
		
		buildAddressIndex = new JCheckBox();
		buildAddressIndex.setText("Build Address index");
		panel.add(buildAddressIndex);
		buildAddressIndex.setSelected(true);
		
		normalizingStreets = new JCheckBox();
		normalizingStreets.setText("Normalizing streets");
		panel.add(normalizingStreets);
		normalizingStreets.setSelected(true);
		
		buildTransportIndex = new JCheckBox();
		buildTransportIndex.setText("Build transport index");
		panel.add(buildTransportIndex);
		buildTransportIndex.setSelected(true);

		loadingAllData = new JCheckBox();
		loadingAllData.setText("Loading all osm data");
		panel.add(loadingAllData);
		loadingAllData.setSelected(false);
		
		updateButtonsBar();
	}
	
	protected void generateData() {
		try {
    		final ProgressDialog dlg = new ProgressDialog(frame, "Generating data");
    		dlg.setRunnable(new Runnable(){

				@Override
				public void run() {
					dlg.startTask("Generating indices...", -1);
					DataIndexWriter builder = new DataIndexWriter(DataExtractionSettings.getSettings().getDefaultWorkingDir(), region);
					StringBuilder msg = new StringBuilder();
					try {
						msg.append("Indices for ").append(region.getName());
						if(buildPoiIndex.isSelected()){
							dlg.startTask("Generating POI index...", -1);
							builder.writePOI();
							msg.append(", POI index ").append("successfully created");
						}
						if(buildAddressIndex.isSelected()){
							dlg.startTask("Generating address index...", -1);
							builder.writeAddress();
							msg.append(", address index ").append("successfully created");
						}
						if(buildTransportIndex.isSelected()){
							dlg.startTask("Generating transport index...", -1);
							builder.writeTransport();
							msg.append(", transport index ").append("successfully created");
						}
						
//						new DataIndexReader().testIndex(new File(
//								DataExtractionSettings.getSettings().getDefaultWorkingDir(), 
//								IndexConstants.ADDRESS_INDEX_DIR+region.getName()+IndexConstants.ADDRESS_INDEX_EXT));
						msg.append(".");
					    JOptionPane pane = new JOptionPane(msg);
					    JDialog dialog = pane.createDialog(frame, "Generation data");
					    dialog.setVisible(true);
					} catch (SQLException e1) {
						throw new IllegalArgumentException(e1);
					} catch (IOException e1) {
						throw new IllegalArgumentException(e1);
					}
				}
    		});
    		dlg.run();
		} catch (InterruptedException e1) {
			log.error("Interrupted", e1); 
		} catch (InvocationTargetException e1) {
			ExceptionHandler.handle((Exception) e1.getCause());
		}
		
	}
	
	
	public void fillPopupMenuWithActions(JPopupMenu menu) {
		Action delete = new AbstractAction("Delete") {
			private static final long serialVersionUID = 7476603434847164396L;

			public void actionPerformed(ActionEvent e) {
				TreePath[] p = treePlaces.getSelectionPaths();
				if(p != null && 
						JOptionPane.OK_OPTION == 
							JOptionPane.showConfirmDialog(frame, "Are you sure about deleting " +p.length + " resources ? ")){
				for(TreePath path : treePlaces.getSelectionPaths()){
					Object node = path.getLastPathComponent();
					if (node instanceof DataExtractionTreeNode) {
						DataExtractionTreeNode n = ((DataExtractionTreeNode) node);
						if(n.getParent() instanceof DataExtractionTreeNode){
							DataExtractionTreeNode parent = ((DataExtractionTreeNode) n.getParent());
							boolean remove = false;
							if (n.getModelObject() instanceof Street) {
								((City)parent.getModelObject()).unregisterStreet(((Street)n.getModelObject()).getName());
								remove = true;
							} else if (n.getModelObject() instanceof Building) {
								((Street)parent.getModelObject()).getBuildings().remove(n.getModelObject());
								remove = true;
							} else if (n.getModelObject() instanceof City) {
								Region r = (Region) ((DataExtractionTreeNode)parent.getParent()).getModelObject();
								r.unregisterCity((City) n.getModelObject());
								remove = true;
							} else if (n.getModelObject() instanceof Amenity) {
								Region r = (Region) ((DataExtractionTreeNode)parent.getParent().getParent()).getModelObject();
								Amenity am = (Amenity) n.getModelObject();
								r.getAmenityManager().unregisterObject(am.getLocation().getLatitude(), am.getLocation().getLongitude(), am);
								remove = true;
							}
							if(remove){
								parent.remove(n);
								((DefaultTreeModel) treePlaces.getModel()).nodeStructureChanged(parent);
							}
						}
					}
				}
				
				}
			}
		};
		menu.add(delete);
		Action rename= new AbstractAction("Rename") {
			private static final long serialVersionUID = -8257594433235073767L;

			public void actionPerformed(ActionEvent e) {
				TreePath path = treePlaces.getSelectionPath();
				if(path != null){
					treePlaces.startEditingAtPath(path);
				}
			}
		};
		menu.add(rename);

	}
	
	public void fillMenuWithActions(final JMenuBar bar){
		JMenu menu = new JMenu("File");
		bar.add(menu);
		JMenuItem loadFile = new JMenuItem("Load osm file...");
		menu.add(loadFile);
		JMenuItem loadSpecifiedAreaFile = new JMenuItem("Load osm file for specifed area...");
		menu.add(loadSpecifiedAreaFile);
		JMenuItem closeCurrentFile = new JMenuItem("Close current file");
		menu.add(closeCurrentFile);
		menu.addSeparator();
		JMenuItem saveOsmFile = new JMenuItem("Save data to osm file...");
		menu.add(saveOsmFile);
		JMenuItem specifyWorkingDir = new JMenuItem("Specify working directory...");
		menu.add(specifyWorkingDir);
		menu.addSeparator();
		JMenuItem exitMenu= new JMenuItem("Exit");
		menu.add(exitMenu);
		
		JMenu tileSource = MapPanel.getMenuToChooseSource(mapPanel);
		final JMenuItem sqliteDB = new JMenuItem("Create sqlite database");
		tileSource.addSeparator();
		tileSource.add(sqliteDB);
		bar.add(tileSource);
		
		menu = new JMenu("Window");
		bar.add(menu);
		JMenuItem settings = new JMenuItem("Settings...");
		menu.add(settings);
		menu.addSeparator();
		JMenuItem openLogFile = new JMenuItem("Open log file...");
		menu.add(openLogFile);
		

		openLogFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = new File(OsmExtractionUI.LOG_PATH);
				if (file != null && file.exists()) {
					if (System.getProperty("os.name").startsWith("Windows")) {
						try {
							Runtime.getRuntime().exec(new String[] { "notepad.exe", file.getAbsolutePath() }); //$NON-NLS-1$
						} catch (IOException es) {
							ExceptionHandler.handle("Failed to open log file ", es);
						}
					} else {
						JOptionPane.showMessageDialog(frame, "Open log file manually " + LOG_PATH);
					}

				} else {
					ExceptionHandler.handle("Log file is not found");
				}
			}
		});
		
		sqliteDB.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				final String regionName = region == null ? "Region" : region.getName();
				final ITileSource map = mapPanel.getMap();
				if(map != null){
					try {
			    		final ProgressDialog dlg = new ProgressDialog(frame, "Creating index");
			    		dlg.setRunnable(new Runnable(){

							@Override
							public void run() {
								try {
									SQLiteBigPlanetIndex.createSQLiteDatabase(DataExtractionSettings.getSettings().getTilesDirectory(), regionName, map);
								} catch (SQLException e1) {
									throw new IllegalArgumentException(e1);
								} catch (IOException e1) {
									throw new IllegalArgumentException(e1);
								}
							}
			    		});
			    		dlg.run();
					} catch (InterruptedException e1) {
						log.error("Interrupted", e1); 
					} catch (InvocationTargetException e1) {
						ExceptionHandler.handle("Can't create big planet sqlite index", (Exception) e1.getCause());
					}
					
					
				}
			}
		});

		
		exitMenu.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
			}
		});
		closeCurrentFile.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				setRegion(null, "Region");
				frame.setTitle("OsmAnd Map Creator");
			}
		});
		settings.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				OsmExtractionPreferencesDialog dlg = new OsmExtractionPreferencesDialog(frame);
				dlg.showDialog();
			}
			
		});
		specifyWorkingDir.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
		        fc.setDialogTitle("Choose working directory");
		        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		        File workingDir = DataExtractionSettings.getSettings().getDefaultWorkingDir();
		        if(workingDir != null){
		        	fc.setCurrentDirectory(workingDir);
		        }
		        if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null && 
		        		fc.getSelectedFile().isDirectory()){
		        	DataExtractionSettings.getSettings().saveDefaultWorkingDir(fc.getSelectedFile());
		        	mapPanel.setTilesLocation(DataExtractionSettings.getSettings().getTilesDirectory());
		        	statusBarLabel.setText("Working directory : " + fc.getSelectedFile().getAbsolutePath());
		        	JMenu tileSource = MapPanel.getMenuToChooseSource(mapPanel);
		    		tileSource.add(sqliteDB);
		    		bar.remove(1);
		    		bar.add(tileSource, 1);
		        	updateButtonsBar();
		        }
			}
			
		});
		loadSpecifiedAreaFile.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getOsmFileChooser();
		        int answer = fc.showOpenDialog(frame) ;
		        if (answer == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null){
		        	final JDialog dlg = new JDialog(frame, true);
		        	dlg.setTitle("Select area to filter");
		        	MapPanel panel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
		        	panel.setLatLon(mapPanel.getLatitude(), mapPanel.getLongitude());
		        	panel.setZoom(mapPanel.getZoom());
		        	final StringBuilder res = new StringBuilder();
		        	panel.getLayer(MapInformationLayer.class).setAreaActionHandler(new AbstractAction("Select area"){
						private static final long serialVersionUID = -3452957517341961969L;
						@Override
						public void actionPerformed(ActionEvent e) {
							res.append(true);
							dlg.setVisible(false);
						}
		        		
		        	});
		        	dlg.add(panel);
		        	
		        	
		        	JMenuBar bar = new JMenuBar();
		    	    bar.add(MapPanel.getMenuToChooseSource(panel));
		    	    dlg.setJMenuBar(bar);
		    	    dlg.setSize(512, 512);
		    	    double x = frame.getBounds().getCenterX();
		    	    double y = frame.getBounds().getCenterY();
		    	    dlg.setLocation((int) x - dlg.getWidth() / 2, (int) y - dlg.getHeight() / 2);
		        	
		    	    dlg.setVisible(true);
		    		if(res.length() > 0 && panel.getSelectionArea().isVisible()){
		    			MapSelectionArea area = panel.getSelectionArea();
		    			IOsmStorageFilter filter = new OsmBoundsFilter(area.getLat1(), area.getLon1(), area.getLat2(), area.getLon2());
		    			loadCountry(fc.getSelectedFile(), filter);
		    		}
		        }
			}
			
		});
		loadFile.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getOsmFileChooser();
		        int answer = fc.showOpenDialog(frame) ;
		        if (answer == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null){
		        	loadCountry(fc.getSelectedFile(), null);
		        }
			}
			
		});
		saveOsmFile.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				if(region == null){
					return;
				}
				JFileChooser fc = getOsmFileChooser();
				int answer = fc.showSaveDialog(frame);
		        if (answer == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null){
		        	saveCountry(fc.getSelectedFile());
		        }
			}
			
		});
	}
	
	public JFileChooser getOsmFileChooser(){
		JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose osm file");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setCurrentDirectory(DataExtractionSettings.getSettings().getDefaultWorkingDir().getParentFile());
        fc.setFileFilter(new FileFilter(){

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".bz2") || f.getName().endsWith(".osm");
			}

			@Override
			public String getDescription() {
				return "Osm Files (*.bz2, *.osm)";
			}
        });
        return fc;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public void loadCountry(final File f, final IOsmStorageFilter filter){
		try {
    		final ProgressDialog dlg = new ProgressDialog(frame, "Loading osm file");
    		dlg.setRunnable(new Runnable(){

				@Override
				public void run() {
					Region res;
					try {
						DataExtraction dataExtraction = new DataExtraction(buildAddressIndex.isSelected(), buildPoiIndex.isSelected(),
								buildTransportIndex.isSelected(), normalizingStreets.isSelected(), loadingAllData.isSelected(), 
								DataExtractionSettings.getSettings().getLoadEntityInfo(), 
								DataExtractionSettings.getSettings().getDefaultWorkingDir());
						if(!buildAddressIndex.isSelected()){
							buildAddressIndex.setEnabled(false);
						}
						if(!buildTransportIndex.isSelected()){
							buildTransportIndex.setEnabled(false);
						}
						if(!buildPoiIndex.isSelected()){
							buildPoiIndex.setEnabled(false);
						}
						res = dataExtraction.readCountry(f.getAbsolutePath(), dlg, filter);
					} catch (IOException e) {
						throw new IllegalArgumentException(e);
					} catch (SAXException e) {
						throw new IllegalStateException(e);
					} catch (SQLException e) {
						throw new IllegalStateException(e);
					}
					dlg.setResult(res);
				}
    		});
			Region region = (Region) dlg.run();
			if(region != null){
				setRegion(region, region.getName());
				frame.setTitle("OsmAnd Map Creator - " + f.getName());
			} else {
				//frame.setTitle("OsmAnd Map Creator");
			}
		} catch (InterruptedException e1) {
			log.error("Interrupted", e1); 
			updateButtonsBar();
		} catch (InvocationTargetException e1) {
			ExceptionHandler.handle("Exception during operation", e1.getCause());
		}
	}
	
	public void saveCountry(final File f){
		final OsmStorageWriter writer = new OsmStorageWriter();
		try {
    		final ProgressDialog dlg = new ProgressDialog(frame, "Saving osm file");
    		dlg.setRunnable(new Runnable() {
				@Override
				public void run() {
					try {
						OutputStream output = new FileOutputStream(f);
						try {
							if (f.getName().endsWith(".bz2")) {
								output.write('B');
								output.write('Z');
								output = new CBZip2OutputStream(output);
							}
							writer.saveStorage(output, region.getStorage(), null, false);
						} finally {
							output.close();
						}
					} catch (IOException e) {
						throw new IllegalArgumentException(e);
					} catch (XMLStreamException e) {
						throw new IllegalArgumentException(e);
					}
				}
			});
    		dlg.run();
		} catch (InterruptedException e1) {
			log.error("Interrupted", e1); 
		} catch (InvocationTargetException e1) {
			ExceptionHandler.handle("Log file is not found", e1.getCause());
		}
	}
	
	@Override
	public void locationChanged(final double newLatitude, final double newLongitude, Object source){
		if (amenitiesTree != null) {
			Region reg = (Region) amenitiesTree.getModelObject();
			List<Amenity> closestAmenities = reg.getAmenityManager().getClosestObjects(newLatitude, newLongitude, 0, 5);
			MapUtils.sortListOfMapObject(closestAmenities, newLatitude, newLongitude);
			

			Map<AmenityType, List<Amenity>> filter = new HashMap<AmenityType, List<Amenity>>();
			for (Amenity n : closestAmenities) {
				AmenityType type = n.getType();
				if (!filter.containsKey(type)) {
					filter.put(type, new ArrayList<Amenity>());
				}
				filter.get(type).add(n);
			}
			
			
			for (int i = 1; i < amenitiesTree.getChildCount(); i++) {
				AmenityType type = (AmenityType) ((DataExtractionTreeNode) amenitiesTree.getChildAt(i)).getModelObject();
				((DefaultMutableTreeNode) amenitiesTree.getChildAt(i)).removeAllChildren();
				if (filter.get(type) != null) {
					for (Amenity n : filter.get(type)) {
						int dist = (int) (MapUtils.getDistance(n.getLocation(), newLatitude, newLongitude));
						String str = n.getStringWithoutType(false) + " [" + dist + " m ]";
						DataExtractionTreeNode node = new DataExtractionTreeNode(str, n);
						((DefaultMutableTreeNode) amenitiesTree.getChildAt(i)).add(node);
					}
				}
				((DefaultTreeModel)treePlaces.getModel()).nodeStructureChanged(amenitiesTree.getChildAt(i));
			}
			((DefaultMutableTreeNode) amenitiesTree.getChildAt(0)).removeAllChildren();

			for (int i = 0; i < 15 && i < closestAmenities.size(); i++) {
				Amenity n = closestAmenities.get(i);
				int dist = (int) (MapUtils.getDistance(n.getLocation(), newLatitude, newLongitude));
				String str = n.getSimpleFormat(false) + " [" + dist + " m ]";
				((DefaultMutableTreeNode) amenitiesTree.getChildAt(0)).add(new DataExtractionTreeNode(str, n));
				((DefaultTreeModel)treePlaces.getModel()).nodeStructureChanged(amenitiesTree.getChildAt(0));
			}

			
		}
	}
	
	public void updateListCities(Region r, String text, JList jList) {
		Collection<City> city;
		if (r == null) {
			city = Collections.emptyList();
		} else {
			city = r.getSuggestedCities(text, 100);
		}
		City[] names = new City[city.size()];
		int i = 0;
		for (City c : city) {
			names[i++] = c;
		}
		jList.setListData(names);
	}
	
	
	public static class DataExtractionTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;
		private final Object modelObject;

		public DataExtractionTreeNode(String name, Object modelObject){
			super(name);
			this.modelObject = modelObject;
		}
		
		public Object getModelObject(){
			return modelObject;
		}
		
	}
	
	public static class RegionCellEditor extends DefaultTreeCellEditor {

		public RegionCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
			super(tree, renderer);
		}

		public RegionCellEditor(JTree tree, DefaultTreeCellRenderer renderer, TreeCellEditor editor) {
			super(tree, renderer, editor);
		}

		public boolean isCellEditable(EventObject event) {
			boolean returnValue = super.isCellEditable(event);
			if (returnValue) {
				Object node = tree.getLastSelectedPathComponent();
				if (node instanceof DataExtractionTreeNode) {
					DataExtractionTreeNode treeNode = (DataExtractionTreeNode) node;
					if (treeNode.getModelObject() instanceof Region || treeNode.getModelObject() instanceof MapObject) {
						return true;
					}
				}
			}
			return returnValue;
		}
		
	}
	public class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent event) {
			// save preferences
			DataExtractionSettings settings = DataExtractionSettings.getSettings();
			settings.saveDefaultLocation(mapPanel.getLatitude(), mapPanel.getLongitude());
			settings.saveDefaultZoom(mapPanel.getZoom());
			settings.saveWindowBounds(frame.getBounds());
			System.exit(0);
		}
	}
	
	private class PopupTrigger extends MouseAdapter {
		
		private final JPopupMenu popupMenu;
		
	    public PopupTrigger(JPopupMenu popupMenu) {
			this.popupMenu = popupMenu;
		}
		public void mouseReleased(MouseEvent e)
	    {
	      if (e.isPopupTrigger())
	      {
	        int x = e.getX();
	        int y = e.getY();
	        TreePath path = treePlaces.getPathForLocation(x, y);
	        if (path != null) {
	        	if(!treePlaces.getSelectionModel().isPathSelected(path)){
	        		treePlaces.setSelectionPath(path);
	        	}
	        	popupMenu.show(treePlaces, x, y);
	        }
	      }
	    }
	  }

	
}
