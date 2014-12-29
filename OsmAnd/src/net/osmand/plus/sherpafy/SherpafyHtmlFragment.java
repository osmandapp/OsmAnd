package net.osmand.plus.sherpafy;

import android.support.v4.app.Fragment;
import android.view.MenuItem;
import net.osmand.plus.OsmandApplication;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class SherpafyHtmlFragment extends Fragment {
	public static final String HTML = "HTML";
	public static final String TITLE = "TITLE";
	OsmandApplication app;
	private WebView wv;

	public SherpafyHtmlFragment() {
	}
	

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container,
			Bundle savedInstanceState) {
		app = (OsmandApplication) getActivity().getApplication();
		wv = new WebView(getActivity());
		WebSettings settings = wv.getSettings();
		settings.setDefaultTextEncodingName("utf-8");
		setHasOptionsMenu(true);
		return wv;
	}
	
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String data = getArguments().getString(HTML);
		String tl = getArguments().getString(TITLE);
		if(tl != null){
			getActivity().getActionBar().setTitle(tl);
		}
		wv.loadData("<html><body>"+data+"</body></html", "text/html; charset=utf-8", "utf-8");
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			((TourViewActivity) getActivity()).showSelectedItem();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}