package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class WebImageMenuFragment extends Fragment {
	public static final String TAG = "WebImageMenuFragment";

	private WebImageMenu menu;
	private View view;
	private LinearLayout viewerLayout;
	private View contentView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && getActivity() instanceof MapActivity) {
			menu = WebImageMenu.restoreMenu(savedInstanceState, (MapActivity) getActivity());
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.map_image_view, container, false);
		viewerLayout = (LinearLayout) view.findViewById(R.id.image_viewer);
		contentView = menu.getContentView();
		if (contentView != null) {
			viewerLayout.addView(contentView);
		}
		view.findViewById(R.id.image_close_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		if (!Algorithms.isEmpty(menu.getTitle())) {
			((TextView) view.findViewById(R.id.image_title)).setText(menu.getTitle());
		}
		if (!Algorithms.isEmpty(menu.getDescription())) {
			((TextView) view.findViewById(R.id.image_description)).setText(menu.getDescription());
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (menu != null) {
			menu.onResume();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (menu != null) {
			menu.onPause();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (viewerLayout != null && contentView != null) {
			viewerLayout.removeView(contentView);
			if (contentView instanceof WebView) {
				((WebView) contentView).loadUrl("about:blank");
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		menu.saveMenu(outState);
	}

	public static void showInstance(WebImageMenu menu) {
		WebImageMenuFragment fragment = new WebImageMenuFragment();
		fragment.menu = menu;
		menu.getMapActivity().getSupportFragmentManager().beginTransaction()
				.replace(R.id.topFragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commit();
	}

	public void dismiss() {
		MapActivity activity = menu.getMapActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG,
						FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
