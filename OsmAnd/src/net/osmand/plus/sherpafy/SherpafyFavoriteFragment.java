package net.osmand.plus.sherpafy;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;


public class SherpafyFavoriteFragment extends SherpafyStageInfoFragment {
	private static final int SHOW_ON_MAP = 10;
	public static final String FAV_PARAM = null;
	private StageFavorite fav;
	
	protected void updateView(WebView description, ImageView icon, TextView additional, TextView text, TextView header) {
		if (fav.getImage() != null) {
			icon.setImageBitmap(fav.getImage());
		} else {
			icon.setVisibility(View.GONE);
		}
		additional.setVisibility(View.GONE);
		header.setText(fav.getName());
		text.setText(fav.getShortDescription());
		description.loadData("<html><body>" + fav.getFullDescription() + "</body></html", "text/html; charset=utf-8",
				"utf-8");
	}

	@Override
	protected void extractArguments(Bundle args) {
		super.extractArguments(args);
		int k = args.getInt(FAV_PARAM);
		if (stage != null) {
			fav = (StageFavorite) stage.getFavorites().get(k);
//			if (getSherlockActivity().getSupportActionBar() != null) {
//				getSherlockActivity().getSupportActionBar().setTitle(fav.getName());
//			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		((TourViewActivity) getActivity()).createMenuItem(menu, SHOW_ON_MAP, R.string.shared_string_show_on_map,
				R.drawable.ic_show_on_map,
				MenuItemCompat.SHOW_AS_ACTION_IF_ROOM | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT, new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						return onOptionsItemSelected(item);
					}
				});
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SHOW_ON_MAP) {
			((TourViewActivity) getActivity()).goToMap(fav.location);
			return true;
		} else if (item.getItemId() == android.R.id.home) {
			((TourViewActivity) getActivity()).showSelectedItem();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}