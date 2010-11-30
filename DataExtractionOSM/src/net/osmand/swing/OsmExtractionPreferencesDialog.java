package net.osmand.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class OsmExtractionPreferencesDialog extends JDialog {
	
	private static final long serialVersionUID = -4862884032977071296L;
	
	private JButton okButton;
	private JButton cancelButton;

	private JTextField streetSuffixes;
	private JTextField streetDefaultSuffixes;
	private JTextField mapZooms;
	private JTextField renderingTypesFile;

	private JCheckBox useInternet;
//	private JCheckBox supressWarning;
//	private JCheckBox loadWholeOsmInfo;

	public OsmExtractionPreferencesDialog(Component parent){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	setTitle(Messages.getString("OsmExtractionPreferencesDialog.PREFERENCES")); //$NON-NLS-1$
        initDialog();
        
    }
	
	public void showDialog(){
		setSize(600, 280);
        double x = getParent().getBounds().getCenterX();
        double y = getParent().getBounds().getCenterY();
        setLocation((int) x - getWidth() / 2, (int) y - getHeight() / 2);
        setVisible(true);
	}
	
	private void initDialog() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(pane);
        
        
        createGeneralSection(pane);
        createNormalizingStreetSection(pane);
        pane.add(Box.createVerticalGlue());	
        
        FlowLayout l = new FlowLayout(FlowLayout.RIGHT);
        JPanel buttonsPane = new JPanel(l);
        okButton = new JButton(Messages.getString("OsmExtractionPreferencesDialog.OK")); //$NON-NLS-1$
        buttonsPane.add(okButton);
        cancelButton = new JButton(Messages.getString("OsmExtractionPreferencesDialog.CANCEL")); //$NON-NLS-1$
        buttonsPane.add(cancelButton);
        
        buttonsPane.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) l.preferredLayoutSize(buttonsPane).getHeight()));
        pane.add(buttonsPane);
        
        addListeners();
	}
	
	private void createGeneralSection(JPanel root) {
		JPanel panel = new JPanel();
//		panel.setLayout(new GridLayout(3, 1, 5, 5));
		panel.setLayout(new GridLayout(1, 1, 5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("OsmExtractionPreferencesDialog.GENERAL"))); //$NON-NLS-1$
		root.add(panel);
		
		useInternet = new JCheckBox();
		useInternet.setText(Messages.getString("OsmExtractionPreferencesDialog.INTERNET.TO.DOWNLOAD.FILES")); //$NON-NLS-1$
		useInternet.setSelected(DataExtractionSettings.getSettings().useInternetToLoadImages());
		panel.add(useInternet);
		
//		supressWarning = new JCheckBox();
//		supressWarning.setText(Messages.getString("OsmExtractionPreferencesDialog.DUPLICATED.ID")); //$NON-NLS-1$
//		supressWarning.setSelected(DataExtractionSettings.getSettings().isSupressWarningsForDuplicatedId());
//		panel.add(supressWarning);
//		
//		loadWholeOsmInfo = new JCheckBox();
//		loadWholeOsmInfo.setText(Messages.getString("OsmExtractionPreferencesDialog.LOAD.WHOLE.OSM")); //$NON-NLS-1$
//		loadWholeOsmInfo.setSelected(DataExtractionSettings.getSettings().getLoadEntityInfo());
//		panel.add(loadWholeOsmInfo);
		
		panel.setMaximumSize(new Dimension(Short.MAX_VALUE, panel.getPreferredSize().height));
		
	}

	private void createNormalizingStreetSection(JPanel root) {
		JPanel panel = new JPanel();
		GridBagLayout l = new GridBagLayout();
		panel.setLayout(l);
		panel.setBorder(BorderFactory.createTitledBorder("Map creation parameters"));
		root.add(panel);

		JLabel label = new JLabel(Messages.getString("OsmExtractionPreferencesDialog.NAME.SUFFIXES")); //$NON-NLS-1$
		panel.add(label);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 0;
		l.setConstraints(label, constr);
		
		streetSuffixes = new JTextField();
		streetSuffixes.setText(DataExtractionSettings.getSettings().getSuffixesToNormalizeStreetsString());
		panel.add(streetSuffixes);
		constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 0;
		l.setConstraints(streetSuffixes, constr);
		
		label = new JLabel(Messages.getString("OsmExtractionPreferencesDialog.DEFAULT.SUFFIXES")); //$NON-NLS-1$
		panel.add(label);
		constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 1;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);
		
		streetDefaultSuffixes = new JTextField();
		streetDefaultSuffixes.setText(DataExtractionSettings.getSettings().getDefaultSuffixesToNormalizeStreetsString());
		panel.add(streetDefaultSuffixes);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 1;
		l.setConstraints(streetDefaultSuffixes, constr);
		
		label = new JLabel("Map zooms (specify zoom levels in binary map <= 4)"); 
		panel.add(label);
		constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 2;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);
		
		mapZooms = new JTextField();
		mapZooms.setText(DataExtractionSettings.getSettings().getMapZoomsValue());
		panel.add(mapZooms);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 2;
		l.setConstraints(mapZooms, constr);
		
		
		label = new JLabel("Rendering types (xml config to extract osm data) file path"); 
		panel.add(label);
		constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 3;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);
		
		renderingTypesFile = new JTextField();
		renderingTypesFile.setText(DataExtractionSettings.getSettings().getMapRenderingTypesFile());
		panel.add(renderingTypesFile);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 3;
		l.setConstraints(renderingTypesFile, constr);
		
		panel.setMaximumSize(new Dimension(Short.MAX_VALUE, panel.getPreferredSize().height));
	}

	private void addListeners(){
		okButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				saveProperties();
				setVisible(false);
			}
			
		});
		cancelButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
				
	}
	
	
	public void saveProperties(){
		DataExtractionSettings settings = DataExtractionSettings.getSettings();
		if(!settings.getSuffixesToNormalizeStreetsString().equals(streetSuffixes.getText())){
			settings.setSuffixesToNormalizeStreets(streetSuffixes.getText());
		}
		if(!settings.getDefaultSuffixesToNormalizeStreetsString().equals(streetDefaultSuffixes.getText())){
			settings.setDefaultSuffixesToNormalizeStreets(streetDefaultSuffixes.getText());
		}
		if(settings.useInternetToLoadImages() != useInternet.isSelected()){
			settings.setUseInterentToLoadImages(useInternet.isSelected());
		}
		
		if(!settings.getMapZoomsValue().equals(mapZooms.getText())){
			settings.setMapZooms(mapZooms.getText());
		}
		if(!settings.getMapRenderingTypesFile().equals(renderingTypesFile.getText())){
			settings.setMapRenderingTypesFile(renderingTypesFile.getText());
		}
//		if(settings.isSupressWarningsForDuplicatedId() != supressWarning.isSelected()){
//			settings.setSupressWarningsForDuplicatedId	(supressWarning.isSelected());
//		}
//		if(settings.getLoadEntityInfo() != loadWholeOsmInfo.isSelected()){
//			settings.setLoadEntityInfo(loadWholeOsmInfo.isSelected());
//		}
		
	}
	


}

