package net.osmand.plus.sherpafy;

import net.osmand.plus.OsmandApplication;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;

public class SherpafyHtmlFragment extends SherlockFragment {
	public static final String HTML = "HTML";
	public static final String TITLE = "TITLE";
	OsmandApplication app;
	private WebView wv;

	public SherpafyHtmlFragment() {
	}
	

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container,
			Bundle savedInstanceState) {
		app = (OsmandApplication) getSherlockActivity().getApplication();
		wv = new WebView(getActivity());
		return wv;
	}
	
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String data = getArguments().getString(HTML);
		String tl = getArguments().getString(TITLE);
		if(tl != null){
			getSherlockActivity().getSupportActionBar().setTitle(tl);
		}
		wv.loadData("<html><body>"+data+"</body></html", "text/html", "utf-8");
	}

}