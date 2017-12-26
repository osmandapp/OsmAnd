package net.osmand.plus.liveupdates;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.liveupdates.Protocol.RankingUserByMonthResponse;
import net.osmand.plus.liveupdates.Protocol.UserRankingByMonth;
import net.osmand.plus.liveupdates.ReportsFragment.GetJsonAsyncTask;
import net.osmand.plus.liveupdates.ReportsFragment.GetJsonAsyncTask.OnResponseListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
		final ArrayAdapter<Object> adapter = new ListAdapter(getListItemIcon());
		String url = getArguments().getString(URL_REQUEST);
		//String reg = getArguments().getString(REGION_NAME);
		view.findViewById(R.id.progress).setVisibility(View.VISIBLE);
		if (getTag().equals(ReportsFragment.EDITS_FRAGMENT)) {
			((TextView) view.findViewById(R.id.titleTextView)).setText(R.string.osm_editors_ranking);
			GetJsonAsyncTask<RankingUserByMonthResponse> task = new GetJsonAsyncTask<>(RankingUserByMonthResponse.class);
			task.setOnResponseListener(new OnResponseListener<Protocol.RankingUserByMonthResponse>() {

				@Override
				public void onResponse(RankingUserByMonthResponse response) {
					if (response != null && response.rows != null) {
						for (UserRankingByMonth rankingByMonth : response.rows) {
							if (rankingByMonth != null) {
								adapter.add(rankingByMonth);
							}
						}
					}
					view.findViewById(R.id.progress).setVisibility(View.GONE);
				}
			});
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		} else if (getTag().equals(ReportsFragment.RECIPIENTS_FRAGMENT)) {
			((TextView)view.findViewById(R.id.titleTextView)).setText(R.string.osm_recipients_label);
			GetJsonAsyncTask<Protocol.RecipientsByMonth> task = new GetJsonAsyncTask<>(Protocol.RecipientsByMonth.class);
			task.setOnResponseListener(new OnResponseListener<Protocol.RecipientsByMonth>() {

				@Override
				public void onResponse(Protocol.RecipientsByMonth response) {
					if (response != null && response.rows != null) {
						for (Protocol.Recipient recipient : response.rows) {
							if (recipient != null) {
								adapter.add(recipient);
							}
						}
					}
					view.findViewById(R.id.progress).setVisibility(View.GONE);
				}
			});
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		}
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

	private class ListAdapter extends ArrayAdapter<Object> {
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
		public View getView(int position, View v, ViewGroup parent) {
			if (v == null) {
				v = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
			}
			TextView text1 = (TextView) v.findViewById(android.R.id.text1);
			TextView text2 = (TextView) v.findViewById(android.R.id.text2);
			text1.setTextColor(textColor);
			text2.setTextColor(textSecondaryColor);
			text1.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
			text1.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.list_content_padding));
			text2.setPadding(text1.getTotalPaddingLeft(), text1.getTotalPaddingTop(), text1.getTotalPaddingRight(), text1.getTotalPaddingBottom());
			Object item = getItem(position);
			if (item instanceof UserRankingByMonth) {
				UserRankingByMonth rankingByMonth = (UserRankingByMonth) item;
				text1.setText(rankingByMonth.user);
				text2.setText(getString(R.string.osm_user_stat,
						String.valueOf(rankingByMonth.changes), String.valueOf(rankingByMonth.rank), String.valueOf(rankingByMonth.globalchanges)));
			} else if (item instanceof Protocol.Recipient){
				Protocol.Recipient recipient = (Protocol.Recipient) item;
				text1.setText(recipient.osmid);
				text2.setText(getString(R.string.osm_recipient_stat,
						String.valueOf(recipient.changes), String.format("%.4f", (recipient.btc*1000f))));
			}
			return v;
		}
	}
}
