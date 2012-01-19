package net.osmand.plus.activities.search;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Filter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


public abstract class SearchByNameAbstractActivity<T> extends ListActivity {

	private EditText searchText;
	private AsyncTask<Object, ?, ?> initializeTask;
	
	protected static final int MESSAGE_CLEAR_LIST = 1;
	protected static final int MESSAGE_ADD_ENTITY = 2;
	
	protected ProgressBar progress;
	protected LatLon locationToSearch;
	protected OsmandSettings settings;
	protected List<T> initialListToFilter = new ArrayList<T>();
	protected Handler uiHandler;
	protected Collator collator;
	protected NamesFilter namesFilter;
	private String currentFilter = "";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = OsmandSettings.getOsmandSettings(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_by_name);

		initializeTask = getInitializeTask();
		uiHandler = new UIUpdateHandler();
		namesFilter = new NamesFilter();
		NamesAdapter namesAdapter = new NamesAdapter(new ArrayList<T>()); //$NON-NLS-1$
		setListAdapter(namesAdapter);
		
		collator = Collator.getInstance();
 	    collator.setStrength(Collator.PRIMARY); //ignores also case
 	    
		
		progress = (ProgressBar) findViewById(R.id.ProgressBar);
		searchText = (EditText) findViewById(R.id.SearchText);
		searchText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				querySearch(s.toString());
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
				searchText.setText("");
			}
			
		});
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		if(initializeTask != null){
			initializeTask.execute();
		}
	}
	

	public AsyncTask<Object, ?, ?> getInitializeTask(){
		return null;
	}
	
	public Editable getFilter(){
		return searchText.getText();
	}
	
	public boolean initializeTaskIsFinished(){
		return initializeTask == null || initializeTask.getStatus() == Status.FINISHED;
	}
	
	
	public void querySearch(final String filter) {
		currentFilter = filter;
		progress.setVisibility(View.VISIBLE);
		namesFilter.cancelPreviousFilter(filter);
		namesFilter.filter(filter);
	}
	
	protected void addObjectToInitialList(T initial){
		initialListToFilter.add(initial);
		if(!namesFilter.active){
			if(filterObject(initial, currentFilter)) {
				Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, initial);
				msg.sendToTarget();
			}
		}
	}
	
	protected void finishInitializing(List<T> list){
		if(list != null){
			initialListToFilter = list;
		}
		querySearch(searchText.getText().toString());
	}
	

	public abstract String getText(T obj);
	
	public abstract void itemSelected(T obj);
	
	public boolean filterObject(T obj, String filter){
		if(filter == null || filter.length() == 0){
			return true;
		}
		return CollatorStringMatcher.cmatches(collator, getText(obj), filter, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		T repo = getListAdapter().getItem(position);
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
	public NamesAdapter getListAdapter() {
		return (NamesAdapter) super.getListAdapter();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		namesFilter.cancelPreviousFilter(currentFilter);
	}
	
	
	protected void filterLoop(String query, List<T> list) {
		for (int i = 0; i < list.size(); i++) {
			if(namesFilter.isCancelled){
				break;
			}
			T obj = list.get(i);
			if(filterObject(obj, query)){
				Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, obj);
				msg.sendToTarget();
			}
		}
	}
	
	
	class UIUpdateHandler extends Handler {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == MESSAGE_CLEAR_LIST){
				getListAdapter().clear();
			} else if(msg.what == MESSAGE_ADD_ENTITY){
				getListAdapter().add((T) msg.obj);
			}
		}
	}
	
	class NamesFilter extends Filter {
		
		protected boolean isCancelled = false;
		private String newFilter;
		private boolean active = false;
		private long startTime;
		
		protected void cancelPreviousFilter(String newFilter){
			this.newFilter = newFilter;
			isCancelled = true;
		}
		
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			isCancelled = false;
			String query = constraint.toString();
			if(query.equals(newFilter)){
				active = true;
				startTime = System.currentTimeMillis();
				uiHandler.sendEmptyMessage(MESSAGE_CLEAR_LIST);
				// make link copy
				List<T> list = initialListToFilter;
				filterLoop(query, list);
				active = false;
			}
			if(!isCancelled){
				return new FilterResults();
			}
			
			return null;
		}



		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if(results != null && initializeTaskIsFinished()){
				System.out.println("Search " + constraint + " finished in " + (System.currentTimeMillis() - startTime));
				progress.setVisibility(View.INVISIBLE);
			}
		}
		
	}

	protected class NamesAdapter extends ArrayAdapter<T> {
		NamesAdapter(List<T> list) {
			super(SearchByNameAbstractActivity.this, R.layout.searchbyname_list, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			if (convertView != null) {
				row = convertView;
			} else {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.searchbyname_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.NameLabel);
			label.setText(getText(getItem(position)));
			return row;
		}
	}
}
