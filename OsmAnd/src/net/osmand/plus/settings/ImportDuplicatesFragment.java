package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.view.ComplexButton;

import java.util.List;


public class ImportDuplicatesFragment extends BaseOsmAndDialogFragment implements View.OnClickListener {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();

	private RecyclerView list;

	public static void showInstance(@NonNull FragmentManager fm) {
		ImportDuplicatesFragment fragment = new ImportDuplicatesFragment();
		fragment.show(fm, TAG);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_import_duplicates, container, false);
		setupToolbar((Toolbar) root.findViewById(R.id.toolbar));
		ComplexButton replaceAllBtn = root.findViewById(R.id.replace_all_btn);
		ComplexButton keepBothBtn = root.findViewById(R.id.keep_both_btn);
		keepBothBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_keep_both,
				getSettings().isLightContent()
						? getResources().getColor(R.color.icon_color_active_light)
						: getResources().getColor(R.color.icon_color_active_dark))
		);
		replaceAllBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_replace,
				getSettings().isLightContent()
						? getResources().getColor(R.color.active_buttons_and_links_text_light)
						: getResources().getColor(R.color.active_buttons_and_links_text_dark))
		);
		keepBothBtn.setOnClickListener(this);
		replaceAllBtn.setOnClickListener(this);
		list = root.findViewById(R.id.list);

		return root;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.keep_both_btn: {
				keepBothItems();
				break;
			}
			case R.id.replace_all_btn: {
				replaceAllItems();
				break;
			}
		}
	}

	private void keepBothItems() {

	}

	private void replaceAllItems() {

	}

	private void importItems(List<SettingsItem> list) {

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
