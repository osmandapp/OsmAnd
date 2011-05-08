package net.osmand.plus.activities;

import java.util.List;

import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexInfo;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

public class LocalIndexesActivity extends ListActivity {

	private ProgressDialog progressDlg;
	private AsyncTask<Activity, ProgressDialog, List<LocalIndexInfo>> asyncLoader;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.local_index);

		progressDlg = new ProgressDialog(this);
		progressDlg.setTitle(getString(R.string.loading));
		progressDlg.setMessage(getString(R.string.loading));
		LoadLocalIndexTask task = new LoadLocalIndexTask();
		asyncLoader = task.execute(this);
	}
	
	private class LoadLocalIndexTask extends AsyncTask<Activity, ProgressDialog, List<LocalIndexInfo>> {

		@Override
		protected List<LocalIndexInfo> doInBackground(Activity... params) {
			LocalIndexHelper helper = new LocalIndexHelper((OsmandApplication) params[0].getApplication());
			return helper.getAllLocalIndexData();
		}
		
		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			setListAdapter(new LocalIndexesAdapter(result));
		}
		
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final LocalIndexInfo item = (LocalIndexInfo) getListAdapter().getItem(position);
		
	}
	
	@Override
	public LocalIndexesAdapter getListAdapter() {
		return (LocalIndexesAdapter) super.getListAdapter();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
			asyncLoader.cancel(true);
		}
	}
	
	
	protected class LocalIndexesAdapter extends ArrayAdapter<LocalIndexInfo> implements Filterable {
		

		public LocalIndexesAdapter(List<LocalIndexInfo> builds) {
			super(LocalIndexesActivity.this, R.layout.local_index_list_item, builds);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_index_list_item, parent, false);
			}
			final View row = v;
			LocalIndexInfo item = getItem(position);
			
			((TextView) v.findViewById(R.id.local_index_name)).setText(item.getName());

//			if(currentInstalledDate != null){
//				if(currentInstalledDate.before(build.date)){
//					tagView.setTextColor(Color.GREEN);
//				} else {
//					tagView.setTextColor(Color.BLUE);
//				}
//			} else {
//				tagView.setTextColor(Color.WHITE);
//			}
			return row;
		}
		
	}
	
	
	
	

}
