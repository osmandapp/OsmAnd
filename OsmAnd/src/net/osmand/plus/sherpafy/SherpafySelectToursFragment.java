package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public class SherpafySelectToursFragment extends SherlockListFragment {
	private static final int ACTION_DOWNLOAD = 5;
	OsmandApplication app;
	private SherpafyCustomization custom;

	public SherpafySelectToursFragment() {
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getSherlockActivity().getApplication();
		custom = (SherpafyCustomization) app.getAppCustomization();
		TourAdapter tourAdapter = new TourAdapter(custom.getTourInformations());
		setListAdapter(tourAdapter);
		setHasOptionsMenu(true);
	}
	

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		((TourViewActivity) getActivity()).selectMenu(getListAdapter().getItem(position));
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		com.actionbarsherlock.view.MenuItem menuItem = menu.add(0, ACTION_DOWNLOAD, 0, R.string.sherpafy_download_tours).setShowAsActionFlags(
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//		OsmandApplication app = (OsmandApplication) getActivity().getApplication();
//		boolean light = true; //app.getSettings().isLightActionBar();
		//menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gdirections_light : R.drawable.ic_action_gdirections_dark);
		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				if (custom.getAccessCode().length() == 0) {
					openAccessCode(true);
				} else {
					startDownloadActivity();
				}
				return true;
			}
		});
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
//		getListView().setOnItemClickListener(new OnItemClickListener() {
//
//		    @Override
//		    public void onItemClick(AdapterView<?> parent, View view, int position,long arg3) {
//		    	view.findViewById(R.id.AreaPreview).setSelected(true);
//		    }
//		});
	}
	
	protected void openAccessCode(final boolean startDownload) {
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.enter_access_code);
		final EditText editText = new EditText(getActivity());
		editText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS);
		LinearLayout ll = new LinearLayout(getActivity());
		ll.setPadding(5, 3, 5, 0);
		ll.addView(editText, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		builder.setView(ll);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String acCode = editText.getText().toString();
				if (!custom.setAccessCode(acCode)) {
					Toast.makeText(getActivity(), R.string.access_code_is_not_valid, Toast.LENGTH_LONG).show();
					return;
				}
				if (startDownload) {
					startDownloadActivity();
				}
			}
		});
		builder.create().show();
	}
	
	private void startDownloadActivity() {
		final Intent download = new Intent(getActivity(), DownloadIndexActivity.class);
		download.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(download);
	}
	
	private void selectTourAsync(final TourInformation tour) {
		new AsyncTask<TourInformation, Void, Void>() {
			private ProgressDialog dlg;

			protected void onPreExecute() {
				dlg = new ProgressDialog(getActivity());
				dlg.setTitle(R.string.selecting_tour_progress);
				dlg.setMessage(getString(R.string.indexing_tour, tour == null ? "" : tour.getName()));
				dlg.show();
			};

			@Override
			protected Void doInBackground(TourInformation... params) {
				// if tour is already selected - do nothing
				if (custom.getSelectedTour() != null) {
					if (custom.getSelectedTour().equals(params[0])) {
						return null;
					}
				}
				custom.selectTour(params[0], IProgress.EMPTY_PROGRESS);
				return null;
			}

			protected void onPostExecute(Void result) {
				// to avoid illegal argument exception when rotating phone during loading
				try {
					dlg.dismiss();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				//startTourView();
			};
		}.execute(tour);
	}
	
	class TourAdapter extends ArrayAdapter<TourInformation> {

		public TourAdapter(List<TourInformation> list) {
			super(getActivity(), R.layout.sherpafy_list_tour_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.sherpafy_list_tour_item, parent, false);
			}
			final TourInformation ti = getItem(position);
			TextView description = (TextView) row.findViewById(R.id.TourDescription);
			TextView name = (TextView) row.findViewById(R.id.TourName);
			TextView moreInformation = (TextView) row.findViewById(R.id.MoreInformation);
			SpannableString content = new SpannableString(getString(R.string.sherpafy_more_information));
			content.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					((TourViewActivity) getActivity()).selectMenu( ti);
				}
			}, 0, content.length(), 0);
			moreInformation.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
			moreInformation.setText(content);
			moreInformation.setMovementMethod(LinkMovementMethod.getInstance());
			description.setText(ti.getShortDescription());
			name.setText(ti.getName());
			ImageView iv = (ImageView) row.findViewById(R.id.TourImage);
			if(ti.getImageBitmap() != null) {
				iv.setImageBitmap(ti.getImageBitmap());
			}
			return row;
		}
	}

}