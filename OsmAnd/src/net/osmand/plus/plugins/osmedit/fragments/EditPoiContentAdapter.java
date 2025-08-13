package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.fragments.AdvancedEditPoiFragment.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment.OpenHoursItem;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment.OpeningHoursAdapter;
import net.osmand.plus.plugins.osmedit.fragments.AdvancedEditPoiFragment.OsmTagsArrayAdapter;
import net.osmand.plus.plugins.osmedit.fragments.holders.AddItemHolder;
import net.osmand.plus.plugins.osmedit.fragments.holders.AddOpeningHoursHolder;
import net.osmand.plus.plugins.osmedit.fragments.holders.BasicInfoHolder;
import net.osmand.plus.plugins.osmedit.fragments.holders.DescriptionItemHolder;
import net.osmand.plus.plugins.osmedit.fragments.holders.OpenTimeListHolder;
import net.osmand.plus.plugins.osmedit.fragments.holders.TagItemHolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class EditPoiContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	public static final int TYPE_DESCRIPTION_ITEM = 1;
	public static final int TYPE_TAG_ITEM = 2;
	public static final int TYPE_ADD_TAG = 3;
	public static final int TYPE_ADD_OPENING_HOURS = 4;
	public static final int TYPE_BASIC_INFO = 5;
	public static final int TYPE_OPEN_TIME_LIST_ITEM = 6;

	public static final int PAYLOAD_NAME = 0;
	public static final int PAYLOAD_AMENITY = 1;
	public static final int PAYLOAD_FOCUS_ON_ITEM = 2;

	private final LayoutInflater themedInflater;
	private final OsmandApplication app;
	private final boolean nightMode;

	private final EditPoiDialogFragment editPoiDialogFragment;

	private List<Object> items = new ArrayList<>();

	public final OsmTagsArrayAdapter tagAdapter;
	private final ArrayAdapter<String> valueAdapter;
	private final OpeningHoursAdapter openingHoursAdapter;
	private final Activity activity;

	private EditText currentTagEditText;
	private final EditPoiListener editPoiListener;
	private final EditPoiAdapterListener editPoiAdapterListener;


	public EditPoiContentAdapter(@NonNull Activity activity, @NonNull List<Object> items,
	                             ArrayAdapter<String> valueAdapter, OsmTagsArrayAdapter tagAdapter,
	                             OpeningHoursAdapter openingHoursAdapter, boolean nightMode, @NonNull EditPoiDialogFragment editPoiDialogFragment,
	                             @NonNull EditPoiListener editPoiListener) {
		setHasStableIds(true);
		this.items.addAll(items);
		this.nightMode = nightMode;
		this.app = AndroidUtils.getApp(activity);
		this.activity = activity;
		this.valueAdapter = valueAdapter;
		this.tagAdapter = tagAdapter;
		this.openingHoursAdapter = openingHoursAdapter;
		this.editPoiDialogFragment = editPoiDialogFragment;
		this.editPoiListener = editPoiListener;
		themedInflater = UiUtilities.getInflater(activity, nightMode);
		editPoiAdapterListener = getTagItemHolderListener();
	}

	private EditPoiAdapterListener getTagItemHolderListener() {
		return new EditPoiAdapterListener() {
			@Override
			public void setCurrentTagEditText(@Nullable EditText editText) {
				currentTagEditText = editText;
			}

			@Nullable
			@Override
			public EditText getCurrentTagEditText() {
				return currentTagEditText;
			}

			@Override
			public void removeItem(int position) {
				notifyItemRemoved(position);
				items.remove(position);
			}

			@SuppressLint("NotifyDataSetChanged")
			@Override
			public void dataChanged() {
				notifyDataSetChanged();
			}
		};
	}

	private EditPoiData getData() {
		return editPoiDialogFragment.getEditPoiData();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<Object> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	@NonNull
	public List<Object> getItems() {
		return items;
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof Integer integer) {
			return integer;
		} else if (object instanceof TagItem) {
			return TYPE_TAG_ITEM;
		} else if (object instanceof OpenHoursItem) {
			return TYPE_OPEN_TIME_LIST_ITEM;
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public long getItemId(int position) {
		Object object = items.get(position);
		if (object instanceof Integer integer) {
			return integer;
		} else if (object instanceof TagItem tagItem) {
			if (Algorithms.isEmpty(tagItem.tag())) {
				return tagItem.id();
			} else {
				return tagItem.tag().hashCode();
			}
		} else if (object instanceof OpenHoursItem openHoursItem) {
			return openHoursItem.id();
		}
		return super.getItemId(position);
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView;
		return switch (viewType) {
			case TYPE_DESCRIPTION_ITEM -> {
				itemView = themedInflater.inflate(R.layout.edit_poi_description_item, parent, false);
				yield new DescriptionItemHolder(itemView);
			}
			case TYPE_TAG_ITEM -> {
				itemView = themedInflater.inflate(R.layout.list_item_poi_tag, parent, false);
				yield new TagItemHolder(itemView, app, valueAdapter, tagAdapter, activity, nightMode);
			}
			case TYPE_ADD_TAG -> {
				itemView = themedInflater.inflate(R.layout.edit_poi_add_item, parent, false);
				yield new AddItemHolder(itemView);
			}
			case TYPE_BASIC_INFO -> {
				itemView = themedInflater.inflate(R.layout.edit_poi_basic_info_item, parent, false);
				yield new BasicInfoHolder(itemView);
			}
			case TYPE_ADD_OPENING_HOURS -> {
				itemView = themedInflater.inflate(R.layout.edit_poi_add_opening_hours_item, parent, false);
				yield new AddOpeningHoursHolder(itemView);
			}
			case TYPE_OPEN_TIME_LIST_ITEM -> {
				itemView = themedInflater.inflate(R.layout.open_time_list_item, parent, false);
				yield new OpenTimeListHolder(itemView, activity);
			}

			default -> throw new IllegalArgumentException("Unsupported view type");
		};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object item = items.get(position);

		if (holder instanceof DescriptionItemHolder descriptionItemHolder) {
			descriptionItemHolder.bindView(getData());
		} else if (holder instanceof TagItemHolder tagItemHolder && item instanceof TagItem tagItem) {
			tagItemHolder.bindView(holder, tagItem, getData(), editPoiListener, editPoiAdapterListener);
		} else if (holder instanceof AddItemHolder addItemHolder) {
			addItemHolder.bindView(editPoiListener);
		} else if (holder instanceof BasicInfoHolder basicInfoHolder) {
			basicInfoHolder.bindView(getData(), editPoiListener);
		} else if (holder instanceof AddOpeningHoursHolder addOpeningHoursHolder) {
			addOpeningHoursHolder.bindView(editPoiListener);
		} else if (holder instanceof OpenTimeListHolder openTimeListHolder && item instanceof OpenHoursItem openHoursItem) {
			openTimeListHolder.bindView(openHoursItem, openingHoursAdapter, editPoiAdapterListener, editPoiListener);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (!Algorithms.isEmpty(payloads) && payloads.get(0) instanceof Integer payLoadInteger) {
			if (holder instanceof DescriptionItemHolder descriptionItemHolder) {
				switch (payLoadInteger) {
					case PAYLOAD_NAME:
						descriptionItemHolder.updateName(getData());
						break;
					case PAYLOAD_AMENITY:
						descriptionItemHolder.updatePoiType(getData());
						break;
				}
			} else if (holder instanceof TagItemHolder tagItemHolder && PAYLOAD_FOCUS_ON_ITEM == payLoadInteger) {
				tagItemHolder.focusOnTagEdit();
			}
		} else {
			super.onBindViewHolder(holder, position, payloads);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void clearFocus() {
		if (currentTagEditText != null) {
			currentTagEditText.clearFocus();
			currentTagEditText = null;
		}
	}

	public interface EditPoiListener {
		void onAddNewItem(int position, int buttonType);

		void onDeleteItem(int position);

		InputFilter[] getLengthLimit();

		FragmentManager getChildFragmentManager();

		default boolean isFragmentResumed() {
			return false;
		}

		default boolean isBasicTagsInitialized() {
			return false;
		}
	}

	public interface EditPoiAdapterListener {
		void setCurrentTagEditText(@Nullable EditText editText);

		@Nullable
		EditText getCurrentTagEditText();

		void removeItem(int position);

		void dataChanged();
	}
}

