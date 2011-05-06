package net.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.osmand.data.MapTileDownloader;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.ITileSource;
import net.osmand.osm.MapUtils;
import net.osmand.swing.MapPanel.MapSelectionArea;


public class TileBundleDownloadDialog extends JDialog {
	
	private static final long serialVersionUID = -4862884032977071296L;
	
	private JLabel label;
	private ITileSource map;
	private MapSelectionArea selectionArea;
	private int zoom;
	private JSpinner startSpinner;
	private JSpinner endSpinner;
	private JButton downloadButton;
	private JButton cancelButton;
	private JButton specifyFolder;
	private File tilesLocation;


	public TileBundleDownloadDialog(Component parent, MapPanel panel){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	map = panel.getMap();
    	panel.getLatitude();
    	selectionArea = panel.getSelectionArea();
    	zoom = panel.getZoom();
    	tilesLocation = panel.getTilesLocation();
    	setTitle(Messages.getString("TileBundleDownloadDialog.DOWNLOAD.BUNDLE.TILES")); //$NON-NLS-1$
        initDialog();
        
    }
	
	public void showDialog(){
		setSize(550, 150);
        double x = getParent().getBounds().getCenterX();
        double y = getParent().getBounds().getCenterY();
        setLocation((int) x - getWidth() / 2, (int) y - getHeight() / 2);
        setVisible(true);
	}
	
	protected SpinnerNumberModel getSpinnerModel(int minimum, int maximum){
		SpinnerNumberModel model = new SpinnerNumberModel();
		model.setStepSize(1);
		model.setValue(zoom);
		model.setMaximum(maximum);
		model.setMinimum(minimum);
		return model;
	}

	private void initDialog() {
		JPanel pane = new JPanel(new BorderLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pane);
        
        label = new JLabel();
        pane.add(label, BorderLayout.CENTER);
        
        JPanel zoomControls = new JPanel();
        zoomControls.setLayout(new BoxLayout(zoomControls, BoxLayout.X_AXIS));
        
        JLabel lab = new JLabel(Messages.getString("TileBundleDownloadDialog.START.ZOOM.LEVEL")); //$NON-NLS-1$
        zoomControls.add(lab);
        startSpinner = new JSpinner(getSpinnerModel(map.getMinimumZoomSupported(), zoom));
        zoomControls.add(startSpinner);
        startSpinner.setMaximumSize(new Dimension(15, startSpinner.getMaximumSize().height));
        
        zoomControls.add(Box.createHorizontalStrut(15));
        lab = new JLabel(Messages.getString("TileBundleDownloadDialog.END.ZOOM.LEVEL")); //$NON-NLS-1$
        zoomControls.add(lab);
        endSpinner = new JSpinner(getSpinnerModel(zoom, map.getMaximumZoomSupported()));
        zoomControls.add(endSpinner);
        endSpinner.setMaximumSize(new Dimension(15, endSpinner.getMaximumSize().height));
        zoomControls.add(Box.createHorizontalGlue());
        pane.add(zoomControls, BorderLayout.NORTH);
        
        
        JPanel buttonControls = new JPanel();
        buttonControls.setLayout(new BoxLayout(buttonControls, BoxLayout.X_AXIS));
        buttonControls.add(Box.createHorizontalGlue());
        specifyFolder = new JButton(Messages.getString("TileBundleDownloadDialog.SPECIFY.FOLDER")); //$NON-NLS-1$
        buttonControls.add(specifyFolder);
        buttonControls.add(Box.createHorizontalStrut(3));
        downloadButton = new JButton(Messages.getString("TileBundleDownloadDialog.DOWNLOAD.TILES")); //$NON-NLS-1$
        buttonControls.add(downloadButton);
        buttonControls.add(Box.createHorizontalStrut(3));
        cancelButton = new JButton(Messages.getString("TileBundleDownloadDialog.CANCEL")); //$NON-NLS-1$
        buttonControls.add(cancelButton);
        pane.add(buttonControls, BorderLayout.SOUTH);
        
        updateLabel();
        addListeners();
	}
	
	private void addListeners(){
		cancelButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		startSpinner.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				((SpinnerNumberModel)endSpinner.getModel()).setMinimum((Integer) startSpinner.getValue());
				updateLabel();
			}
		});
		endSpinner.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				((SpinnerNumberModel)startSpinner.getModel()).setMaximum((Integer) endSpinner.getValue());
				updateLabel();
			}
		});
		downloadButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				downloadTiles();
			}
			
		});
		specifyFolder.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
		        fc.setDialogTitle(Messages.getString("TileBundleDownloadDialog.CHOOSE.DIRECTORY")); //$NON-NLS-1$
		        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		        if(tilesLocation != null){
		        	fc.setCurrentDirectory(tilesLocation);
		        }
		        if(fc.showOpenDialog(TileBundleDownloadDialog.this) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null && 
		        		fc.getSelectedFile().isDirectory()){
		        	tilesLocation = fc.getSelectedFile();
		        }
			}
			
		});
		
	}
	
	public void downloadTiles(){
		setVisible(false);
		final ProgressDialog progressDialog = new ProgressDialog(this, Messages.getString("TileBundleDownloadDialog.DOWNLOADING.TILES")); //$NON-NLS-1$
		int numberTiles = 0;
		final int startZoom = (Integer) startSpinner.getValue();
		final int endZoom = (Integer) endSpinner.getValue();
		for (int zoom = startZoom; zoom <= endZoom; zoom++) {
			int x1 = (int) MapUtils.getTileNumberX(zoom, selectionArea.getLon1());
			int x2 = (int) MapUtils.getTileNumberX(zoom, selectionArea.getLon2());
			int y1 = (int) MapUtils.getTileNumberY(zoom, selectionArea.getLat1());
			int y2 = (int) MapUtils.getTileNumberY(zoom, selectionArea.getLat2());
			numberTiles += (x2 - x1 + 1) * (y2 - y1 + 1);
		}
		final int number = numberTiles;
		final MapTileDownloader instance = MapTileDownloader.getInstance();
		progressDialog.setRunnable(new Runnable(){

			@Override
			public void run() {
				progressDialog.startTask(Messages.getString("TileBundleDownloadDialog.LOADING"), number); //$NON-NLS-1$
				for (int zoom = startZoom; zoom <= endZoom; zoom++) {
					int x1 = (int) MapUtils.getTileNumberX(zoom, selectionArea.getLon1());
					int x2 = (int) MapUtils.getTileNumberX(zoom, selectionArea.getLon2());
					int y1 = (int) MapUtils.getTileNumberY(zoom, selectionArea.getLat1());
					int y2 = (int) MapUtils.getTileNumberY(zoom, selectionArea.getLat2());
					for(int x = x1; x <= x2; x++){
						for(int y=y1; y<= y2; y++){
							String file = getFileForImage(x, y, zoom, map.getTileFormat());
							if(new File(tilesLocation, file).exists()){
								progressDialog.progress(1);
							} else {
								DownloadRequest req = new DownloadRequest(map.getUrlToLoad(x, y, zoom), 
										new File(tilesLocation, file), x, y, zoom);
								instance.requestToDownload(req);
							}

						}
					}
					while(instance.isSomethingBeingDownloaded()){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							throw new IllegalArgumentException(e);
						}
					}
				}
				
			}
			
		});
		ArrayList<IMapDownloaderCallback> previousCallbacks = 
			new ArrayList<IMapDownloaderCallback>(instance.getDownloaderCallbacks());
		instance.getDownloaderCallbacks().clear();
		instance.addDownloaderCallback(new IMapDownloaderCallback(){
			@Override
			public void tileDownloaded(DownloadRequest request) {
				// TODO request could be null if bundle loading?
				progressDialog.progress(1);
			}
			
		});
		
		
		try {
			progressDialog.run();
			instance.refuseAllPreviousRequests();
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle((Exception) e.getCause());
		} catch (InterruptedException e) {
			ExceptionHandler.handle(e);
		} finally {
			instance.getDownloaderCallbacks().clear();
			instance.getDownloaderCallbacks().addAll(previousCallbacks);
		}
		
	}
	
	public String getFileForImage (int x, int y, int zoom, String ext){
		return map.getName() +"/"+zoom+"/"+(x) +"/"+y+ext+".tile"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	
	protected void updateLabel(){
		int numberTiles = 0;
		for (int zoom = (Integer) startSpinner.getValue(); zoom <= (Integer) endSpinner.getValue(); zoom++) {
			int x1 = (int) MapUtils.getTileNumberX(zoom, selectionArea.getLon1());
			int x2 = (int) MapUtils.getTileNumberX(zoom, selectionArea.getLon2());
			int y1 = (int) MapUtils.getTileNumberY(zoom, selectionArea.getLat1());
			int y2 = (int) MapUtils.getTileNumberY(zoom, selectionArea.getLat2());
			numberTiles += (x2 - x1 + 1) * (y2 - y1 + 1);
		}
		
		String text = MessageFormat.format(Messages.getString("TileBundleDownloadDialog.REQUEST.DOWNLOAD"),  //$NON-NLS-1$
				numberTiles, map.getName(),	(double)numberTiles*12/1000);
		label.setText(text);
	}

}
