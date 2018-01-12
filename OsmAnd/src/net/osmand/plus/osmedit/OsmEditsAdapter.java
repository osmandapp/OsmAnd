package net.osmand.plus.osmedit;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
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

import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;

public class OsmEditsAdapter extends ArrayAdapter<Object> {

	public static final int TYPE_HEADER = 0;
	private static final int TYPE_ITEM = 1;
	private static final int TYPE_COUNT = 2;

	private OsmandApplication app;

	private List<Object> items;
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
		if (portrait) {
			if (convertView == null) {
				if (position == 0) {
					convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_header, parent, false);
					HeaderViewHolder holder = new HeaderViewHolder(convertView);
					convertView.setTag(holder);
				} else {
					convertView = LayoutInflater.from(getContext()).inflate(R.layout.note_list_item, parent, false);
					OsmEditViewHolder holder = new OsmEditViewHolder(convertView);
					convertView.setTag(holder);
				}
			}
			if (position == 0) {
				bindHeaderViewHolder((HeaderViewHolder) convertView.getTag());
			} else {
				final Object item = getItem(position);
				if (item instanceof OsmPoint) {
					final OsmEditViewHolder holder = (OsmEditViewHolder) convertView.getTag();
					bindOsmEditViewHolder(holder, (OsmPoint) item, position);
				}
			}

			return convertView;
		} else {
			int margin = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
			int sideMargin = app.getResources().getDisplayMetrics().widthPixels / 10;

			FrameLayout fl = new FrameLayout(getContext());
			LinearLayout ll = new LinearLayout(getContext());
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setBackgroundResource(app.getSettings().isLightContent() ? R.drawable.bg_card_light : R.drawable.bg_card_dark);
			fl.addView(ll);
			((FrameLayout.LayoutParams) ll.getLayoutParams()).setMargins(sideMargin, margin, sideMargin, margin);

			HeaderViewHolder headerViewHolder = new HeaderViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.list_item_header, parent, false));
			bindHeaderViewHolder(headerViewHolder);
			ll.addView(headerViewHolder.mainView);

			for (int i = 0; i < items.size(); i++) {
				Object item = getItem(i);
				if (item instanceof OsmPoint) {
					OsmEditViewHolder viewHolder = new OsmEditViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.note_list_item, parent, false));
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

	private void bindHeaderViewHolder(final HeaderViewHolder holder) {
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

	private void bindOsmEditViewHolder(final OsmEditViewHolder holder, final OsmPoint osmEdit, int position) {
		setupBackground(holder.mainView);
		holder.titleTextView.setText(getTitle(osmEdit));
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

		holder.optionsImageButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
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
						listener.onItemShowMap(osmEdit);
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

	private SpannableString getTitle(OsmPoint osmPoint) {
		SpannableString title = new SpannableString(OsmEditingPlugin.getName(osmPoint));
		if (TextUtils.isEmpty(title)) {
			title = SpannableString.valueOf(getCategory(osmPoint));
			title.setSpan(new StyleSpan(Typeface.ITALIC), 0, title.length(), 0);
		}
		return title;
	}

	private Drawable getIcon(OsmPoint point) {
		if (point.getGroup() == OsmPoint.Group.POI) {
			OpenstreetmapPoint osmPoint = (OpenstreetmapPoint) point;
			int iconResId = 0;
			String poiTranslation = osmPoint.getEntity().getTag(EditPoiData.POI_TYPE_TAG);
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
				iconResId = R.drawable.ic_type_info;
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
			return app.getIconsCache().getIcon(iconResId, colorResId);
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return app.getIconsCache().getIcon(R.drawable.ic_type_bug, R.color.color_distance);
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

	private String getCategory(OsmPoint point) {
		return OsmEditingPlugin.getCategory(point, getContext());
	}

	private String getDescription(OsmPoint point) {
		String action = "";
		if (point.getAction() == OsmPoint.Action.CREATE) {
			action = getContext().getString(R.string.shared_string_added);
		} else if (point.getAction() == OsmPoint.Action.MODIFY) {
			action = getContext().getString(R.string.shared_string_edited);
		} else if (point.getAction() == OsmPoint.Action.DELETE) {
			action = getContext().getString(R.string.shared_string_deleted);
		} else if (point.getAction() == OsmPoint.Action.REOPEN) {
			action = getContext().getString(R.string.shared_string_edited);
		}

		String category = getCategory(point);

		String prefix = OsmEditingPlugin.getPrefix(point);

		String description = "";
		if (!Algorithms.isEmpty(action)) {
			description += action + " • ";
		}
		if (!Algorithms.isEmpty(category)) {
			description += category + " • ";
		}
		description += prefix;

		return description;
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
			checkBox = (CheckBox) view.findViewById(R.id.check_box);
			title = (TextView) view.findViewById(R.id.title_text_view);
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
			icon = (ImageView) view.findViewById(R.id.icon);
			selectCheckBox = (CheckBox) view.findViewById(R.id.check_box);
			optionsImageButton = (ImageButton) view.findViewById(R.id.options);
			titleTextView = (TextView) view.findViewById(R.id.title);
			descriptionTextView = (TextView) view.findViewById(R.id.description);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public interface OsmEditsAdapterListener {

		void onHeaderCheckboxClick(boolean checked);

		void onItemSelect(OsmPoint point, boolean checked);

		void onItemShowMap(OsmPoint point);

		void onOptionsClick(OsmPoint note);
	}
}
