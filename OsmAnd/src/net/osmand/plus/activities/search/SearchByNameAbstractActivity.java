package net.osmand.plus.activities.search;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.TextView.OnEditorActionListener;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.activities.search.SearchAddressFragment.AddressInformation;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.dialogs.FavoriteDialogs;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressLint("NewApi")
public abstract class SearchByNameAbstractActivity<T> extends OsmandListActivity {

	private static final String ENDING_TEXT = "ending_text";
	private EditText searchText;
	private AsyncTask<Object, ?, ?> initializeTask;
	
	protected static final int MESSAGE_CLEAR_LIST = OsmAndConstants.UI_HANDLER_SEARCH + 2;
	protected static final int MESSAGE_ADD_ENTITY = OsmAndConstants.UI_HANDLER_SEARCH + 3;
	protected static final int MESSAGE_ADD_ENTITIES = OsmAndConstants.UI_HANDLER_SEARCH + 4;
	protected static final String SELECT_ADDRESS = "SEQUENTIAL_SEARCH";
	
	protected ProgressBar progress;
	protected LatLon locationToSearch;
	protected OsmandSettings settings;
	protected List<T> initialListToFilter = new ArrayList<>();
	protected Handler uiHandler;
	protected Collator collator;
	protected NamesFilter namesFilter;
	private String currentFilter = "";
	private boolean initFilter = false;
	private String endingText = "";
	private T endingObject;
	private StyleSpan previousSpan = new StyleSpan(Typeface.BOLD_ITALIC);
	private static final Log log = PlatformUtil.getLog(SearchByNameAbstractActivity.class);
	
	private static final int NAVIGATE_TO = 3;
	private static final int SHOW_ON_MAP = 5;
	private static final int ADD_TO_FAVORITE = 6;
	
	
	private void separateMethod() {
		getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = ((OsmandApplication) getApplication()).getSettings();
		setContentView(R.layout.search_by_name);
		

		initializeTask = getInitializeTask();
		uiHandler = new UIUpdateHandler();
		namesFilter = new NamesFilter();
		addFooterViews();
		final NamesAdapter namesAdapter = new NamesAdapter(new ArrayList<T>(), createComparator()); //$NON-NLS-1$
		setListAdapter(namesAdapter);
		
		collator = OsmAndCollator.primaryCollator();

		progress = (ProgressBar) findViewById(R.id.ProgressBar);
			
		searchText = (EditText) findViewById(R.id.SearchText);

		// ppenguin 2016-03-07: try to avoid full screen input in landscape mode (when softKB too large)
		searchText.setImeOptions(searchText.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);

		searchText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				String newFilter = s.toString();
				String newEndingText = endingText;
				if (newEndingText.length() > 0) {
					while(!newFilter.endsWith(newEndingText) && newEndingText.length() > 0) {
						newEndingText = newEndingText.substring(1);
					}
					newFilter = newFilter.substring(0, newFilter.length() - newEndingText.length());
				}
				updateTextBox(newFilter, newEndingText, endingObject, false);
				querySearch(newFilter);
				
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		// Not perfect
//		filter.setOnClickListener(new OnClickListener() {
//			}
//		});

        // ppenguin 2016-03-07: try to avoid full screen input in landscape mode (when softKB too large) => IME-flags necessary here too!
		searchText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
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
				reset();
			}
			
		});
		selectAddress = getIntent() != null && getIntent().hasExtra(SELECT_ADDRESS);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		if (initializeTask != null){
			initializeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}
	
	protected void reset() {
		searchText.setText("");
	}
	
	public String getLangPreferredName(MapObject mo) {
		return mo.getName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get());
	}
	
	protected void addFooterViews() {
	}
	
	public void setLabelText(int res) {
		getSupportActionBar().setSubtitle(getString(res));
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
	private boolean selectAddress;
	
	public String getCurrentFilter() {
		return currentFilter;
	}

	public void research() {
		initFilter = false;
		querySearch(currentFilter);
	}
	
	protected View getFooterView() {
		return null;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ENDING_TEXT, endingText);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle prevState) {
		endingText = prevState.getString(ENDING_TEXT);
		if(endingText == null) {
			endingText = "";
		}
		super.onRestoreInstanceState(prevState);
	}
	
	protected boolean isSelectAddres() {
		return selectAddress;
	}
	
	private void querySearch(final String filter) {
		if (!currentFilter.equals(filter) || !initFilter) {
			currentFilter = filter;
			initFilter = true;
			progress.setVisibility(View.VISIBLE);
			namesFilter.cancelPreviousFilter(filter);
			namesFilter.filter(filter);
		}
	}

	private void updateTextBox(String currentFilter, String locEndingText, T obj, boolean updateText) {
		String prevEndtext = endingText;
		endingText = locEndingText;
		endingObject = obj;
		if(updateText) {
			searchText.getText().replace(currentFilter.length(), currentFilter.length() + prevEndtext.length(), locEndingText);
		}

		searchText.getText().removeSpan(previousSpan);
		if (locEndingText.length() > 0) {
			searchText.getText().setSpan(previousSpan, currentFilter.length(), currentFilter.length() + locEndingText.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			if (searchText.getSelectionEnd() > currentFilter.length()) {
				searchText.setSelection(currentFilter.length());
			}
		}
	}
	
	protected void addObjectToInitialList(T initial){
		initialListToFilter.add(initial);
		if (!namesFilter.active) {
			if (filterObject(initial, currentFilter)) {
				Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, initial);
				msg.sendToTarget();
			}
		}
	}
	
	protected void finishInitializing(List<T> list){
		Comparator<? super T> cmp = createComparator();
		getListAdapter().sort(cmp);
		if (list != null) {
			Collections.sort(list,cmp);
			initialListToFilter = list;
		}
		research();
	}
	
	protected abstract Comparator<? super T> createComparator();
	
	public String getDistanceText(T obj) {
		return null;
	}

	public abstract String getText(T obj);
	
	public String getAdditionalFilterText(T obj) {
		return null;
	}
	
	public String getShortText(T obj) {
		return getText(obj);
	}
	public void itemSelectedBase(final T obj, View v) {
		itemSelected(obj);
	}
	public abstract void itemSelected(T obj);
	
	public boolean filterObject(T obj, String filter){
		if(filter == null || filter.length() == 0){
			return true;
		}
		boolean matches = CollatorStringMatcher.cmatches(collator, getText(obj), filter, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		if(!matches && getAdditionalFilterText(obj) != null) {
			matches = CollatorStringMatcher.cmatches(collator, getAdditionalFilterText(obj), filter, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		}
		return matches;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		selectAddress = getIntent() != null && getIntent().getBooleanExtra(SELECT_ADDRESS, false);
		setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				T repo = getListAdapter().getItem(position);
				itemSelectedBase(repo, view);
			}
		});
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
	
	protected boolean filterLoop(String query, Collection<T> list) {
		boolean result = false;
		for (T obj : list) {
			if (namesFilter.isCancelled){
				break;
			}
			if (filterObject(obj, query)){
				result = true;
				Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, obj);
				msg.sendToTarget();
			}
		}
		return result;
	}
	
	
	class UIUpdateHandler extends Handler {
		private Map<String, Integer> endingMap = new HashMap<>();
		private int minimalIndex = Integer.MAX_VALUE;
		private String minimalText = null;
		
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			String currentFilter = SearchByNameAbstractActivity.this.currentFilter;
			if (msg.what == MESSAGE_CLEAR_LIST) {
				minimalIndex = Integer.MAX_VALUE;
				minimalText = null;
				getListAdapter().clear();
				if (currentFilter.length() == 0) {
					endingMap.clear();
				}
				updateTextBox(currentFilter, "", null, true);
			} else if(msg.what == MESSAGE_ADD_ENTITY){
				final Object obj = msg.obj;
				addObjectToAdapter(currentFilter, (T) obj);
			} else if (msg.what == MESSAGE_ADD_ENTITIES) {
				final List<T> objects = (List<T>) msg.obj;
				for (T object : objects) {
					addObjectToAdapter(currentFilter, object);
				}
			}
		}

		private void addObjectToAdapter(String currentFilter, T obj) {
			getListAdapter().add(obj);
			if (currentFilter.length() > 0) {
				String shortText = getShortText(obj);
				int entries = !endingMap.containsKey(shortText) ? 0 : endingMap.get(shortText);
				if (entries < minimalIndex) {
					if(minimalText != null) {
						endingMap.put(minimalText, endingMap.get(minimalText) - 1);
					}
					minimalIndex = entries;
					minimalText = shortText;
					endingMap.put(shortText, entries + 1);
					String locEndingText;
					if (shortText.toLowerCase().startsWith(currentFilter.toLowerCase())) {
						locEndingText = shortText.substring(currentFilter.length());
					} else {
						locEndingText = " - " + shortText;
					}
					if (locEndingText.length() > MAX_VISIBLE_NAME) {
						locEndingText = locEndingText.substring(0, MAX_VISIBLE_NAME) + "..";
					}
					updateTextBox(currentFilter, locEndingText, obj, true);
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
			if (query.equals(newFilter)) {
				active = true;
				startTime = System.currentTimeMillis();
				uiHandler.sendEmptyMessage(MESSAGE_CLEAR_LIST);
				// make link copy
				Collection<T> list = initialListToFilter;
				filterLoop(query, list);
				active = false;
			}
			if (!isCancelled) {
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
			String distanceText = getDistanceText(getItem(position));
			String text = getText(getItem(position));
			if(distanceText == null) {
				label.setText(text);
			} else {
				label.setText(distanceText + " " + text, BufferType.SPANNABLE);
				((Spannable) label.getText()).setSpan(new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0,
						distanceText.length(), 0);
			}
			return row;
		}
	}
	
	protected void quitActivity(Class<? extends Activity> next) {
		finish();
		if(next != null) {
			Intent intent = new Intent(this, next);
			if(getIntent() != null){
				Intent cintent = getIntent();
				if(cintent.hasExtra(SearchActivity.SEARCH_LAT) && cintent.hasExtra(SearchActivity.SEARCH_LON)){
					intent.putExtra(SearchActivity.SEARCH_LAT, cintent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0));
					intent.putExtra(SearchActivity.SEARCH_LON, cintent.getDoubleExtra(SearchActivity.SEARCH_LON, 0));
				}
			}
			intent.putExtra(SELECT_ADDRESS, selectAddress);
			startActivity(intent);
		}
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 1) {
			finish();
			return true;
		} else {
			select(item.getItemId());
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!selectAddress && getAddressInformation() != null) {
			createMenuItem(menu, SHOW_ON_MAP, R.string.shared_string_show_on_map,
					R.drawable.ic_action_marker_dark, MenuItem.SHOW_AS_ACTION_ALWAYS);
		} else {
			createMenuItem(menu, 1, R.string.shared_string_ok,
					R.drawable.ic_action_done, MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	protected AddressInformation getAddressInformation() {
		return null;
	}

	protected void select(int mode) {
		LatLon searchPoint = settings.getLastSearchedPoint();
		AddressInformation ai = getAddressInformation();
		if (ai != null && searchPoint != null) {
			if (mode == ADD_TO_FAVORITE) {
				Bundle b = new Bundle();
				Dialog dlg = FavoriteDialogs.createAddFavouriteDialog(getActivity(), b);
				dlg.show();
				FavoriteDialogs.prepareAddFavouriteDialog(getActivity(), dlg, b, searchPoint.getLatitude(),
						searchPoint.getLongitude(), new PointDescription(PointDescription.POINT_TYPE_ADDRESS, ai.objectName));
			} else if (mode == NAVIGATE_TO) {
				DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), searchPoint.getLatitude(),
						searchPoint.getLongitude(), ai.getHistoryName());
			} else if (mode == SHOW_ON_MAP) {
				showOnMap(searchPoint, ai);
			}
		}
		
	}

	public void showOnMap(LatLon searchPoint, AddressInformation ai) {
		settings.setMapLocationToShow(searchPoint.getLatitude(), searchPoint.getLongitude(), ai.zoom,
				ai.getHistoryName());
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private Activity getActivity() {
		return this;
	}	
}
