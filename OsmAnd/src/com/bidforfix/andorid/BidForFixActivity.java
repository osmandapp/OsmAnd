package com.bidforfix.andorid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.osmand.plus.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bidforfix.andorid.BidForFixHelper.BFFIssue;
import com.gafmedia.Graph.PieItem;
import com.gafmedia.Graph.View_PieChart;

public abstract class BidForFixActivity extends ListActivity {
	
	private static final int LOAD_ITEMS = 0;

	LoadItemsTask loadItemTask;

	private BidForFixHelper helper;
	
	public abstract BidForFixHelper getBidForFixHelper();
	
	private BidForFixHelper getHelper() {
		if (helper == null) {
			helper = getBidForFixHelper();
		}
		return helper;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(android.R.style.Theme_Light);
		super.onCreate(savedInstanceState);
		if (getHelper().isReloadNeeded()) {
			showDialog(LOAD_ITEMS);
		} else {
			setListAdapter(new BFFIssueArrayAdapter(this, getHelper().getList()));
		}
	}
	

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case LOAD_ITEMS:
			return new ProgressDialog(this);
		}
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case LOAD_ITEMS:
			dialog.setTitle("Loading");
			loadItemTask = new LoadItemsTask((ProgressDialog)dialog);
			loadItemTask.execute();
			return;
		}
		super.onPrepareDialog(id, dialog);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		BFFIssue issue = (BFFIssue) getListAdapter().getItem(position);
		String url = issue.getLink();  
		try {
		    Intent i = new Intent(Intent.ACTION_VIEW);  
		    i.setData(Uri.parse(url));  
		    startActivity(i);
		} catch (ActivityNotFoundException ex) {
			Toast.makeText(this, ex.getMessage() + " for " + url, Toast.LENGTH_LONG).show();
		}
		super.onListItemClick(l, v, position, id);
	}
	
	public class LoadItemsTask extends AsyncTask<Object, Integer, Integer> {

		private final ProgressDialog dialog;

		public LoadItemsTask(ProgressDialog dialog) {
			this.dialog = dialog;
			this.dialog.setIndeterminate(true);
		}
		
		@Override
		protected Integer doInBackground(Object... params) {
			getHelper().loadList();
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			//set the adapater (clear,set)
			setListAdapter(new BFFIssueArrayAdapter(BidForFixActivity.this, getHelper().getList()));
			dismissDialog(LOAD_ITEMS);
		}
	}
	
	public static class BFFIssueArrayAdapter extends ArrayAdapter<BFFIssue> {
		private final Activity context;
		private final List<BFFIssue> values;

		Random random = new Random();
		
		static class ViewHolder {
			public TextView text;
			public ImageView image;
			public TextView support;
			public TextView funded;
		}
		
		public BFFIssueArrayAdapter(Activity context, List<BFFIssue> values) {
			super(context, R.layout.list_item_bidforfix, values);
			this.context = context;
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				rowView = inflater.inflate(R.layout.list_item_bidforfix, null);
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.text = (TextView) rowView.findViewById(R.id.bffiText);
				viewHolder.support =  (TextView) rowView.findViewById(R.id.bffiSupport);
				viewHolder.funded = (TextView) rowView.findViewById(R.id.bffiFunded);
				viewHolder.image = (ImageView) rowView.findViewById(R.id.bffiGraph);
				rowView.setTag(viewHolder);
			}

			ViewHolder holder = (ViewHolder) rowView.getTag();
			BFFIssue s = values.get(position);
			holder.text.setText(s.getName());
			holder.support.setText("18 Supporters");
			int funded = random.nextInt(100);
			holder.funded.setText("funded: "+funded+"%");
			
			List<PieItem> PieData = new ArrayList<PieItem>();
        	PieItem item       = new PieItem();
        	item.Count = funded;
        	item.Color = 0xFF72B123; 
        	PieItem item2       = new PieItem();
        	item2.Count = 100-funded;
        	item2.Color = 0xFFDDDDDD;
        	
        	PieData.add(item);
        	PieData.add(item2);
        	
        	int Size = Math.max(holder.image.getHeight(),40);
        	
            Bitmap mBackgroundImage = Bitmap.createBitmap(Size, Size, Bitmap.Config.RGB_565);
            //------------------------------------------------------------------------------------------
            // Generating Pie view
            //------------------------------------------------------------------------------------------
            View_PieChart pieChartView = new View_PieChart(context);
            pieChartView.setLayoutParams(new LayoutParams(Size, Size));
            pieChartView.setGeometry(Size, Size, 2, 2, 2, 2, -1); //OverlayId);
            pieChartView.setSkinParams(0xffffffff);
            pieChartView.setData(PieData, 100);
            pieChartView.invalidate();
            //------------------------------------------------------------------------------------------
            // Draw Pie Vien on Bitmap canvas
            //------------------------------------------------------------------------------------------
            pieChartView.draw(new Canvas(mBackgroundImage));
            pieChartView = null;

            holder.image.setImageBitmap(mBackgroundImage);
        	//MaxCount += ItemCount;
//			if (s.startsWith("Windows7") || s.startsWith("iPhone")
//					|| s.startsWith("Solaris")) {
//				holder.image.setImageResource(R.drawable.no);
//			} else {
//				holder.image.setImageResource(R.drawable.ok);
//			}

			return rowView;
		}
	}
}
