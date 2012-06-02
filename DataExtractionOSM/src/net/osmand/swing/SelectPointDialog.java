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

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.LatLon;

import org.apache.commons.logging.Log;


public class SelectPointDialog extends JDialog {
	
	private static final long serialVersionUID = -4862884032977071296L;
	private static final Log log = LogUtil.getLog(SelectPointDialog.class);
	
	private JButton okButton;
	private JButton cancelButton;

	private JTextField latPosition;
	private JTextField lonPosition;
	
	private LatLon result;


	public SelectPointDialog(Component parent, LatLon position){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	setTitle("Select point"); //$NON-NLS-1$
        initDialog(position);
        
    }
	
	public LatLon getResult() {
		return result;
	}
	
	public void showDialog(){
		setSize(800, 200);
        double x = getParent().getBounds().getCenterX();
        double y = getParent().getBounds().getCenterY();
        setLocation((int) x - getWidth() / 2, (int) y - getHeight() / 2);
        setVisible(true);
	}
	
	private void initDialog(LatLon position) {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(pane);
        
        
        createInputSourceSection(pane, position);
        pane.add(Box.createVerticalGlue());	
        
        FlowLayout l = new FlowLayout(FlowLayout.RIGHT);
        JPanel buttonsPane = new JPanel(l);
        okButton = new JButton("OK"); //$NON-NLS-1$
        buttonsPane.add(okButton);
        cancelButton = new JButton(Messages.getString("NewTileSourceDialog.CANCEL")); //$NON-NLS-1$
        buttonsPane.add(cancelButton);
        
        buttonsPane.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) l.preferredLayoutSize(buttonsPane).getHeight()));
        pane.add(buttonsPane);
        
        addListeners();
	}
	
	
	private void createInputSourceSection(JPanel root, LatLon position) {
		JPanel panel = new JPanel();
		GridBagLayout l = new GridBagLayout();
		panel.setLayout(l);
		panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("NewTileSourceDialog.INPUT.SOURCE"))); //$NON-NLS-1$
		root.add(panel);
		

		JLabel label = new JLabel("Latitude :"); //$NON-NLS-1$
		panel.add(label);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.ipadx = 5;
		constr.gridx = 0;
		constr.gridy = 0;
		l.setConstraints(label, constr);

		latPosition = new JTextField();
		latPosition.setText(((float)position.getLatitude())+""); //$NON-NLS-1$
		panel.add(latPosition);
		constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 0;
		l.setConstraints(latPosition, constr);

		label = new JLabel("Longitude :"); //$NON-NLS-1$
		panel.add(label);
		constr = new GridBagConstraints();
		constr.ipadx = 5;
		constr.ipady = 10;
		constr.gridx = 0;
		constr.gridy = 1;
		constr.anchor = GridBagConstraints.WEST;
		l.setConstraints(label, constr);

		lonPosition = new JTextField();
		// Give hint about wms 
		lonPosition.setText(((float)position.getLongitude())+""); //$NON-NLS-1$
		panel.add(lonPosition);
		constr = new GridBagConstraints();
		constr.weightx = 1;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.ipadx = 5;
		constr.gridx = 1;
		constr.gridy = 1;
		l.setConstraints(lonPosition, constr);
		
		
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
	
	private double parseDouble(String s) {
		if (Algoritms.isEmpty(s)) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}
	
	
	public boolean okPressed() {
		if (parseDouble(latPosition.getText()) == Double.NaN) {
			JOptionPane.showMessageDialog(this, "Lat coordinate is not a number", "Wrong position", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		if (parseDouble(lonPosition.getText()) == Double.NaN) {
			JOptionPane.showMessageDialog(this, "Lon coordinate is not a number", "Wrong position", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		result = new LatLon(parseDouble(latPosition.getText()), parseDouble(lonPosition.getText()));
		return true;

	}
}

