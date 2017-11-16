package net.osmand.plus.audionotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.audionotes.NotesFragment;

import java.util.List;
import java.util.Set;

public class NotesAdapter extends ArrayAdapter<Object> {

	public static final int TYPE_DATE_HEADER = 0;
	public static final int TYPE_AUDIO_HEADER = 1;
	public static final int TYPE_PHOTO_HEADER = 2;
	public static final int TYPE_VIDEO_HEADER = 3;
	private static final int TYPE_ITEM = 4;
	private static final int TYPE_COUNT = 5;

	private OsmandApplication app;
	private NotesAdapterListener listener;

	private boolean selectionMode;
	private Set<Recording> selected;

	public void setListener(NotesAdapterListener listener) {
		this.listener = listener;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelected(Set<Recording> selected) {
		this.selected = selected;
	}

	public NotesAdapter(OsmandApplication app, List<Object> items) {
		super(app, R.layout.note, items);
		this.app = app;
	}

	@NonNull
	@Override
	public View getView(final int position, View row, @NonNull ViewGroup parent) {
		final int type = getItemViewType(position);
		boolean header = type == TYPE_DATE_HEADER
				|| type == TYPE_AUDIO_HEADER
				|| type == TYPE_PHOTO_HEADER
				|| type == TYPE_VIDEO_HEADER;

		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (header) {
				row = inflater.inflate(R.layout.list_item_header, parent, false);
				HeaderViewHolder hHolder = new HeaderViewHolder(row);
				row.setTag(hHolder);
			} else {
				row = inflater.inflate(R.layout.note, parent, false);
				ItemViewHolder iHolder = new ItemViewHolder(row);
				row.setTag(iHolder);
			}
		}

		if (header) {
			final HeaderViewHolder holder = (HeaderViewHolder) row.getTag();
			holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			holder.headerRow.setEnabled(selectionMode);
			if (selectionMode) {
				holder.checkBox.setChecked(isSelectAllChecked());
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
			int titleId;
			if (type == TYPE_DATE_HEADER) {
				titleId = R.string.notes_by_date;
			} else if (type == TYPE_AUDIO_HEADER) {
				titleId = R.string.shared_string_audio;
			} else if (type == TYPE_PHOTO_HEADER) {
				titleId = R.string.shared_string_photo;
			} else {
				titleId = R.string.shared_string_video;
			}
			holder.title.setText(titleId);
		} else {
			final Object item = getItem(position);
			if (item instanceof Recording) {
				final Recording recording = (Recording) item;
				final ItemViewHolder holder = (ItemViewHolder) row.getTag();

				if (recording == NotesFragment.SHARE_LOCATION_FILE) {
					holder.title.setText(R.string.av_locations);
					holder.description.setText(R.string.av_locations_descr);
				} else {
					holder.title.setText(recording.getName(app, true));
					holder.description.setText(recording.getNewSmallDescription(app));
					int iconRes = recording.isAudio() ? R.drawable.ic_type_audio
							: (recording.isVideo() ? R.drawable.ic_type_video : R.drawable.ic_type_img);
					holder.icon.setImageDrawable(app.getIconsCache().getIcon(iconRes, R.color.color_distance));
				}

				holder.play.setVisibility(View.GONE);
				holder.bottomDivider.setVisibility(View.VISIBLE);
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
					holder.options.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
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
								listener.onItemClick(recording);
							}
						}
					}
				});
			}
		}

		return row;
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

	private boolean isSelectAllChecked() {
		for (int i = 0; i < getCount(); i++) {
			Object item = getItem(i);
			if (item instanceof Recording) {
				if (!selected.contains(item)) {
					return false;
				}
			}
		}
		return true;
	}

	private class HeaderViewHolder {

		final View view;
		final View headerRow;
		final CheckBox checkBox;
		final TextView title;

		HeaderViewHolder(View view) {
			this.view = view;
			headerRow = view.findViewById(R.id.header_row);
			checkBox = (CheckBox) view.findViewById(R.id.check_box);
			title = (TextView) view.findViewById(R.id.title_text_view);
		}
	}

	private class ItemViewHolder {

		final View view;
		final ImageView icon;
		final CheckBox checkBox;
		final TextView title;
		final TextView description;
		final ImageButton options;
		final View play;
		final View bottomDivider;

		ItemViewHolder(View view) {
			this.view = view;
			icon = (ImageView) view.findViewById(R.id.icon);
			checkBox = (CheckBox) view.findViewById(R.id.check_local_index);
			title = (TextView) view.findViewById(R.id.name);
			description = (TextView) view.findViewById(R.id.description);
			options = (ImageButton) view.findViewById(R.id.options);
			play = view.findViewById(R.id.play);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public interface NotesAdapterListener {

		void onHeaderClick(int type, boolean checked);

		void onCheckBoxClick(Recording rec, boolean checked);

		void onItemClick(Recording rec);

		void onOptionsClick(Recording rec);
	}
}
