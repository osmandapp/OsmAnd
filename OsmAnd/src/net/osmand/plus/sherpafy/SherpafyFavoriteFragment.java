package net.osmand.plus.sherpafy;

import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

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
			if (getSherlockActivity().getSupportActionBar() != null) {
				getSherlockActivity().getSupportActionBar().setTitle(fav.getName());
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		((TourViewActivity) getSherlockActivity()).createMenuItem(menu, SHOW_ON_MAP, R.string.show_poi_on_map,
				R.drawable.ic_action_map_marker_light, R.drawable.ic_action_map_marker_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT, new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						return onOptionsItemSelected(item);
					}
				});
	}

	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == SHOW_ON_MAP) {
			((TourViewActivity) getSherlockActivity()).goToMap(fav.location);
			return true;
		} else if (item.getItemId() == android.R.id.home) {
			((TourViewActivity) getSherlockActivity()).showSelectedItem();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}