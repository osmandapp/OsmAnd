package net.osmand.plus.plugins.weather.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseBottomSheetDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.enums.WeatherSource;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class SelectWeatherSourceBottomSheet extends BaseBottomSheetDialogFragment {

	private static final String TAG = SelectWeatherSourceBottomSheet.class.getSimpleName();

	private WeatherPlugin plugin;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.bottom_sheet_select_weather_source);

		((TextView) view.findViewById(R.id.title)).setText(R.string.data_source);
		((TextView) view.findViewById(R.id.description)).setText(R.string.weather_data_sources_prompt);

		view.findViewById(R.id.cancel_btn).setOnClickListener(v -> dismiss());

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(new WeatherSourceAdapter());

		return view;
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.OVER_MAP;
	}

	public class WeatherSourceAdapter extends RecyclerView.Adapter<WeatherSourceAdapter.WeatherSourceViewHolder> {
		public WeatherSourceAdapter() {
		}

		@NonNull
		@Override
		public WeatherSourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			return new WeatherSourceViewHolder(inflater.inflate(R.layout.bottom_sheet_item_with_descr_and_left_radio_btn, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull WeatherSourceViewHolder holder, int position) {
			WeatherSource source = WeatherSource.getEntries().get(position);
			holder.title.setText(source.getTitleId());
			holder.description.setText(source.getDescriptionId());
			holder.description.setText(source.getDescriptionId());
			holder.radioButton.setChecked(source == plugin.getWeatherSource());
			holder.itemView.setOnClickListener(view -> {
				plugin.setWeatherSource(source);
				holder.radioButton.setChecked(true);
				dismiss();
			});
		}

		@Override
		public int getItemCount() {
			return WeatherSource.getEntries().size();
		}

		class WeatherSourceViewHolder extends RecyclerView.ViewHolder {

			private final TextView title;
			private final TextView description;
			private final RadioButton radioButton;

			public WeatherSourceViewHolder(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				description = itemView.findViewById(R.id.description);
				description.setMaxLines(2);
				radioButton = itemView.findViewById(R.id.compound_button);
			}
		}
	}


	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectWeatherSourceBottomSheet fragment = new SelectWeatherSourceBottomSheet();
			fragment.show(manager, TAG);
		}
	}
}