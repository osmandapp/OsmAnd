package net.osmand.plus.plugins.osmedit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.render.RenderingIcons;

import java.util.List;
import java.util.Map;

public class OsmEditsAdapter extends ArrayAdapter<Object> {

	public static final int TYPE_HEADER = 0;
	private static final int TYPE_ITEM = 1;
	private static final int TYPE_COUNT = 2;

	private final OsmandApplication app;

	private final List<Object> items;
	private boolean selectionMode;
	private List<OsmPoint> selectedOsmEdits;
	private boolean portrait;

	private OsmEditsAdapterListener listener;

	public OsmEditsAdapter(OsmandApplication app, @NonNull List<Object> items) {
		super(app, 0, items);
		this.app = app;
		this.items = items;
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

	public void setPortrait(boolean portrait) {
		this.portrait = portrait;
	}

	public void setAdapterListener(OsmEditsAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		boolean nightMode = !app.getSettings().isLightContent();
		Context themedCtx = UiUtilities.getThemedContext(getContext(), nightMode);
		if (portrait) {
			if (convertView == null) {
				if (position == 0) {
					convertView = LayoutInflater.from(themedCtx).inflate(R.layout.list_item_header, parent, false);
					HeaderViewHolder holder = new HeaderViewHolder(convertView);
					convertView.setTag(holder);
				} else {
					convertView = LayoutInflater.from(themedCtx).inflate(R.layout.note_list_item, parent, false);
					OsmEditViewHolder holder = new OsmEditViewHolder(convertView);
					convertView.setTag(holder);
				}
			}
			if (position == 0) {
				bindHeaderViewHolder((HeaderViewHolder) convertView.getTag());
			} else {
				Object item = getItem(position);
				if (item instanceof OsmPoint) {
					OsmEditViewHolder holder = (OsmEditViewHolder) convertView.getTag();
					bindOsmEditViewHolder(holder, (OsmPoint) item, position);
				}
			}

			return convertView;
		} else {
			int margin = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
			int sideMargin = app.getResources().getDisplayMetrics().widthPixels / 10;

			FrameLayout fl = new FrameLayout(themedCtx);
			LinearLayout ll = new LinearLayout(themedCtx);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setBackgroundResource(app.getSettings().isLightContent() ? R.drawable.bg_card_light : R.drawable.bg_card_dark);
			fl.addView(ll);
			((FrameLayout.LayoutParams) ll.getLayoutParams()).setMargins(sideMargin, margin, sideMargin, margin);

			HeaderViewHolder headerViewHolder = new HeaderViewHolder(LayoutInflater.from(themedCtx).inflate(R.layout.list_item_header, parent, false));
			bindHeaderViewHolder(headerViewHolder);
			ll.addView(headerViewHolder.mainView);

			for (int i = 0; i < items.size(); i++) {
				Object item = getItem(i);
				if (item instanceof OsmPoint) {
					OsmEditViewHolder viewHolder = new OsmEditViewHolder(LayoutInflater.from(themedCtx).inflate(R.layout.note_list_item, parent, false));
					bindOsmEditViewHolder(viewHolder, (OsmPoint) item, i);
					ll.addView(viewHolder.mainView);
				}
			}

			return fl;
		}
	}

	@Override
	public int getCount() {
		if (portrait) {
			return super.getCount();
		} else {
			return getHeadersCount();
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object item = getItem(position);
		if (item instanceof OsmPoint) {
			return TYPE_ITEM;
		}
		return (int) item;
	}

	@Override
	public int getViewTypeCount() {
		return TYPE_COUNT;
	}

	private int getHeadersCount() {
		int count = 0;
		for (int i = 0; i < items.size(); i++) {
			Object item = items.get(i);
			if (item instanceof Integer) {
				count++;
			}
		}
		return count;
	}

	private void bindHeaderViewHolder(HeaderViewHolder holder) {
		setupBackground(holder.backgroundView);
		holder.topDivider.setVisibility(portrait ? View.VISIBLE : View.GONE);
		holder.title.setText(R.string.your_edits);
		holder.checkBox.setChecked(isAllSelected());
		if (selectionMode) {
			holder.checkBox.setVisibility(View.VISIBLE);
			holder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onHeaderCheckboxClick(holder.checkBox.isChecked());
					}
				}
			});
		} else {
			holder.checkBox.setVisibility(View.GONE);
		}
	}

	private void bindOsmEditViewHolder(OsmEditViewHolder holder, OsmPoint osmEdit, int position) {
		setupBackground(holder.mainView);
		holder.titleTextView.setText(OsmEditingPlugin.getTitle(osmEdit, getContext()));
		holder.descriptionTextView.setText(getDescription(osmEdit));
		Drawable icon = getIcon(osmEdit);
		if (icon != null) {
			holder.icon.setImageDrawable(icon);
		}
		if (selectionMode) {
			holder.optionsImageButton.setVisibility(View.GONE);
			holder.selectCheckBox.setVisibility(View.VISIBLE);
			holder.selectCheckBox.setChecked(selectedOsmEdits.contains(osmEdit));
			holder.icon.setVisibility(View.GONE);
			holder.selectCheckBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onItemSelect(osmEdit, holder.selectCheckBox.isChecked());
					}
				}
			});
		} else {
			holder.icon.setVisibility(View.VISIBLE);
			holder.optionsImageButton.setVisibility(View.VISIBLE);
			holder.selectCheckBox.setVisibility(View.GONE);
		}

		holder.optionsImageButton.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
		holder.optionsImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onOptionsClick(osmEdit);
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
						listener.onItemShowMap(osmEdit, position);
					}
				}

			}
		});
		boolean showDivider = getItemsCount() > 1 && position != getItemsCount() - 1;
		holder.bottomDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
	}

	private void setupBackground(View view) {
		if (!portrait) {
			view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_transparent));
		}
	}

	private int getItemsCount() {
		return items.size();
	}


	private Drawable getIcon(OsmPoint point) {
		if (point.getGroup() == OsmPoint.Group.POI) {
			OpenstreetmapPoint osmPoint = (OpenstreetmapPoint) point;
			int iconResId = 0;
			String poiTranslation = osmPoint.getEntity().getTag(Entity.POI_TYPE_TAG);
			if (poiTranslation != null) {
				Map<String, PoiType> poiTypeMap = app.getPoiTypes().getAllTranslatedNames(false);
				PoiType poiType = poiTypeMap.get(poiTranslation.toLowerCase());
				if (poiType != null) {
					String id = null;
					if (RenderingIcons.containsBigIcon(poiType.getIconKeyName())) {
						id = poiType.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
						id = poiType.getOsmTag() + "_" + poiType.getOsmValue();
					}
					if (id != null) {
						iconResId = RenderingIcons.getBigIconResourceId(id);
					}
				}
			}
			if (iconResId == 0) {
				iconResId = R.drawable.ic_action_info_dark;
			}
			int colorResId = R.color.color_distance;
			if (point.getAction() == OsmPoint.Action.CREATE) {
				colorResId = R.color.color_osm_edit_create;
			} else if (point.getAction() == OsmPoint.Action.MODIFY) {
				colorResId = R.color.color_osm_edit_modify;
			} else if (point.getAction() == OsmPoint.Action.DELETE) {
				colorResId = R.color.color_osm_edit_delete;
			} else if (point.getAction() == OsmPoint.Action.REOPEN) {
				colorResId = R.color.color_osm_edit_modify;
			}
			return app.getUIUtilities().getIcon(iconResId, colorResId);
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return app.getUIUtilities().getIcon(R.drawable.ic_action_osm_note_add, R.color.color_distance);
		}
		return null;
	}

	private boolean isAllSelected() {
		for (int i = 0; i < items.size(); i++) {
			Object item = items.get(i);
			if (item instanceof OsmPoint) {
				if (!selectedOsmEdits.contains(item)) {
					return false;
				}
			}
		}
		return true;
	}

	private String getDescription(OsmPoint point) {
		return OsmEditingPlugin.getDescription(point, getContext(), true);
	}

	private class HeaderViewHolder {
		View mainView;
		View topDivider;
		View backgroundView;
		CheckBox checkBox;
		TextView title;

		HeaderViewHolder(View view) {
			mainView = view;
			topDivider = view.findViewById(R.id.top_divider);
			backgroundView = view.findViewById(R.id.background_view);
			checkBox = view.findViewById(R.id.check_box);
			title = view.findViewById(R.id.title_text_view);
		}
	}

	private class OsmEditViewHolder {
		View mainView;
		ImageView icon;
		CheckBox selectCheckBox;
		ImageButton optionsImageButton;
		TextView titleTextView;
		TextView descriptionTextView;
		View bottomDivider;

		OsmEditViewHolder(View view) {
			mainView = view;
			icon = view.findViewById(R.id.icon);
			selectCheckBox = view.findViewById(R.id.check_box);
			optionsImageButton = view.findViewById(R.id.options);
			titleTextView = view.findViewById(R.id.title);
			descriptionTextView = view.findViewById(R.id.description);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public interface OsmEditsAdapterListener {

		void onHeaderCheckboxClick(boolean checked);

		void onItemSelect(OsmPoint point, boolean checked);

		void onItemShowMap(OsmPoint point, int position);

		void onOptionsClick(OsmPoint note);
	}
}
