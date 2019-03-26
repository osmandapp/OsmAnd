package net.osmand.plus.profiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import org.apache.commons.logging.Log;

public class SettingsProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;
	private AppCompatButton btn;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profiles_list_fragment, container, false);

		recyclerView = view.findViewById(R.id.profiles_list);
		btn = view.findViewById(R.id.add_profile_btn);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//todo add new profile;
			}
		});

		return view;
	}
}
