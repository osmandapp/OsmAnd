package net.osmand.plus.dashboard;

import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashListFragment extends DashBaseFragment {
	public static final String TAG = "DASH_LIST_FRAGMENT";


	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_list_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.fav_text)).setTypeface(typeface);
		((TextView) view.findViewById(R.id.fav_text)).setText("Here will be text");
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_all)).setVisibility(View.GONE);
		return view;
	}

	@Override
	public void onOpenDash() {
		setupList();
	}

	public void setupList() {
		View mainView = getView();
		mainView.findViewById(R.id.main_fav).setVisibility(View.VISIBLE);
		LinearLayout lv = (LinearLayout) mainView.findViewById(R.id.items);
		lv.removeAllViews();
		ArrayAdapter<?> la = dashboard.getListAdapter();
		final OnItemClickListener onClick = dashboard.getListAdapterOnClickListener();
		for(int i = 0; i < la.getCount(); i++) {
			final int position = i;
			View v = la.getView(position, null, lv);
			if (onClick != null) {
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onClick.onItemClick(null, v, position, position);
					}
				});
			}
			lv.addView(v);
		}
	}


}
