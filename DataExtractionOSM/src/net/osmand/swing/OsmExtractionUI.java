package net.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.stream.XMLStreamException;

import net.osmand.MapCreatorVersion;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.io.IOsmStorageFilter;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.osm.io.OsmBoundsFilter;
import net.osmand.osm.io.OsmStorageWriter;
import net.osmand.swing.MapPanel.MapSelectionArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.xml.sax.SAXException;

import rtree.RTree;


public class OsmExtractionUI implements IMapLocationListener {

	private static final Log log = LogFactory.getLog(OsmExtractionUI.class);
	public static final String LOG_PATH  = getUserLogDirectoryPath() + "/osmand.log"; //$NON-NLS-1$ //$NON-NLS-2$
	public static OsmExtractionUI MAIN_APP;

	public static String getUserLogDirectoryPath() {
		return DataExtractionSettings.getSettings().getDefaultWorkingDir().getAbsolutePath();
//		String path = null;
//		if (System.getProperty("os.name").startsWith("Windows")) {
//			path = System.getenv("APPDATA").replaceAll("\\\\", "/");
//		} else if (System.getProperty("os.name").startsWith("Mac")) {
//			path = System.getProperty("user.home") + "/Library/Logs";
//		} else if (System.getenv("XDG_CACHE_HOME") != null) {
//			path = System.getenv("XDG_CACHE_HOME");
//		} else {
//			path = System.getProperty("user.home") + "/.cache";
//		}
//		return path;
	}
	
	public static void main(String[] args) {
		// first of all config log
		configLogFile();
		
		final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				if(!(e instanceof ThreadDeath)){
					ExceptionHandler.handle("Error in thread " + t.getName(), e); //$NON-NLS-1$
				}
				if(defaultHandler != null){
					defaultHandler.uncaughtException(t, e);
				}
			}
		});
		
        MAIN_APP = new OsmExtractionUI();
        MAIN_APP.frame.setBounds(DataExtractionSettings.getSettings().getWindowBounds());
        MAIN_APP.frame.setVisible(true);
	}

	public static void configLogFile() {
		new File(LOG_PATH).getParentFile().mkdirs();
		try {
			FileHandler fh = new FileHandler(LOG_PATH, 5000000, 1, true);
			fh.setFormatter(new SimpleFormatter());
			LogManager.getLogManager().getLogger("").addHandler(fh);
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	
	
	
	private JTree treePlaces;
//	private DataExtractionTreeNode amenitiesTree;
//	private TreeModelListener treeModelListener;
	
	
	private MapPanel mapPanel;
	private JFrame frame;
	private JLabel statusBarLabel;
	

	private JCheckBox buildPoiIndex;
	private JCheckBox buildAddressIndex;
	private JCheckBox buildMapIndex;
	private JCheckBox buildRoutingIndex;
	private JCheckBox buildTransportIndex;
	private JCheckBox normalizingStreets;
	private JCheckBox showOfflineIndex;

	private String regionName;
	
	public OsmExtractionUI(){
		createUI();
	}

	
        
	
	
	public void createUI(){
		frame = new JFrame(Messages.getString("OsmExtractionUI.OSMAND_MAP_CREATOR")); //$NON-NLS-1$
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			log.error("Can't set look and feel", e); //$NON-NLS-1$
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
	    statusBarLabel.setText(workingDir == null ? Messages.getString("OsmExtractionUI.WORKING_DIR_UNSPECIFIED") : Messages.getString("OsmExtractionUI.WORKING_DIRECTORY") + workingDir.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
	    
	   
	    treePlaces = new JTree();
		treePlaces.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(Messages.getString("OsmExtractionUI.REGION")), false)); 	     //$NON-NLS-1$
	    JSplitPane panelForTreeAndMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(treePlaces), mapPanel);
	    panelForTreeAndMap.setResizeWeight(0.2);
	    content.add(panelForTreeAndMap, BorderLayout.CENTER);
	    
	    createButtonsBar(content);
	   
	    JMenuBar bar = new JMenuBar();
	    fillMenuWithActions(bar);

	    frame.setJMenuBar(bar);
	}
	
	
	
	
	public void createButtonsBar(Container content){
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		content.add(panel, BorderLayout.NORTH);
		
		buildMapIndex = new JCheckBox();
		buildMapIndex.setText(Messages.getString("OsmExtractionUI.BUILD_MAP")); //$NON-NLS-1$
		panel.add(buildMapIndex);
		buildMapIndex.setSelected(true);
		
		buildRoutingIndex = new JCheckBox();
		buildRoutingIndex.setText(Messages.getString("OsmExtractionUI.BUILD_ROUTING")); //$NON-NLS-1$
		panel.add(buildRoutingIndex);
		buildRoutingIndex.setSelected(true);
		
		buildPoiIndex = new JCheckBox();
		buildPoiIndex.setText(Messages.getString("OsmExtractionUI.BUILD_POI")); //$NON-NLS-1$
		panel.add(buildPoiIndex);
		buildPoiIndex.setSelected(true);
		
		buildAddressIndex = new JCheckBox();
		buildAddressIndex.setText(Messages.getString("OsmExtractionUI.BUILD_ADDRESS")); //$NON-NLS-1$
		panel.add(buildAddressIndex);
		buildAddressIndex.setSelected(true);
		
		normalizingStreets = new JCheckBox();
		normalizingStreets.setText(Messages.getString("OsmExtractionUI.NORMALIZE_STREETS")); //$NON-NLS-1$
//		panel.add(normalizingStreets);
		normalizingStreets.setSelected(true);
		
		buildTransportIndex = new JCheckBox();
		buildTransportIndex.setText(Messages.getString("OsmExtractionUI.BUILD_TRANSPORT")); //$NON-NLS-1$
		panel.add(buildTransportIndex);
		buildTransportIndex.setSelected(true);
		
		showOfflineIndex = new JCheckBox();
		showOfflineIndex.setText("Offline Rendering");
		panel.add(showOfflineIndex);
		showOfflineIndex.setSelected(false);
		showOfflineIndex.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(showOfflineIndex.isSelected()) {
					NativePreferencesDialog dlg = new NativePreferencesDialog(frame);
					dlg.showDialog();
					if(dlg.isOkPressed()) {
						initNativeRendering(NativePreferencesDialog.getRenderingProperties());
					} else {
						showOfflineIndex.setSelected(false);
					}
				} else {
					mapPanel.setNativeLibrary(null);
				}
			}
		});
	}
	
	private void initNativeRendering(String renderingProperties) {
		String fl = DataExtractionSettings.getSettings().getNativeLibFile();
		if (fl != null) {
			NativeSwingRendering lib = NativeSwingRendering.loadLibrary(fl);
			if (lib != null) {
				try {
					lib.initFilesInDir(new File(DataExtractionSettings.getSettings().getBinaryFilesDir()));
					lib.loadRuleStorage(DataExtractionSettings.getSettings().getRenderXmlPath(), renderingProperties);
					mapPanel.setNativeLibrary(lib);
					mapPanel.repaint();
				} catch (SAXException e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e);
				}
			}
		} else {
			JOptionPane.showMessageDialog(frame, "Native library was not configured in settings");
		}
	}
	
	public void fillMenuWithActions(final JMenuBar bar){
		JMenu menu = new JMenu(Messages.getString("OsmExtractionUI.MENU_FILE")); //$NON-NLS-1$
		bar.add(menu);
		JMenuItem loadFile = new JMenuItem(Messages.getString("OsmExtractionUI.MENU_SELECT_FILE")); //$NON-NLS-1$
		menu.add(loadFile);
		JMenuItem loadSpecifiedAreaFile = new JMenuItem(Messages.getString("OsmExtractionUI.MENU_SELECT_OSM_FILE_AREA")); //$NON-NLS-1$
		menu.add(loadSpecifiedAreaFile);
		JMenuItem specifyWorkingDir = new JMenuItem(Messages.getString("OsmExtractionUI.SPECIFY_WORKING_DIR")); //$NON-NLS-1$
		menu.add(specifyWorkingDir);
		menu.addSeparator();
		JMenuItem exitMenu= new JMenuItem(Messages.getString("OsmExtractionUI.MENU_EXIT")); //$NON-NLS-1$
		menu.add(exitMenu);
		
		JMenu tileSource = MapPanel.getMenuToChooseSource(mapPanel);
		final JMenuItem sqliteDB = new JMenuItem(Messages.getString("OsmExtractionUI.MENU_CREATE_SQLITE")); //$NON-NLS-1$
		tileSource.addSeparator();
		tileSource.add(sqliteDB);
		bar.add(tileSource);
		
		menu = new JMenu(Messages.getString("OsmExtractionUI.MENU_WINDOW")); //$NON-NLS-1$
		bar.add(menu);
		JMenuItem settings = new JMenuItem(Messages.getString("OsmExtractionUI.MENU_SETTINGS")); //$NON-NLS-1$
		menu.add(settings);
		menu.addSeparator();
		JMenuItem openLogFile = new JMenuItem(Messages.getString("OsmExtractionUI.MENU_OPEN_LOG")); //$NON-NLS-1$
		menu.add(openLogFile);
		
		menu = new JMenu(Messages.getString("OsmExtractionUI.MENU_ABOUT")); //$NON-NLS-1$
		bar.add(menu);
		JMenuItem aboutApplication = new JMenuItem(Messages.getString("OsmExtractionUI.MENU_ABOUT_2")); //$NON-NLS-1$
		menu.add(aboutApplication);
		
		
		aboutApplication.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, MapCreatorVersion.APP_MAP_CREATOR_FULL_NAME);
			}
		});

		openLogFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = new File(OsmExtractionUI.LOG_PATH);
				if (file != null && file.exists()) {
					if (System.getProperty("os.name").startsWith("Windows")) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							Runtime.getRuntime().exec(new String[] { "notepad.exe", file.getAbsolutePath() }); //$NON-NLS-1$
						} catch (IOException es) {
							ExceptionHandler.handle(Messages.getString("OsmExtractionUI.UNABLE_OPEN_FILE"), es); //$NON-NLS-1$
						}
					} else {
						JOptionPane.showMessageDialog(frame, Messages.getString("OsmExtractionUI.OPEN_LOG_FILE_MANUALLY") + LOG_PATH); //$NON-NLS-1$
					}

				} else {
					ExceptionHandler.handle(Messages.getString("OsmExtractionUI.LOG_FILE_NOT_FOUND")); //$NON-NLS-1$
				}
			}
		});
		
		sqliteDB.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				final String regionName = OsmExtractionUI.this.regionName == null ? Messages.getString("OsmExtractionUI.REGION") : OsmExtractionUI.this.regionName; //$NON-NLS-1$
				final ITileSource map = mapPanel.getMap();
				if(map != null){
					try {
			    		final ProgressDialog dlg = new ProgressDialog(frame, Messages.getString("OsmExtractionUI.CREATING_INDEX")); //$NON-NLS-1$
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
						log.error("Interrupted", e1);  //$NON-NLS-1$
					} catch (InvocationTargetException e1) {
						ExceptionHandler.handle("Can't create big planet sqlite index", (Exception) e1.getCause()); //$NON-NLS-1$
					}
					
					
				}
			}
		});

		
		exitMenu.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				exit();
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
		        fc.setDialogTitle(Messages.getString("OsmExtractionUI.CHOOSE_WORKING_DIR")); //$NON-NLS-1$
		        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		        File workingDir = DataExtractionSettings.getSettings().getDefaultWorkingDir();
		        if(workingDir != null){
		        	fc.setCurrentDirectory(workingDir);
		        }
		        if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null && 
		        		fc.getSelectedFile().isDirectory()){
		        	DataExtractionSettings.getSettings().saveDefaultWorkingDir(fc.getSelectedFile());
		        	mapPanel.setTilesLocation(DataExtractionSettings.getSettings().getTilesDirectory());
		        	statusBarLabel.setText(Messages.getString("OsmExtractionUI.WORKING_DIR") + fc.getSelectedFile().getAbsolutePath()); //$NON-NLS-1$
		        	JMenu tileSource = MapPanel.getMenuToChooseSource(mapPanel);
		    		tileSource.add(sqliteDB);
		    		bar.remove(1);
		    		bar.add(tileSource, 1);
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
		        	dlg.setTitle(Messages.getString("OsmExtractionUI.SELECT_AREA_TO_FILTER")); //$NON-NLS-1$
		        	MapPanel panel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
		        	panel.setLatLon(mapPanel.getLatitude(), mapPanel.getLongitude());
		        	panel.setZoom(mapPanel.getZoom());
		        	final StringBuilder res = new StringBuilder();
		        	panel.getLayer(MapInformationLayer.class).setAreaActionHandler(new AbstractAction(Messages.getString("OsmExtractionUI.SELECT_AREA")){ //$NON-NLS-1$
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

	}
	
	public JFileChooser getOsmFileChooser(){
		JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(Messages.getString("OsmExtractionUI.CHOOSE_OSM_FILE")); //$NON-NLS-1$
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setCurrentDirectory(DataExtractionSettings.getSettings().getDefaultWorkingDir().getParentFile());
        fc.setFileFilter(new FileFilter(){

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".bz2") || f.getName().endsWith(".osm") || f.getName().endsWith(".pbf"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			@Override
			public String getDescription() {
				return Messages.getString("OsmExtractionUI.OSM_FILES"); //$NON-NLS-1$
			}
        });
        return fc;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public void loadCountry(final File f, final IOsmStorageFilter filter){
		try {
    		final ProgressDialog dlg = new ProgressDialog(frame, Messages.getString("OsmExtractionUI.LOADING_OSM_FILE")); //$NON-NLS-1$
    		dlg.setRunnable(new Runnable(){

				@Override
				public void run() {
					File dir = DataExtractionSettings.getSettings().getDefaultWorkingDir();
					IndexCreator creator = new IndexCreator(dir);
					try {
						creator.setIndexAddress(buildAddressIndex.isSelected());
						creator.setIndexPOI(buildPoiIndex.isSelected());
						creator.setNormalizeStreets(normalizingStreets.isSelected());
						creator.setIndexTransport(buildTransportIndex.isSelected());
						creator.setIndexMap(buildMapIndex.isSelected());
						creator.setIndexRouting(buildRoutingIndex.isSelected());
						creator.setCityAdminLevel(DataExtractionSettings.getSettings().getCityAdminLevel());
						String fn = DataExtractionSettings.getSettings().getMapRenderingTypesFile();
						MapRenderingTypes types;
						if(fn == null || fn.length() == 0){
							types = MapRenderingTypes.getDefault();
						} else {
							types = new MapRenderingTypes(fn);
						}
						RTree.clearCache();
						int smoothness = 0;
						try {
							smoothness = Integer.parseInt(DataExtractionSettings.getSettings().getLineSmoothness());
						} catch (NumberFormatException e) {
						}
						creator.setZoomWaySmothness(smoothness);
						creator.generateIndexes(f, dlg, filter, DataExtractionSettings.getSettings().getMapZooms(), types, log);
					} catch (IOException e) {
						throw new IllegalArgumentException(e);
					} catch (SAXException e) {
						throw new IllegalStateException(e);
					} catch (SQLException e) {
						throw new IllegalStateException(e);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
					regionName = creator.getRegionName();
					StringBuilder msg = new StringBuilder();
					msg.append(Messages.getString("OsmExtractionUI.INDEXES_FOR")).append(regionName).append(" : "); //$NON-NLS-1$ //$NON-NLS-2$
					boolean comma = false;
					if (buildMapIndex.isSelected()) {
						if(comma) msg.append(", "); //$NON-NLS-1$
						comma = true;
						msg.append(Messages.getString("OsmExtractionUI.MAP")); //$NON-NLS-1$
					}
					if (buildPoiIndex.isSelected()) {
						if(comma) msg.append(", "); //$NON-NLS-1$
						comma = true;
						msg.append(Messages.getString("OsmExtractionUI.POI")); //$NON-NLS-1$
					}
					if (buildAddressIndex.isSelected()) {
						if(comma) msg.append(", "); //$NON-NLS-1$
						comma = true;
						msg.append(Messages.getString("OsmExtractionUI.ADDRESS")); //$NON-NLS-1$
					}
					if (buildTransportIndex.isSelected()) {
						if(comma) msg.append(", "); //$NON-NLS-1$
						comma = true;
						msg.append(Messages.getString("OsmExtractionUI.TRANSPORT")); //$NON-NLS-1$
					}
					msg.append(MessageFormat.format(Messages.getString("OsmExtractionUI.WERE_SUCCESFULLY_CREATED"), dir.getAbsolutePath())); //$NON-NLS-1$
					JOptionPane pane = new JOptionPane(msg);
					JDialog dialog = pane.createDialog(frame, Messages.getString("OsmExtractionUI.GENERATION_DATA")); //$NON-NLS-1$
					dialog.setVisible(true);
				}
    		});
    		
			dlg.run();
			frame.setTitle(Messages.getString("OsmExtractionUI.OSMAND_MAP_CREATOR_FILE") + f.getName()); //$NON-NLS-1$
		} catch (InterruptedException e1) {
			log.error("Interrupted", e1);  //$NON-NLS-1$
		} catch (InvocationTargetException e1) {
			ExceptionHandler.handle("Exception during operation", e1.getCause()); //$NON-NLS-1$
		}
	}
	
	public void saveCountry(final File f, final OsmBaseStorage storage){
		final OsmStorageWriter writer = new OsmStorageWriter();
		try {
    		final ProgressDialog dlg = new ProgressDialog(frame, Messages.getString("OsmExtractionUI.SAVING_OSM_FILE")); //$NON-NLS-1$
    		dlg.setRunnable(new Runnable() {
				@Override
				public void run() {
					try {
						OutputStream output = new FileOutputStream(f);
						try {
							if (f.getName().endsWith(".bz2")) { //$NON-NLS-1$
								output.write('B');
								output.write('Z');
								output = new CBZip2OutputStream(output);
							}
							writer.saveStorage(output, storage, null, false);
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
			log.error("Interrupted", e1);  //$NON-NLS-1$
		} catch (InvocationTargetException e1) {
			ExceptionHandler.handle("Log file is not found", e1.getCause()); //$NON-NLS-1$
		}
	}
	
	@Override
	public void locationChanged(final double newLatitude, final double newLongitude, Object source){
//		recalculateAmenities(newLatitude, newLongitude);
	}



	
	public class ExitListener extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent event) {
			exit();
		}
	}
	
	public void exit(){
		// save preferences
		DataExtractionSettings settings = DataExtractionSettings.getSettings();
		settings.saveDefaultLocation(mapPanel.getLatitude(), mapPanel.getLongitude());
		settings.saveDefaultZoom(mapPanel.getZoom());
		settings.saveWindowBounds(frame.getBounds());
		System.exit(0);
	}
	


	
	
	// OLD CODE
/*

	public JTree createTree(Container content) {		
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
							if (location != null) {
								if (o instanceof Street) {
									DataTileManager<Way> ways = new DataTileManager<Way>();
									for (Way w : ((Street) o).getWayNodes()) {
										LatLon l = w.getLatLon();
										ways.registerObject(l.getLatitude(), l.getLongitude(), w);
									}
									mapPanel.setPoints(ways);
									mapPanel.requestFocus();
								}
								mapPanel.setLatLon(location.getLatitude(), location.getLongitude());
								mapPanel.requestFocus();
							}
							if (o instanceof TransportRoute) {
								DataTileManager<Entity> ways = new DataTileManager<Entity>();
								for (Way w : ((TransportRoute) o).getWays()) {
									LatLon l = w.getLatLon();
									ways.registerObject(l.getLatitude(), l.getLongitude(), w);
								}
								for (TransportStop w : ((TransportRoute) o).getBackwardStops()) {
									LatLon l = w.getLocation();
									ways.registerObject(l.getLatitude(), l.getLongitude(), new Node(l.getLatitude(), l.getLongitude(), w
											.getId()));
								}
								for (TransportStop w : ((TransportRoute) o).getForwardStops()) {
									LatLon l = w.getLocation();
									ways.registerObject(l.getLatitude(), l.getLongitude(), new Node(l.getLatitude(), l.getLongitude(), w
											.getId()));
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
				if (e.getChildren() != null && e.getChildren().length > 0) {
					node = e.getChildren()[0];
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
	
	private void recalculateAmenities(final double newLatitude, final double newLongitude) {
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
*/

}