package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SherpafyTourOverviewFragment extends SherlockListFragment {
	private static final int SHARE_ID = 6;
	OsmandApplication app;
	private SherpafyCustomization customization;
	private TourInformation item;

	public SherpafyTourOverviewFragment() {
	}
	
	public void setTour(TourInformation item) {
		this.item = item;
	}
	
	private enum StageItemType {
		OVERVIEW,
		INSTRUCTIONS,
		GALLERY,
		STAGE,
		TEXT
	}
	
	private static class StageItem {
		
		boolean stage;
		String txt;
		Object type;

		public StageItem(Object type, String txt, boolean stage) {
			this.type = type;
			this.txt = txt;
			this.stage = stage;
		}
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getSherlockActivity().getApplication();
		customization = (SherpafyCustomization) app.getAppCustomization();
		List<StageItem> items = new ArrayList<SherpafyTourOverviewFragment.StageItem>();
		items.add(new StageItem(StageItemType.TEXT, getString(R.string.sherpafy_tour_info_txt), false));
		items.add(new StageItem(StageItemType.OVERVIEW, getString(R.string.sherpafy_overview), false));
		items.add(new StageItem(StageItemType.INSTRUCTIONS, getString(R.string.sherpafy_instructions), false));
		items.add(new StageItem(StageItemType.GALLERY, getString(R.string.sherpafy_gallery), false));
		items.add(new StageItem(StageItemType.TEXT, getString(R.string.sherpafy_stages_txt), true));
		for(StageInformation si : item.getStageInformation()) {
			StageItem it = new StageItem(si, si.getName(), true);
			items.add(it);
		}
		StageAdapter stageAdapter = new StageAdapter(items);
		getSherlockActivity().getSupportActionBar().setTitle(item.getName());
		ImageView iv = new ImageView(getActivity());
		iv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		iv.setScaleType(ScaleType.CENTER_CROP);
		iv.setImageBitmap(item.getImageBitmap());
		getListView().addHeaderView(iv);
		setListAdapter(stageAdapter);
		setHasOptionsMenu(true);
	}
	

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Toast.makeText(getActivity(), getListAdapter().getItem(position).toString(), Toast.LENGTH_LONG).show();
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		createMenuItem(menu, ACTION_GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
//		MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		((TourViewActivity) getSherlockActivity()).createMenuItem(menu, SHARE_ID, R.string.settings, R.drawable.ic_action_gshare_light,
		R.drawable.ic_action_gshare_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == SHARE_ID) {
			ShareDialog sd = new ShareDialog(getActivity());
			if(this.item.getHomeUrl().equals("")) {
				sd.shareURLOrText(null, this.item.getName(), null);
			} else {
				sd.shareURLOrText(this.item.getHomeUrl(), this.item.getName() + " " + this.item.getHomeUrl(), null);
			}
			sd.showDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	
	
	
	class StageAdapter extends ArrayAdapter<StageItem> {

		public StageAdapter(List<StageItem> list) {
			super(getActivity(), R.layout.sherpafy_stage_list_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.sherpafy_stage_list_item, parent, false);
			}
			StageItem ti = getItem(position);
			TextView header = (TextView) row.findViewById(R.id.HeaderText);
			ImageView img = (ImageView) row.findViewById(R.id.Icon);
			TextView text = (TextView) row.findViewById(R.id.Text);
			TextView addtext = (TextView) row.findViewById(R.id.AdditionalText);
			
			if(ti.type instanceof StageInformation) {
				// TODO !!
				addtext.setText("10 km");
			} else {
				addtext.setText("");
			}
			if(ti.type == StageItemType.TEXT){
				text.setTextColor(ti.stage ? StageImageDrawable.STAGE_COLOR :
						StageImageDrawable.INFO_COLOR);
				text.setText(ti.txt);
				img.setImageDrawable(null);
				header.setVisibility(View.GONE);
			} else if(ti.type instanceof StageInformation) {
				StageInformation si = (StageInformation) ti.type;
				header.setVisibility(View.VISIBLE);
				text.setTextColor(StageImageDrawable.MENU_COLOR);
				text.setText(ti.txt);
				img.setImageDrawable(new StageImageDrawable(getActivity(), 
					StageImageDrawable.STAGE_COLOR, (si.getOrder() + 1) +"", 0 ));
			} else {
				header.setVisibility(View.VISIBLE);
				text.setTextColor(StageImageDrawable.MENU_COLOR);
				text.setText(ti.txt);
				img.setImageDrawable(new StageImageDrawable(getActivity(), 
					StageImageDrawable.INFO_COLOR, ti.txt.substring(0, 1) +"", 0 ));
			}
			return row;
		}
	}
	

	private ImageGetter getImageGetter(final View v) {
		return new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String s) {
				Bitmap file = customization.getSelectedTour().getImageBitmapFromPath(s);
				v.setTag(file);
				Drawable bmp = new BitmapDrawable(getResources(), file);
				// if image is thicker than screen - it may cause some problems, so we need to scale it
				int imagewidth = bmp.getIntrinsicWidth();
				// TODO
//				if (displaySize.x - 1 > imagewidth) {
//					bmp.setBounds(0, 0, bmp.getIntrinsicWidth(), bmp.getIntrinsicHeight());
//				} else {
//					double scale = (double) (displaySize.x - 1) / imagewidth;
//					bmp.setBounds(0, 0, (int) (scale * bmp.getIntrinsicWidth()),
//							(int) (scale * bmp.getIntrinsicHeight()));
//				}
				return bmp;
			}

		};
	}
	


	private void addOnClickListener(final TextView tv) {
		tv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.getTag() instanceof Bitmap) {
					final AccessibleAlertBuilder dlg = new AccessibleAlertBuilder(getActivity());
					dlg.setPositiveButton(R.string.default_buttons_ok, null);
					ScrollView sv = new ScrollView(getActivity());
					ImageView img = new ImageView(getActivity());
					img.setImageBitmap((Bitmap) tv.getTag());
					sv.addView(img);
					dlg.setView(sv);
					dlg.show();
				}
			}
		});
	}

	private void prepareBitmap(Bitmap imageBitmap) {
		ImageView img = null;
		if (imageBitmap != null) {
			img.setImageBitmap(imageBitmap);
			img.setAdjustViewBounds(true);
			img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			img.setCropToPadding(true);
			img.setVisibility(View.VISIBLE);
		} else {
			img.setVisibility(View.GONE);
		}
	}

	private void goToMap() {
		if (customization.getSelectedStage() != null) {
			GPXFile gpx = customization.getSelectedStage().getGpx();
			List<SelectedGpxFile> sgpx = getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
			if (gpx == null && sgpx.size() > 0) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
			} else if (sgpx.size() != 1 || sgpx.get(0).getGpxFile() != gpx) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				if (gpx != null && gpx.findPointToShow() != null) {
					WptPt p = gpx.findPointToShow();
					getMyApplication().getSettings().setMapLocationToShow(p.lat, p.lon, 16, null);
					getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
		}
		Intent newIntent = new Intent(getActivity(), customization.getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		this.startActivityForResult(newIntent, 0);
	}
	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
	
}