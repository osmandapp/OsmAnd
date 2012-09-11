package net.osmand.plus.osmedit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;

public class OsmBugsRemoteUtil implements OsmBugsUtil {

	private static final Log log = LogUtil.getLog(OsmBugsRemoteUtil.class);

	private final static String SITE_API = "http://openstreetbugs.schokokeks.org/api/0.1/"; //$NON-NLS-1$

	@Override
	public boolean createNewBug(double latitude, double longitude, String text, String authorName){
		StringBuilder b = new StringBuilder();
		b.append(SITE_API).append("addPOIexec?"); //$NON-NLS-1$
		b.append("lat=").append(latitude); //$NON-NLS-1$
		b.append("&lon=").append(longitude); //$NON-NLS-1$
		text = text + " [" + authorName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		b.append("&text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
		b.append("&name=").append(URLEncoder.encode(authorName)); //$NON-NLS-1$
		return editingPOI(b.toString(), "creating bug"); //$NON-NLS-1$
	}

	@Override
	public boolean addingComment(long id, String text, String authorName){
		StringBuilder b = new StringBuilder();
		b.append(SITE_API).append("editPOIexec?"); //$NON-NLS-1$
		b.append("id=").append(id); //$NON-NLS-1$
		text = text + " [" + authorName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		b.append("&text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
		b.append("&name=").append(URLEncoder.encode(authorName)); //$NON-NLS-1$
		return editingPOI(b.toString(), "adding comment"); //$NON-NLS-1$
	}

	@Override
	public boolean closingBug(long id, String text, String authorName){
		StringBuilder b = new StringBuilder();
		b.append(SITE_API).append("closePOIexec?"); //$NON-NLS-1$
		b.append("id=").append(id); //$NON-NLS-1$
		if(text != null) {
			b.append("&text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
		}
		b.append("&name=").append(URLEncoder.encode(authorName)); //$NON-NLS-1$
		
		return editingPOI(b.toString(),"closing bug"); //$NON-NLS-1$
	}

	private boolean editingPOI(String urlStr, String debugAction){
		try {
			log.debug("Action " + debugAction + " " + urlStr); //$NON-NLS-1$ //$NON-NLS-2$
			URL url = new URL(urlStr);
			URLConnection connection = url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while(reader.readLine() != null){
			}
			log.debug("Action " + debugAction + " successfull"); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} catch (IOException e) {
			log.error("Error " +debugAction, e); //$NON-NLS-1$
		} catch (RuntimeException e) {
			log.error("Error "+debugAction, e); //$NON-NLS-1$
		}
		return false;
	}

}
