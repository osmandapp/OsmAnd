package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class SherpafySelectToursFragment extends OsmAndListFragment {
	private static final int ACTION_DOWNLOAD = 5;
	OsmandApplication app;
	private SherpafyCustomization custom;

	public SherpafySelectToursFragment() {
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getActivity().getApplication();
		custom = (SherpafyCustomization) app.getAppCustomization();
		setHasOptionsMenu(true);
		refreshAdapter();
	}
	
	public void refreshAdapter() {
		setListAdapter(new TourAdapter(custom.getTourInformations()));
	}
	

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		((TourViewActivity) getActivity()).selectMenu(getListAdapter().getItem(position));
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem menuItem = menu.add(0, ACTION_DOWNLOAD, 0, R.string.sherpafy_download_tours);
		MenuItemCompat.setShowAsAction(menuItem,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
//		boolean light = true; //app.getSettings().isLightActionBar();
		//menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gdirections_light : R.drawable.ic_action_gdirections_dark);
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				openAccessCode();
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
	
	protected void openAccessCode() {
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.enter_access_code);
		final EditText editText = new EditText(getActivity());
		editText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS);
		LinearLayout ll = new LinearLayout(getActivity());
		ll.setPadding(5, 3, 5, 0);
		ll.addView(editText, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		AndroidUtils.softKeyboardDelayed(editText);
		builder.setView(ll);
		builder.setNegativeButton(R.string.sherpafy_public_access, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				custom.setAccessCode("");
				((TourViewActivity) getActivity()).startDownloadActivity();
			}
		});
		
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String acCode = editText.getText().toString();
				if (!custom.setAccessCode(acCode)) {
					Toast.makeText(getActivity(), R.string.access_code_is_not_valid, Toast.LENGTH_LONG).show();
					return;
				}
				((TourViewActivity) getActivity()).startDownloadActivity();
			}
		});
		builder.create().show();
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
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					((TourViewActivity) getActivity()).selectMenu( ti);
				}
			});
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