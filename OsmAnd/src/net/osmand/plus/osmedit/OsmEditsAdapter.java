package net.osmand.plus.osmedit;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.List;

public class OsmEditsAdapter extends ArrayAdapter<OsmPoint> {

	private List<OsmPoint> osmEdits;
	private OsmandApplication app;

	private boolean selectionMode;
	private List<OsmPoint> selectedOsmEdits;

	private OsmEditsAdapterListener listener;

	public OsmEditsAdapter(OsmandApplication app, @NonNull List<OsmPoint> points) {
		super(app, R.layout.note, points);
		this.app = app;
		osmEdits = points;
	}

	public List<OsmPoint> getOsmEdits() {
		return osmEdits;
	}

	public void setOsmEdits(List<OsmPoint> osmEdits) {
		this.osmEdits = osmEdits;
		notifyDataSetChanged();
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelectedOsmEdits(List<OsmPoint> selectedOsmEdits) {
		this.selectedOsmEdits = selectedOsmEdits;
	}

	public void setAdapterListener(OsmEditsAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = LayoutInflater.from(getContext()).inflate(R.layout.note, parent, false);
			OsmEditViewHolder holder = new OsmEditViewHolder(view);
			view.setTag(holder);
		}
		final OsmPoint child = getItem(position);

		OsmEditsFragment.getOsmEditView(view, child, app);

		final OsmEditViewHolder holder = (OsmEditViewHolder) view.getTag();
		holder.playImageButton.setVisibility(View.GONE);
		if (selectionMode) {
			holder.optionsImageButton.setVisibility(View.GONE);
			holder.selectCheckBox.setVisibility(View.VISIBLE);
			holder.selectCheckBox.setChecked(selectedOsmEdits.contains(child));
			holder.icon.setVisibility(View.GONE);
			holder.selectCheckBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onItemSelect(child, holder.selectCheckBox.isChecked());
					}
				}
			});
		} else {
			holder.icon.setVisibility(View.VISIBLE);
			holder.optionsImageButton.setVisibility(View.VISIBLE);
			holder.selectCheckBox.setVisibility(View.GONE);
		}

		holder.optionsImageButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
		holder.optionsImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onOptionsClick(v, child);
				}
			}
		});
		holder.mainView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectionMode) {
					holder.selectCheckBox.performClick();
				} else {
					if (listener != null) {
						listener.onItemShowMap(child);
					}
				}

			}
		});
		return view;
	}

	private class OsmEditViewHolder {
		View mainView;
		ImageView icon;
		ImageButton playImageButton;
		CheckBox selectCheckBox;
		ImageButton optionsImageButton;

		OsmEditViewHolder(View view) {
			mainView = view;
			icon = (ImageView) view.findViewById(R.id.icon);
			playImageButton = (ImageButton) view.findViewById(R.id.play);
			selectCheckBox = (CheckBox) view.findViewById(R.id.check_local_index);
			optionsImageButton = (ImageButton) view.findViewById(R.id.options);
		}
	}

	public interface OsmEditsAdapterListener {

		void onItemSelect(OsmPoint point, boolean checked);

		void onItemShowMap(OsmPoint point);

		void onOptionsClick(View view, OsmPoint note);
	}
}
