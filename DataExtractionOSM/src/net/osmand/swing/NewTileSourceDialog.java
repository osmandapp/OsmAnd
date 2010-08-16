package net.osmand.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.osmand.Algoritms;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;


public class NewTileSourceDialog extends JDialog {
	
	private static final long serialVersionUID = -4862884032977071296L;
	private static final Log log = LogUtil.getLog(NewTileSourceDialog.class);
	
	private JButton okButton;
	private JButton cancelButton;

	private JTextField templateName;
	private JTextField templateUrl;


	public NewTileSourceDialog(Component parent){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	setTitle("Create new tile source");
        initDialog();
        
    }
	
	public void showDialog(){
		setSize(800, 200);
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
        
        
        createInputSourceSection(pane);
        pane.add(Box.createVerticalGlue());	
        
        FlowLayout l = new FlowLayout(FlowLayout.RIGHT);
        JPanel buttonsPane = new JPanel(l);
        okButton = new JButton("OK");
        buttonsPane.add(okButton);
        cancelButton = new JButton("Cancel");
        buttonsPane.add(cancelButton);
        
        buttonsPane.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) l.preferredLayoutSize(buttonsPane).getHeight()));
        pane.add(buttonsPane);
        
        addListeners();
	}
	
	
	private void createInputSourceSection(JPanel root) {
		JPanel panel = new JPanel();
		GridBagLayout l = new GridBagLayout();
		panel.setLayout(l);
		panel.setBorder(BorderFactory.createTitledBorder("Input source"));
		root.add(panel);
		

		JLabel label = new JLabel("Name of template : ");
		panel.add(label);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 0;
		l.setConstraints(label, constr);

		templateName = new JTextField();
		templateName.setText("Mapnik example");
		panel.add(templateName);
		constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 0;
		l.setConstraints(templateName, constr);

		label = new JLabel("Url template with placeholders {$x}, {$y}, {$z} : ");
		panel.add(label);
		constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.ipady = 10;
		constr.gridx = 0;
		constr.gridy = 1;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);

		templateUrl = new JTextField();
		// Give hint about wms 
		templateUrl.setText("http://tile.openstreetmap.org/{$z}/{$x}/{$y}.png");
		panel.add(templateUrl);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 1;
		l.setConstraints(templateUrl, constr);
		
		
		label = new JLabel("To add wms service you can use retiling pattern : ");
		panel.add(label);
		constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.ipady = 10;
		constr.gridx = 0;
		constr.gridy = 2;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);

		label = new JLabel();
		// Give hint about wms 
		label.setText("http://whoots.mapwarper.net/tms/{$z}/{$x}/{$y}/{layer}/http://path.to.wms.server");
		panel.add(label);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 2;
		l.setConstraints(label, constr);

		panel.setMaximumSize(new Dimension(Short.MAX_VALUE, panel.getPreferredSize().height));
	}

	private void addListeners(){
		okButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(okPressed()){
					setVisible(false);
				}
			}
			
		});
		cancelButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
				
	}
	
	
	public boolean okPressed(){
		if(Algoritms.isEmpty(templateName.getText())){
			JOptionPane.showMessageDialog(this, "Please specify template name" , "Error creating new tile source", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(Algoritms.isEmpty(templateUrl.getText())){
			JOptionPane.showMessageDialog(this, "Please specify template url" , "Error creating new tile source", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		String url = templateUrl.getText();
		if(url.indexOf("{$x}") == -1 || url.indexOf("{$y}") == -1 || url.indexOf("{$z}") == -1){
			JOptionPane.showMessageDialog(this, "Please specify all placeholders {$x}, {$y}, {$z} in url" , "Error creating new tile source", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		File tilesDirectory = DataExtractionSettings.getSettings().getTilesDirectory();
		if(tilesDirectory != null){
			File dir = new File(tilesDirectory, templateName.getText());
			if(dir.mkdirs()){
				try {
					FileOutputStream ous = new FileOutputStream(new File(dir, "url"));
					ous.write(url.getBytes("UTF-8"));
					ous.close();
				} catch (UnsupportedEncodingException e) {
					log.error("Error creating new tile source " + url, e);
				} catch (IOException e) {
					log.error("Error creating new tile source " + url, e);
				}
			}
		}
		
		return true;
	}
	


}

