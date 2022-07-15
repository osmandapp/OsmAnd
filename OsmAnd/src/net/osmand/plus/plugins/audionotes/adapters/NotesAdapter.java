package net.osmand.plus.plugins.audionotes.adapters;

import android.content.Context;
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

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.plugins.audionotes.NotesFragment;

import java.util.List;
import java.util.Set;

public class NotesAdapter extends ArrayAdapter<Object> {

	public static final int TYPE_DATE_HEADER = 0;
	public static final int TYPE_AUDIO_HEADER = 1;
	public static final int TYPE_PHOTO_HEADER = 2;
	public static final int TYPE_VIDEO_HEADER = 3;
	private static final int TYPE_ITEM = 4;
	private static final int TYPE_COUNT = 5;

	private final OsmandApplication app;
	private NotesAdapterListener listener;
	private final List<Object> items;

	private boolean selectionMode;
	private Set<Recording> selected;

	private boolean portrait;

	public void setListener(NotesAdapterListener listener) {
		this.listener = listener;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelected(Set<Recording> selected) {
		this.selected = selected;
	}

	public void setPortrait(boolean portrait) {
		this.portrait = portrait;
	}

	public NotesAdapter(OsmandApplication app, List<Object> items) {
		super(app, R.layout.note, items);
		this.app = app;
		this.items = items;
	}

	@NonNull
	@Override
	public View getView(int position, View row, @NonNull ViewGroup parent) {
		boolean nightMode = !app.getSettings().isLightContent();
		Context themedCtx = UiUtilities.getThemedContext(getContext(), nightMode);
		if (portrait) {
			int type = getItemViewType(position);
			boolean header = type == TYPE_DATE_HEADER
					|| type == TYPE_AUDIO_HEADER
					|| type == TYPE_PHOTO_HEADER
					|| type == TYPE_VIDEO_HEADER;

			if (row == null) {
				LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
				if (header) {
					row = inflater.inflate(R.layout.list_item_header, parent, false);
					HeaderViewHolder hHolder = new HeaderViewHolder(row);
					row.setTag(hHolder);
				} else {
					row = inflater.inflate(R.layout.note_list_item, parent, false);
					ItemViewHolder iHolder = new ItemViewHolder(row);
					row.setTag(iHolder);
				}
			}

			if (header) {
				setupHeader(type, (HeaderViewHolder) row.getTag());
			} else {
				Object item = getItem(position);
				if (item instanceof Recording) {
					setupItem(position, (Recording) item, (ItemViewHolder) row.getTag());
				}
			}

			return row;
		} else {
			LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
			boolean lastCard = getCardsCount() == position + 1;
			int margin = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
			int sideMargin = app.getResources().getDisplayMetrics().widthPixels / 10;

			FrameLayout fl = new FrameLayout(themedCtx);
			LinearLayout ll = new LinearLayout(themedCtx);
			fl.addView(ll);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setBackgroundResource(nightMode ? R.drawable.bg_card_dark : R.drawable.bg_card_light);
			((FrameLayout.LayoutParams) ll.getLayoutParams()).setMargins(sideMargin, margin, sideMargin, lastCard ? margin : 0);

			if (position == 0 && hasShareLocationItem()) {
				createItem(parent, inflater, ll, 0, NotesFragment.SHARE_LOCATION_FILE);
				return fl;
			}

			int headerInd = getHeaderIndex(hasShareLocationItem() ? position - 1 : position);
			HeaderViewHolder headerVH = new HeaderViewHolder(inflater.inflate(R.layout.list_item_header, parent, false));
			setupHeader((int) items.get(headerInd), headerVH);
			ll.addView(headerVH.view);

			for (int i = headerInd + 1; i < items.size(); i++) {
				Object item = items.get(i);
				if (item instanceof Recording) {
					createItem(parent, inflater, ll, i, (Recording) item);
				} else {
					break;
				}
			}

			return fl;
		}
	}

	private void createItem(@NonNull ViewGroup parent, LayoutInflater inflater, LinearLayout ll, int pos, Recording item) {
		ItemViewHolder itemVH = new ItemViewHolder(inflater.inflate(R.layout.note_list_item, parent, false));
		setupItem(pos, item, itemVH);
		ll.addView(itemVH.view);
	}

	@Override
	public int getCount() {
		if (portrait) {
			return super.getCount();
		}
		return getCardsCount();
	}

	@Override
	public int getItemViewType(int position) {
		Object item = getItem(position);
		if (item instanceof Recording) {
			return TYPE_ITEM;
		}
		return (int) item;
	}

	@Override
	public int getViewTypeCount() {
		return TYPE_COUNT;
	}

	private boolean hasShareLocationItem() {
		return items.get(0) == NotesFragment.SHARE_LOCATION_FILE;
	}

	private void setupHeader(int type, HeaderViewHolder holder) {
		setupBackground(holder.backgroundView);
		holder.topDivider.setVisibility(portrait ? View.VISIBLE : View.GONE);
		holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
		holder.headerRow.setEnabled(selectionMode);
		if (selectionMode) {
			holder.checkBox.setChecked(isSelectAllChecked(type));
			holder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onHeaderClick(type, holder.checkBox.isChecked());
					}
				}
			});
			holder.headerRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					holder.checkBox.performClick();
				}
			});
		} else {
			holder.view.setOnClickListener(null);
		}
		holder.title.setText(getHeaderTitleRes(type));
	}

	private int getHeaderTitleRes(int type) {
		if (type == TYPE_DATE_HEADER) {
			return R.string.notes_by_date;
		} else if (type == TYPE_AUDIO_HEADER) {
			return R.string.shared_string_audio;
		} else if (type == TYPE_PHOTO_HEADER) {
			return R.string.shared_string_photo;
		}
		return R.string.shared_string_video;
	}

	private void setupItem(int position, Recording recording, ItemViewHolder holder) {
		setupBackground(holder.view);
		if (recording == NotesFragment.SHARE_LOCATION_FILE) {
			holder.title.setText(R.string.av_locations);
			holder.description.setText(getLocationsDescId());
		} else {
			holder.title.setText(recording.getName(app, true));
			holder.description.setText(recording.getExtendedDescription(app));
			int iconRes = recording.isAudio() ? R.drawable.ic_type_audio
					: (recording.isVideo() ? R.drawable.ic_type_video : R.drawable.ic_type_img);
			int colorRes = ColorUtilities.getDefaultIconColorId(!app.getSettings().isLightContent());
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, colorRes));
		}

		holder.bottomDivider.setVisibility(hideBottomDivider(position) ? View.GONE : View.VISIBLE);
		holder.icon.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
		holder.options.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		if (selectionMode) {
			holder.checkBox.setChecked(selected.contains(recording));
			holder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onCheckBoxClick(recording, holder.checkBox.isChecked());
					}
				}
			});
		} else {
			holder.options.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
			holder.options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onOptionsClick(recording);
					}
				}
			});
		}

		holder.view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectionMode) {
					holder.checkBox.performClick();
				} else {
					if (listener != null) {
						listener.onItemClick(recording, position);
					}
				}
			}
		});
	}

	private int getLocationsDescId() {
		if (selected.contains(NotesFragment.SHARE_LOCATION_FILE)) {
			return selected.size() == 1 ? R.string.av_locations_all_desc : R.string.av_locations_selected_desc;
		}
		return R.string.av_locations_descr;
	}

	private void setupBackground(View view) {
		if (!portrait) {
			view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_transparent));
		}
	}

	public int getItemsCount() {
		return items.size();
	}

	private int getCardsCount() {
		int res = 0;
		for (int i = 0; i < items.size(); i++) {
			if ((i == 0 && hasShareLocationItem()) || items.get(i) instanceof Integer) {
				res++;
			}
		}
		return res;
	}

	private int getHeaderIndex(int position) {
		int count = 0;
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i) instanceof Integer) {
				if (count == position) {
					return i;
				}
				count++;
			}
		}
		return -1;
	}

	private boolean hideBottomDivider(int pos) {
		return pos == items.size() - 1 || !(getItem(pos + 1) instanceof Recording);
	}

	private boolean isSelectAllChecked(int type) {
		for (Object item : items) {
			if (item instanceof Recording) {
				if (type != TYPE_DATE_HEADER && !isAppropriate((Recording) item, type)) {
					continue;
				}
				if (!selected.contains(item)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isAppropriate(Recording rec, int type) {
		if (type == TYPE_AUDIO_HEADER) {
			return rec.isAudio();
		} else if (type == TYPE_PHOTO_HEADER) {
			return rec.isPhoto();
		}
		return rec.isVideo();
	}

	private class HeaderViewHolder {

		final View view;
		final View topDivider;
		final View backgroundView;
		final View headerRow;
		final CheckBox checkBox;
		final TextView title;

		HeaderViewHolder(View view) {
			this.view = view;
			topDivider = view.findViewById(R.id.top_divider);
			backgroundView = view.findViewById(R.id.background_view);
			headerRow = view.findViewById(R.id.header_row);
			checkBox = view.findViewById(R.id.check_box);
			title = view.findViewById(R.id.title_text_view);
		}
	}

	private class ItemViewHolder {

		final View view;
		final ImageView icon;
		final CheckBox checkBox;
		final TextView title;
		final TextView description;
		final ImageButton options;
		final View bottomDivider;

		ItemViewHolder(View view) {
			this.view = view;
			icon = view.findViewById(R.id.icon);
			checkBox = view.findViewById(R.id.check_box);
			title = view.findViewById(R.id.title);
			description = view.findViewById(R.id.description);
			options = view.findViewById(R.id.options);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public interface NotesAdapterListener {

		void onHeaderClick(int type, boolean checked);

		void onCheckBoxClick(Recording rec, boolean checked);

		void onItemClick(Recording rec, int position);

		void onOptionsClick(Recording rec);
	}
}
