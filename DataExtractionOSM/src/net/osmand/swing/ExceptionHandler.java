package net.osmand.swing;

import javax.swing.JOptionPane;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;


public class ExceptionHandler {
	private static final Log log = LogUtil.getLog(ExceptionHandler.class);
	
	public static void handle(Throwable e){
		handle("Error occurred", e);
	}
	
	public static void handle(String msg, Throwable e){
		if(e != null){
			log.error(msg, e);
		} else {
			log.error(msg);
		}
		if(e != null){
			e.printStackTrace();
		}
		if (OsmExtractionUI.MAIN_APP != null && OsmExtractionUI.MAIN_APP.getFrame() != null) {
			String text;
			String title;
			if (e != null) {
				text = e+" ";
				title = msg;
			} else {
				title = "Error occured";
				text = msg;
			}
			JOptionPane.showMessageDialog(OsmExtractionUI.MAIN_APP.getFrame(), text, title, JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void handle(String msg){
		handle(msg, null);
	}

}
