package net.osmand.activities.search;

import java.util.List;
import java.util.concurrent.Semaphore;

import net.osmand.R;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
	private Handler handlerToLoop;
	private ProgressBar progress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.search_by_name);
		NamesAdapter namesAdapter = new NamesAdapter(getObjects("")); //$NON-NLS-1$
		setListAdapter(namesAdapter);
		progress = (ProgressBar) findViewById(R.id.ProgressBar);
		searchText = (EditText) findViewById(R.id.SearchText);
		searchText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				setText(s.toString());
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
				searchText.setText(""); //$NON-NLS-1$
			}
			
		});
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
	
	public boolean isFilterableByDefault(){
		return false;
	}
	
	public Editable getFilter(){
		return searchText.getText();
	}
	
	protected void updateUIList(final List<T> objects){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				NamesAdapter namesAdapter = (NamesAdapter)getListAdapter();
				namesAdapter.setNotifyOnChange(false);
				namesAdapter.clear();
				for(T o : objects){
					namesAdapter.add(o);
				}
				namesAdapter.notifyDataSetChanged();
				progress.setVisibility(View.INVISIBLE);
			}
		});
	}
	
	public void setText(final String filter) {
		if(isFilterableByDefault()){
			((NamesAdapter) getListAdapter()).getFilter().filter(filter);
			return;
		}

		if(handlerToLoop == null){
			return;
		}
		handlerToLoop.removeMessages(1);
		Message msg = Message.obtain(handlerToLoop, new Runnable() {
			@Override
			public void run() {
				showProgress(View.VISIBLE);
				List<T> loadedObjects = getObjects(filter);
				updateUIList(loadedObjects);
				//because of incremental, wait for the ui update
				waitForUIThread();
			}
		});
		msg.what = 1;
		handlerToLoop.sendMessageDelayed(msg, 150);
	}

	private void waitForUIThread() {
		final Semaphore semafor = new Semaphore(1);
		try {
			semafor.acquire();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					semafor.release();
				}
			});
			semafor.acquire();
		} catch (InterruptedException e) {
		}
	}
	
	private void showProgress(final int v){
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				progress.setVisibility(v);
				
			}
			
		});
	}

	public abstract List<T> getObjects(String filter);

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
		synchronized (this) {
			if (handlerToLoop == null) {
				new Thread("Filter data") { //$NON-NLS-1$
					@Override
					public void run() {
						Looper.prepare();
						handlerToLoop = new Handler();
						Looper.loop();
					}
				}.start();
			}
			
		}
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		synchronized (this) {
			if(handlerToLoop != null){
				handlerToLoop.post(new Runnable(){
					@Override
					public void run() {
						Looper.myLooper().quit();
					}
				});
				handlerToLoop = null;
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
