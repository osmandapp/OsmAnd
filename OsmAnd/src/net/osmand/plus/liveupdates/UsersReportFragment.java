package net.osmand.plus.liveupdates;

import java.util.Arrays;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.liveupdates.Protocol.RankingUserByMonthResponse;
import net.osmand.plus.liveupdates.Protocol.UserRankingByMonth;
import net.osmand.plus.liveupdates.ReportsFragment.GetJsonAsyncTask;
import net.osmand.plus.liveupdates.ReportsFragment.GetJsonAsyncTask.OnResponseListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class UsersReportFragment extends BaseOsmAndDialogFragment {

	public static final String URL_REQUEST = "URL_REQUEST";
	public static final String REGION_NAME = "REGION_NAME";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_simple_list, container, false);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		final ArrayAdapter<UserRankingByMonth> adapter = new ListAdapter(getListItemIcon());
		String url = getArguments().getString(URL_REQUEST);
		//String reg = getArguments().getString(REGION_NAME);
		view.findViewById(R.id.progress).setVisibility(View.VISIBLE);
		((TextView)view.findViewById(R.id.titleTextView)).setText(R.string.osm_editors_ranking);
		GetJsonAsyncTask<RankingUserByMonthResponse> task = new GetJsonAsyncTask<>(RankingUserByMonthResponse.class);
		task.setOnResponseListener(new OnResponseListener<Protocol.RankingUserByMonthResponse>() {

			@Override
			public void onResponse(RankingUserByMonthResponse response) {
				if (response != null && response.rows != null) {
					for (UserRankingByMonth rankingByMonth : response.rows) {
						adapter.add(rankingByMonth);
					}
				}
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
		});
		task.execute(url);
		listView.setAdapter(adapter);
		
		ImageButton clearButton = (ImageButton) view.findViewById(R.id.closeButton);
		//setThemedDrawable(clearButton, R.drawable.ic_action_remove_dark);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		return view;
	}

	@DrawableRes
	protected int getListItemIcon() {
		return R.drawable.ic_person;
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	private class ListAdapter extends ArrayAdapter<UserRankingByMonth> {
		private final Drawable drawableLeft;
		@ColorInt
		private final int textColor;
		private final int textSecondaryColor;

		public ListAdapter(@DrawableRes int drawableLeftId) {
			super(getMyActivity(), android.R.layout.simple_list_item_2);
			this.drawableLeft = drawableLeftId == -1 ? null : getContentIcon(drawableLeftId);
			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = getActivity().getTheme();
			theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
			textColor = typedValue.data;
			theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
			textSecondaryColor = typedValue.data;
			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			UserRankingByMonth item = getItem(position);
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
			}
			TextView text1 = (TextView) v.findViewById(android.R.id.text1);
			TextView text2 = (TextView) v.findViewById(android.R.id.text2);
			text1.setText(item.user);
			text2.setText(getString(R.string.osm_user_stat,
					String.valueOf(item.changes), String.valueOf(item.rank), String.valueOf(item.globalchanges)));
			text1.setTextColor(textColor);
			text2.setTextColor(textSecondaryColor);
			text1.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
			text1.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.list_content_padding));
			return v;
		}
	}
}
