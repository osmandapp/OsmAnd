package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;


public class ImportDuplicatesFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();

	private FrameLayout replaceAllBtn;

	public static void showInstance(@NonNull FragmentManager fm) {
		ImportDuplicatesFragment fragment = new ImportDuplicatesFragment();
		fragment.show(fm, TAG);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_import_duplicates, container, false);
		setupToolbar((Toolbar) root.findViewById(R.id.toolbar));
		replaceAllBtn = root.findViewById(R.id.replace_all);


		return root;

	}

	private void setupToolbar(Toolbar toolbar) {
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_arrow_back,
				getSettings().isLightContent()
						? getResources().getColor(R.color.active_buttons_and_links_text_light)
						: getResources().getColor(R.color.active_buttons_and_links_text_dark)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}
}
