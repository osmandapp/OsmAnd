package net.osmand.plus.activities.search;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.LogUtil;
import net.osmand.data.MapObject;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.CustomTitleBar;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.OsmandListActivity;

import org.apache.commons.logging.Log;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public abstract class SearchByNameAbstractActivity<T> extends OsmandListActivity {

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
	private boolean initFilter = false;
	private String endingText = "";
	private T endingObject;
	private StyleSpan previousSpan;
	private CustomTitleBar titleBar;
	private static final Log log = LogUtil.getLog(SearchByNameAbstractActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = ((OsmandApplication) getApplication()).getSettings();
		titleBar = new CustomTitleBar(this, R.string.search_activity, R.drawable.tab_search_address_icon);
		setContentView(R.layout.search_by_name);
		titleBar.afterSetContentView();

		initializeTask = getInitializeTask();
		uiHandler = new UIUpdateHandler();
		namesFilter = new NamesFilter();
		final NamesAdapter namesAdapter = new NamesAdapter(new ArrayList<T>(), createComparator()); //$NON-NLS-1$
		setListAdapter(namesAdapter);
		
		collator = Collator.getInstance(Locale.US);
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
		// Not perfect
//		searchText.setOnClickListener(new OnClickListener() {
//			String previousSelect = "";
//			@Override
//			public void onClick(View v) {
//				if(!previousSelect.equals(endingText) && endingText.length() > 0) {
//					previousSelect = endingText;
//					itemSelectedBase(endingObject, v);
//				}
//			}
//		});
		searchText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		searchText.requestFocus();
		searchText.setOnEditorActionListener(new OnEditorActionListener() {
	        @Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
	            	if(endingObject != null) {
	            		itemSelectedBase(endingObject, v);
	            	}
	            	return true;
	            }    
	            return false;
	        }
	    });
		
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
	
	public void setLabelText(int res) {
		titleBar.getTitleView().setText(res);
		//((TextView)findViewById(R.id.Label)).setText(res);
	}
	
	protected int getZoomToDisplay(T item){
		return 15;
	}
	
	protected LatLon getLocation(T item) {
		if (item instanceof MapObject) {
			return ((MapObject) item).getLocation();
		}
		return null;
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
	
	
	private int MAX_VISIBLE_NAME = 18;

	public void querySearch(final String filter) {
		String f = filter;
		boolean change = false;
		if (endingText.length() > 0) {
			while(!f.endsWith(endingText) && endingText.length() > 0) {
				endingText = endingText.substring(1);
				change = true;
			}
			f = f.substring(0, f.length() - endingText.length());
		}
		if (!currentFilter.equals(f) || !initFilter) {
			currentFilter = f;
			initFilter = true;
			progress.setVisibility(View.VISIBLE);
			namesFilter.cancelPreviousFilter(f);
			namesFilter.filter(f);
		}
		if(change) {
			if(previousSpan != null) {
				searchText.getText().removeSpan(previousSpan);
			}
			if (endingText.length() > 0) {
				previousSpan = new StyleSpan(Typeface.BOLD_ITALIC);
				searchText.getText().setSpan(previousSpan, currentFilter.length(), currentFilter.length() + endingText.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
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
		Comparator<? super T> cmp = createComparator();
		getListAdapter().sort(cmp);
		if(list != null){
			Collections.sort(list,cmp);
			initialListToFilter = list;
		}
		querySearch(searchText.getText().toString());
	}
	
	protected abstract Comparator<? super T> createComparator();

	public abstract String getText(T obj);
	
	public String getShortText(T obj) {
		return getText(obj);
	}
	public void itemSelectedBase(final T obj, View v) {
		itemSelected(obj);
//		LatLon loc = getLocation(obj);
//		if (obj != null && loc != null) {
//			QuickAction qa = new QuickAction(v);
//			ActionItem ai = new ActionItem();
//			ai.setTitle("Default");
//			ai.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					itemSelected(obj);
//				}
//			});
//			qa.addActionItem(ai);
//			// TODO more granular description and text message!
//			MapActivityActions.createDirectionsActions(qa, loc,
//					obj, getText(obj), getZoomToDisplay(endingObject),
//					SearchByNameAbstractActivity.this, true, null);
//			qa.show();
//		}
	}
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
		itemSelectedBase(repo, v);
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
	
	@SuppressWarnings("unchecked")
	@Override
	public NamesAdapter getListAdapter() {
		return (NamesAdapter) super.getListAdapter();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		namesFilter.cancelPreviousFilter(currentFilter);
	}
	
	
	protected void filterLoop(String query, Collection<T> list) {
		for (T obj : list) {
			if(namesFilter.isCancelled){
				break;
			}
			if(filterObject(obj, query)){
				Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, obj);
				msg.sendToTarget();
			}
		}
	}
	
	
	class UIUpdateHandler extends Handler {
		private Set<String> endingSet = new HashSet<String>();
		private boolean add = true;
		
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == MESSAGE_CLEAR_LIST){
				add = true;
				getListAdapter().clear();
				endingObject = null;
				if(currentFilter.length() == 0) {
					endingSet.clear();
				}
				String etext = endingText;
				endingText = "";
				if(previousSpan != null) {
					searchText.getText().removeSpan(previousSpan);
				}
				searchText.getText().replace(currentFilter.length(), currentFilter.length() + etext.length(), "");
				// searchText.setSelection(currentFilter.length());
			} else if(msg.what == MESSAGE_ADD_ENTITY){
				getListAdapter().add((T) msg.obj);
				if (add && currentFilter.length() > 0) {
					String text = getShortText((T) msg.obj);
					boolean newEntry = endingSet.add(text);
					if(text.toLowerCase().startsWith(currentFilter.toLowerCase())){
						text = text.substring(currentFilter.length());
					} else {
						text = " - " + text;
					}
					if (text.length() > MAX_VISIBLE_NAME) {
						text = text.substring(0, MAX_VISIBLE_NAME) + "..";
					}
					String etext = endingText;
					endingText = text;
					searchText.getText().replace(currentFilter.length(), currentFilter.length() + etext.length(), text);
					if(previousSpan != null) {
						searchText.getText().removeSpan(previousSpan);
					}
					previousSpan = new StyleSpan(Typeface.BOLD_ITALIC);
					searchText.getText().setSpan(previousSpan, currentFilter.length(),
							currentFilter.length() + text.length() , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					if(searchText.getSelectionEnd() > currentFilter.length()) {
						searchText.setSelection(currentFilter.length());
					}
					add = !newEntry;
					//endingButton.setText(text + "..");
					endingObject = (T) msg.obj;
				}
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
				Collection<T> list = initialListToFilter;
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
				log.debug("Search " + constraint + " finished in " + (System.currentTimeMillis() - startTime));
				progress.setVisibility(View.INVISIBLE);
			}
		}
		
	}

	protected class NamesAdapter extends ArrayAdapter<T> {
		
		NamesAdapter(List<T> list, Comparator<? super T> cmp) {
			super(SearchByNameAbstractActivity.this, R.layout.searchbyname_list, list);
			Collections.sort(list, cmp);
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
