package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


public abstract class SearchByNameAbstractActivity<T> extends ListActivity {

	private EditText searchText;
	private AsyncTask<Object, ?, ?> initializeTask;
	protected SearchByNameTask searchTask = new SearchByNameTask();
	
	protected ProgressBar progress;
	protected LatLon locationToSearch;
	protected OsmandSettings settings;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = OsmandSettings.getOsmandSettings(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.search_by_name);
		NamesAdapter namesAdapter = new NamesAdapter(new ArrayList<T>()); //$NON-NLS-1$
		setListAdapter(namesAdapter);
		progress = (ProgressBar) findViewById(R.id.ProgressBar);
		searchText = (EditText) findViewById(R.id.SearchText);
		searchText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				if(initializeTask.getStatus() == Status.FINISHED){
					setText(s.toString());
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
		});
		searchText.requestFocus();
		
		progress.setVisibility(View.INVISIBLE);
		findViewById(R.id.ResetButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				resetText();
			}
			
		});
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		
		initializeTask = getInitializeTask();
		if(initializeTask != null){
			initializeTask.execute();
		}
	}
	

	public AsyncTask<Object, ?, ?> getInitializeTask(){
		return null;
	}
	
	
	public boolean isFilterableByDefault(){
		return false;
	}
	
	public Editable getFilter(){
		return searchText.getText();
	}
	
	public void resetText(){
		setText("");
	}
	
	
	public void setText(final String filter) {
		if(isFilterableByDefault()){
			((NamesAdapter) getListAdapter()).getFilter().filter(filter);
			return;
		}
		((NamesAdapter) getListAdapter()).clear();
		Status status = searchTask.getStatus();
		if(status == Status.FINISHED){
			searchTask = new SearchByNameTask();
		} else if(status == Status.RUNNING){
			searchTask.cancel(true);
			// TODO improve
			searchTask = new SearchByNameTask();
		}
		searchTask.execute(filter);
	}
	

	public abstract List<T> getObjects(String filter, SearchByNameTask searchTask);

	public abstract void updateTextView(T obj, TextView txt);
	
	public abstract void itemSelected(T obj);
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		T repo = (T) getListAdapter().getItem(position);
		itemSelected(repo);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if(intent != null){
			if(intent.hasExtra(SearchActivity.SEARCH_LAT) && intent.hasExtra(SearchActivity.SEARCH_LON)){
				double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
				double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
				locationToSearch = new LatLon(lat, lon); 
			}
		}
		if(locationToSearch == null){
			locationToSearch = settings.getLastKnownMapLocation();
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		searchTask.cancel(true);
	}
	
	
	protected class SearchByNameTask extends AsyncTask<String, T, List<T>> {

		@Override
		protected List<T> doInBackground(String... params) {
			if(params == null || params.length == 0){
				return null;
			}
			String filter = params[0];
			return getObjects(filter, this);
		}
		
		public void progress(T... values){
			publishProgress(values);
		}
		
		protected void onProgressUpdate(T... values) {
			for(T t :values){
				((NamesAdapter) getListAdapter()).add(t);
			}
		};
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onPostExecute(List<T> result) {
			if (!isCancelled() && result != null) {
				((NamesAdapter) getListAdapter()).setNotifyOnChange(false);
				((NamesAdapter) getListAdapter()).clear();
				for (T o : result) {
					((NamesAdapter) getListAdapter()).add(o);
				}
				((NamesAdapter) getListAdapter()).notifyDataSetChanged();
			}
			if (!isCancelled()) {
				progress.setVisibility(View.INVISIBLE);
			}
		}
		
	}

	class NamesAdapter extends ArrayAdapter<T> {
		NamesAdapter(List<T> list) {
			super(SearchByNameAbstractActivity.this, R.layout.searchbyname_list, list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			if (convertView != null) {
				row = convertView;
			} else {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.searchbyname_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.NameLabel);
			updateTextView(getItem(position), label);
			return row;
		}
	}
}
