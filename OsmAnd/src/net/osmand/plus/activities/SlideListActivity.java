package net.osmand.plus.activities;

import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SlideListActivity extends ListActivity {

	private String[] strings = new String[] {"1", "2", "3"};
	
	private ListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras(); 
		if(extras != null)
			strings = extras.getStringArray("Content");
		setListAdapter(new ArrayAdapter<String>(this, R.layout.slide_list_item, strings));
		list = getListView();
		list.setTextFilterEnabled(true);

		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
		    	Intent returnIntent = new Intent();
		    	returnIntent.putExtra("SelectedItem", position);
		    	setResult(RESULT_OK, returnIntent);    	
		    	finish();
			}
		});

		list.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				int x = (int) e.getX();
				int y = (int) e.getY();
				int pos = list.pointToPosition(x, y);
				if (pos >= 0) {
					if (pos != list.getSelectedItemPosition()) {
						list.setSelection(pos);
						list.requestFocusFromTouch();
						list.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
					}
					if (e.getAction() == MotionEvent.ACTION_UP) {
						list.performItemClick(list, pos, list.getSelectedItemId());
					}
				}
				return true;
			}
		});
	}	

}
