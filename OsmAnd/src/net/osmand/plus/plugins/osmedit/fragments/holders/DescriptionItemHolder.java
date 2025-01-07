package net.osmand.plus.plugins.osmedit.fragments.holders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;

public class DescriptionItemHolder extends RecyclerView.ViewHolder {
	private final TextView nameTextView;
	private final TextView amenityTagTextView;
	private final TextView amenityTextView;

	public DescriptionItemHolder(@NonNull View itemView) {
		super(itemView);
		this.nameTextView = itemView.findViewById(R.id.nameTextView);
		this.amenityTagTextView = itemView.findViewById(R.id.amenityTagTextView);
		this.amenityTextView = itemView.findViewById(R.id.amenityTextView);
	}

	public void bindView(EditPoiData data) {
		updateName(data);
		updatePoiType(data);
	}

	public void updateName(EditPoiData data) {
		nameTextView.setText(data.getTag(OSMSettings.OSMTagKey.NAME.getValue()));
	}

	public void updatePoiType(EditPoiData data) {
		PoiType pt = data.getPoiTypeDefined();
		if (pt != null) {
			amenityTagTextView.setText(pt.getEditOsmTag());
			amenityTextView.setText(pt.getEditOsmValue());
		} else {
			PoiCategory category = data.getPoiCategory();
			if (category != null) {
				amenityTagTextView.setText(category.getDefaultTag());
			} else {
				amenityTagTextView.setText(R.string.tag_poi_amenity);
			}
			amenityTextView.setText(data.getPoiTypeString());
		}
	}
}