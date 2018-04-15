package net.osmand.plus.wikivoyage;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.WikivoyageShowImages;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;

public class WikivoyageShowPicturesDialogFragment  extends BottomSheetDialogFragment {
	public static final String TAG = WikivoyageShowPicturesDialogFragment.class.getSimpleName();

	public static final int SHOW_PICTURES_CHANGED = 1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_wikivoyage_show_images_first_time, container, false);
		view.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandApplication app = getMyApplication();
						if (app != null) {
							app.getSettings().WIKIVOYAGE_SHOW_IMAGES.set(WikivoyageShowImages.OFF);
						}
						sendResult();
						dismiss();
					}
				});
		view.findViewById(R.id.button_wifi).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandApplication app = getMyApplication();
						if (app != null) {
							app.getSettings().WIKIVOYAGE_SHOW_IMAGES.set(WikivoyageShowImages.WIFI);
						}
						sendResult();
						dismiss();
					}
				});
		view.findViewById(R.id.button_yes).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandApplication app = getMyApplication();
						if (app != null) {
							app.getSettings().WIKIVOYAGE_SHOW_IMAGES.set(WikivoyageShowImages.ON);
						}
						sendResult();
						dismiss();
					}
				});

		return view;
	}

	private void sendResult() {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(getTargetRequestCode(), SHOW_PICTURES_CHANGED, null);
		}
	}
}
