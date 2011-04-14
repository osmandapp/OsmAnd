package net.osmand.plus.activities;

import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContributionVersionActivity extends ListActivity {

	private static ContributionVersionActivityThread thread = new ContributionVersionActivityThread();
	private static final int DOWNLOAD_BUILDS_LIST = 1;
	private static final String URL_TO_RETRIEVE_BUILDS = "http://download.osmand.net/builds.php";
	private static final String CONTRIBUTION_INSTALL_APP_DATE = "CONTRIBUTION_INSTALL_APP_DATE"; 
	private ProgressDialog progressFileDlg;
	private Date currentInstalledDate;
	
	private List<OsmAndBuild> downloadedBuilds = new ArrayList<OsmAndBuild>();
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.download_builds);
		
		String installDate = OsmandSettings.getPrefs(this).getString(CONTRIBUTION_INSTALL_APP_DATE, null);
		if(installDate != null){
			try {
				currentInstalledDate = dateFormat.parse(installDate);
			} catch (ParseException e) {
			}
		}
		
		downloadedBuilds.clear();
		startThreadOperation(DOWNLOAD_BUILDS_LIST, getString(R.string.loading_builds));
	}

	private void startThreadOperation(int operationId, String message) {
		progressFileDlg = ProgressDialog.show(this, getString(R.string.loading), message);
//		progressFileDlg.setCancelable(false);
		if(thread.getState() == Thread.State.TERMINATED || thread.getOperationId() != operationId){
			thread = new ContributionVersionActivityThread();
			thread.setOperationId(operationId);
		}
		thread.setActivity(this);
		if(thread.getState() == Thread.State.NEW){
			thread.start();
		}
	}
	
	protected void endThreadOperation(int operationId, Exception e){
		if(progressFileDlg != null){
			progressFileDlg.dismiss();
			progressFileDlg = null;
		}
		if(operationId == DOWNLOAD_BUILDS_LIST){
			if(e != null){
				Toast.makeText(this, R.string.loading_builds_failed + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
				finish();
			} else {
				setListAdapter(new OsmandBuildsAdapter(downloadedBuilds));
			}
		}
	}
	
	protected void executeThreadOperation(int operationId) throws Exception {
		if(operationId == DOWNLOAD_BUILDS_LIST){
			URLConnection connection = new URL(URL_TO_RETRIEVE_BUILDS).openConnection();
			XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setInput(connection.getInputStream(), "UTF-8");
			int next;
			while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if(next == XmlPullParser.START_TAG && parser.getName().equals("build")) { //$NON-NLS-1$
					if ("osmand".equalsIgnoreCase(parser.getAttributeValue(null, "type"))) {

						String path = parser.getAttributeValue(null, "path"); //$NON-NLS-1$
						String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
						String date = parser.getAttributeValue(null, "date"); //$NON-NLS-1$
						String tag = parser.getAttributeValue(null, "tag"); //$NON-NLS-1$
						Date d = null;
						if (date != null) {
							try {
								d = dateFormat.parse(date);
							} catch (ParseException e) {
							}
						}
						OsmAndBuild build = new OsmAndBuild(path, size, d, tag);
						downloadedBuilds.add(build);
					}
				}
			}
			
			
		}
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		OsmAndBuild item = (OsmAndBuild) getListAdapter().getItem(position);
		// TODO
		Toast.makeText(this, "Install " + item.path, Toast.LENGTH_LONG).show();
		OsmandSettings.getPrefs(this).edit().putString(CONTRIBUTION_INSTALL_APP_DATE, dateFormat.format(item.date)).commit();
		currentInstalledDate = item.date;
		getListAdapter().notifyDataSetInvalidated();
		
	}
	
	@Override
	public OsmandBuildsAdapter getListAdapter() {
		return (OsmandBuildsAdapter) super.getListAdapter();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		thread.setActivity(null);
		if(progressFileDlg != null){
			progressFileDlg.dismiss();
			progressFileDlg = null;
		}
	}
	
	
	protected class OsmandBuildsAdapter extends ArrayAdapter<OsmAndBuild> implements Filterable {
		

		public OsmandBuildsAdapter(List<OsmAndBuild> builds) {
			super(ContributionVersionActivity.this, R.layout.download_build_list_item, builds);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.download_build_list_item, parent, false);
			}
			final View row = v;
			OsmAndBuild build = getItem(position);
			TextView tagView = (TextView) row.findViewById(R.id.download_tag);
			tagView.setText(build.tag);
			
			TextView description = (TextView) row.findViewById(R.id.download_descr);
			StringBuilder format = new StringBuilder();
			format.append(dateFormat.format(build.date))/*.append(" : ").append(build.size).append(" MB")*/;
			description.setText(format.toString());

			if(currentInstalledDate != null){
				if(currentInstalledDate.after(build.date)){
					tagView.setTextColor(Color.BLUE);
				} else {
					tagView.setTextColor(Color.GREEN);
				}
			} else {
				tagView.setTextColor(Color.WHITE);
			}
			return row;
		}
		
	}
	
	
	private static class ContributionVersionActivityThread extends Thread {
		private ContributionVersionActivity activity;
		private int operationId;
		
		public void setActivity(ContributionVersionActivity activity) {
			this.activity = activity;
		}
		
		public int getOperationId() {
			return operationId;
		}
		
		public void setOperationId(int operationId) {
			this.operationId = operationId;
		}

		@Override
		public void run() {
			Exception ex= null;
			try {
				if(this.activity != null){
					this.activity.executeThreadOperation(operationId);
				}
			} catch (Exception e) {
				ex = e;
			}
			final Exception e = ex;
			if(this.activity != null){
				this.activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.endThreadOperation(operationId, e);
					}
				});
			}
		}
	}
	
	private static class OsmAndBuild {
		public String path;
		public String size;
		public Date date;
		public String tag;
		
		public OsmAndBuild(String path, String size, Date date, String tag) {
			super();
			this.path = path;
			this.size = size;
			this.date = date;
			this.tag = tag;
		}
		
		
	}

}
