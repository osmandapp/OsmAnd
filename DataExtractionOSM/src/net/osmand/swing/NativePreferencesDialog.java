package net.osmand.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class NativePreferencesDialog extends JDialog {
	
	private static final long serialVersionUID = -4862884032977071296L;
	
	private JButton okButton;
	private JButton cancelButton;

	private JTextField nativeFilesDirectory;
	private JTextField renderingStyleFile;
	private boolean okPressed;

	private JTextField renderingPropertiesTxt;
	private static String renderingProperties = "nightMode=false, appMode=default, noPolygons=false, hmRendered=false";

	
	public NativePreferencesDialog(Component parent){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	setTitle(Messages.getString("OsmExtractionPreferencesDialog.PREFERENCES")); //$NON-NLS-1$
        initDialog();
        
    }
	
	public void showDialog(){
		setSize(700, 380);
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
		GridBagLayout l = new GridBagLayout();
		panel.setLayout(l);
		panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("OsmExtractionPreferencesDialog.GENERAL"))); //$NON-NLS-1$
		root.add(panel);
		
		JLabel label = new JLabel("Directory with binary files : ");
		panel.add(label);
		GridBagConstraints constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 1;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);
		
		nativeFilesDirectory = new JTextField();
		
		nativeFilesDirectory.setText(DataExtractionSettings.getSettings().getBinaryFilesDir());
		panel.add(nativeFilesDirectory);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 1;
		l.setConstraints(nativeFilesDirectory, constr);
		
        
        label = new JLabel("Rendering style file : ");
        panel.add(label);
        constr = new GridBagConstraints();
        constr.ipadx = 5;
        constr.gridx = 0;
        constr.gridy = 3;
        constr.anchor = GridBagConstraints.WEST;
        l.setConstraints(label, constr);
        
        renderingStyleFile = new JTextField();
        renderingStyleFile.setText(DataExtractionSettings.getSettings().getRenderXmlPath());
        panel.add(renderingStyleFile);
        constr = new GridBagConstraints();
        constr.weightx = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.ipadx = 5;
        constr.gridx = 1;
        constr.gridy = 3;
        l.setConstraints(renderingStyleFile, constr);
        
        label = new JLabel("Rendering properties : ");
        panel.add(label);
        constr = new GridBagConstraints();
        constr.ipadx = 5;
        constr.gridx = 0;
        constr.gridy = 4;
        constr.anchor = GridBagConstraints.WEST;
        l.setConstraints(label, constr);
        
        renderingPropertiesTxt = new JTextField();
        renderingPropertiesTxt.setText(renderingProperties);
        panel.add(renderingPropertiesTxt);
        constr = new GridBagConstraints();
        constr.weightx = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.ipadx = 5;
        constr.gridx = 1;
        constr.gridy = 4;
        l.setConstraints(renderingPropertiesTxt, constr);
		
		panel.setMaximumSize(new Dimension(Short.MAX_VALUE, panel.getPreferredSize().height));
		
	}


	public static String getRenderingProperties() {
		return renderingProperties;
	}
	
	private void addListeners(){
		okButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				saveProperties();
				okPressed = true;
				renderingProperties = renderingPropertiesTxt.getText();
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
		if(!settings.getBinaryFilesDir().equals(nativeFilesDirectory.getText())){
			settings.setBinaryFilesDir(nativeFilesDirectory.getText());
		}
		
		if(!settings.getRenderXmlPath().equals(renderingStyleFile.getText())){
			settings.setRenderXmlPath(renderingStyleFile.getText());
		}
		
	}
	
	public boolean isOkPressed() {
		return okPressed;
	}

}

