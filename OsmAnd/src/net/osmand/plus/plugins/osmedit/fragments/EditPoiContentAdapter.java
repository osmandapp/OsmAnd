package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;
import static net.osmand.plus.plugins.osmedit.fragments.NewAdvancedEditPoiFragment.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.NewBasicEditPoiFragment.OpenHoursItem;
import net.osmand.plus.plugins.osmedit.dialogs.NewBasicEditPoiFragment.OpeningHoursAdapter;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursDaysDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursHoursDialogFragment;
import net.osmand.plus.plugins.osmedit.fragments.NewAdvancedEditPoiFragment.OsmTagsArrayAdapter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;
import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

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


	public EditPoiContentAdapter(@NonNull MapActivity mapActivity, @NonNull List<Object> items,
	                             ArrayAdapter<String> valueAdapter, OsmTagsArrayAdapter tagAdapter,
	                             OpeningHoursAdapter openingHoursAdapter, boolean nightMode, EditPoiDialogFragment editPoiDialogFragment,
	                             EditPoiListener editPoiListener) {
		setHasStableIds(true);
		this.items.addAll(items);
		this.nightMode = nightMode;
		this.app = mapActivity.getMyApplication();
		this.activity = mapActivity;
		this.valueAdapter = valueAdapter;
		this.tagAdapter = tagAdapter;
		this.openingHoursAdapter = openingHoursAdapter;
		this.editPoiDialogFragment = editPoiDialogFragment;
		this.editPoiListener = editPoiListener;
		themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
	}

	private EditPoiData getData() {
		return editPoiDialogFragment.getEditPoiData();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<Object> items) {
		this.items = items;
		notifyDataSetChanged();
	}

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
			return tagItem.id();
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
				yield new TagItemHolder(itemView, app);
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
				yield new OpenTimeListHolder(itemView);
			}

			default -> throw new IllegalArgumentException("Unsupported view type");
		};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object item = items.get(position);

		if (holder instanceof DescriptionItemHolder descriptionItemHolder) {
			descriptionItemHolder.bindView();
		} else if (holder instanceof TagItemHolder tagItemHolder && item instanceof TagItem tagItem) {
			tagItemHolder.bindView(holder, tagItem, nightMode);
		} else if (holder instanceof AddItemHolder addItemHolder) {
			addItemHolder.bindView();
		} else if (holder instanceof BasicInfoHolder basicInfoHolder) {
			basicInfoHolder.bindView();
		} else if (holder instanceof AddOpeningHoursHolder addOpeningHoursHolder) {
			addOpeningHoursHolder.bindView();
		} else if (holder instanceof OpenTimeListHolder openTimeListHolder && item instanceof OpenHoursItem openHoursItem) {
			openTimeListHolder.bindView(openHoursItem);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (!Algorithms.isEmpty(payloads) && payloads.get(0) instanceof Integer payLoadInteger) {
			if (holder instanceof DescriptionItemHolder descriptionItemHolder) {
				switch (payLoadInteger) {
					case PAYLOAD_NAME:
						descriptionItemHolder.updateName();
						break;
					case PAYLOAD_AMENITY:
						descriptionItemHolder.updatePoiType();
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

	private void showKeyboard(@NonNull View view) {
		view.requestFocus();
		if (activity != null) {
			AndroidUtils.showSoftKeyboard(activity, view);
		}
	}

	public void clearFocus() {
		currentTagEditText.clearFocus();
	}

	public void removeItem(int position) {
		notifyItemRemoved(position);
		items.remove(position);
	}

	class TagItemHolder extends RecyclerView.ViewHolder {

		private final OsmandApplication app;
		private final OsmandTextFieldBoxes tagFB;
		private final OsmandTextFieldBoxes valueFB;
		private final ExtendedEditText tagEditText;
		private final AutoCompleteTextView valueEditText;
		private final View deleteButton;

		public TagItemHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
			super(itemView);
			this.app = app;
			tagFB = itemView.findViewById(R.id.tag_fb);
			valueFB = itemView.findViewById(R.id.value_fb);
			tagEditText = itemView.findViewById(R.id.tagEditText);
			valueEditText = itemView.findViewById(R.id.valueEditText);
			deleteButton = itemView.findViewById(R.id.delete_button);
		}

		public void focusOnTagEdit() {
			showKeyboard(tagEditText);
		}

		public void bindView(@NonNull RecyclerView.ViewHolder holder, @NonNull TagItem tagItem, boolean nightMode) {
			Drawable deleteDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, !nightMode);

			String tag = tagItem.tag();
			String value = tagItem.value();
			tagFB.setClearButton(deleteDrawable);
			tagFB.post(tagFB::hideClearButton);

			valueFB.setClearButton(deleteDrawable);
			valueFB.post(valueFB::hideClearButton);

			tagEditText.setText(tag);
			tagEditText.setAdapter(tagAdapter);
			tagEditText.setThreshold(1);
			if (tagItem.isNew()) {
				showKeyboard(tagEditText);
			}

			String[] previousTag = {tag};
			tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (!hasFocus) {
					tagFB.hideClearButton();
					if (!getData().isInEdit()) {
						String s = tagEditText.getText().toString();
						if (!previousTag[0].equals(s)) {
							getData().removeTag(previousTag[0]);
							getData().putTag(s, valueEditText.getText().toString());
							previousTag[0] = s;
						}
					}
				} else {
					tagFB.showClearButton();
					currentTagEditText = tagEditText;
					tagAdapter.getFilter().filter(tagEditText.getText());
				}
			});

			valueEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(AMENITY_TEXT_LENGTH)});
			valueEditText.setText(value);
			valueEditText.setAdapter(valueAdapter);
			valueEditText.setThreshold(3);
			valueEditText.addTextChangedListener(new SimpleTextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					if (!getData().isInEdit()) {
						getData().putTag(tagEditText.getText().toString(), s.toString());
					}
				}
			});

			valueEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (hasFocus) {
					valueFB.showClearButton();
					valueAdapter.getFilter().filter(valueEditText.getText());
				} else {
					valueFB.hideClearButton();
				}
			});

			deleteButton.setOnClickListener(v -> {
				int itemPosition = holder.getAdapterPosition();
				removeItem(itemPosition);
				getData().removeTag(tagEditText.getText().toString());
			});
		}

	}

	class DescriptionItemHolder extends RecyclerView.ViewHolder {
		private final TextView nameTextView;
		private final TextView amenityTagTextView;
		private final TextView amenityTextView;

		public DescriptionItemHolder(@NonNull View itemView) {
			super(itemView);
			this.nameTextView = itemView.findViewById(R.id.nameTextView);
			this.amenityTagTextView = itemView.findViewById(R.id.amenityTagTextView);
			this.amenityTextView = itemView.findViewById(R.id.amenityTextView);
		}

		public void bindView() {
			updateName();
			updatePoiType();
		}

		public void updateName() {
			nameTextView.setText(getData().getTag(OSMSettings.OSMTagKey.NAME.getValue()));
		}

		public void updatePoiType() {
			PoiType pt = getData().getPoiTypeDefined();
			if (pt != null) {
				amenityTagTextView.setText(pt.getEditOsmTag());
				amenityTextView.setText(pt.getEditOsmValue());
			} else {
				PoiCategory category = getData().getPoiCategory();
				if (category != null) {
					amenityTagTextView.setText(category.getDefaultTag());
				} else {
					amenityTagTextView.setText(R.string.tag_poi_amenity);
				}
				amenityTextView.setText(getData().getPoiTypeString());
			}
		}
	}

	class AddItemHolder extends RecyclerView.ViewHolder {

		private final View addTagButton;

		public AddItemHolder(@NonNull View itemView) {
			super(itemView);
			this.addTagButton = itemView.findViewById(R.id.addTagButton);
		}

		public void bindView() {
			addTagButton.setOnClickListener(v -> editPoiListener.onAddNewItem(getAdapterPosition(), TYPE_ADD_TAG));
		}
	}

	class BasicInfoHolder extends RecyclerView.ViewHolder {

		private final EditText streetEditText;
		private final EditText houseNumberEditText;
		private final EditText phoneEditText;
		private final EditText webSiteEditText;
		private final EditText descriptionEditText;

		public BasicInfoHolder(@NonNull View itemView) {
			super(itemView);
			streetEditText = itemView.findViewById(R.id.streetEditText);
			houseNumberEditText = itemView.findViewById(R.id.houseNumberEditText);
			phoneEditText = itemView.findViewById(R.id.phoneEditText);
			webSiteEditText = itemView.findViewById(R.id.webSiteEditText);
			descriptionEditText = itemView.findViewById(R.id.descriptionEditText);
		}

		protected void addTextWatcher(String tag, EditText e) {
			e.addTextChangedListener(new SimpleTextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					EditPoiData data = getData();
					if (data != null && !data.isInEdit()) {
						if (!TextUtils.isEmpty(s)) {
							data.putTag(tag, s.toString());
						} else if (editPoiListener.isBasicTagsInitialized() && editPoiListener.isFragmentResumed()) {
							data.removeTag(tag);
						}
					}
				}

			});
		}

		public void bindView() {
			addTextWatcher(OSMSettings.OSMTagKey.ADDR_STREET.getValue(), streetEditText);
			addTextWatcher(OSMSettings.OSMTagKey.WEBSITE.getValue(), webSiteEditText);
			addTextWatcher(OSMSettings.OSMTagKey.PHONE.getValue(), phoneEditText);
			addTextWatcher(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(), houseNumberEditText);
			addTextWatcher(OSMSettings.OSMTagKey.DESCRIPTION.getValue(), descriptionEditText);
			InputFilter[] lengthLimit = editPoiListener.getLengthLimit();
			streetEditText.setFilters(lengthLimit);
			houseNumberEditText.setFilters(lengthLimit);
			phoneEditText.setFilters(lengthLimit);
			webSiteEditText.setFilters(lengthLimit);
			descriptionEditText.setFilters(lengthLimit);

			AndroidUtils.setTextHorizontalGravity(streetEditText, Gravity.START);
			AndroidUtils.setTextHorizontalGravity(houseNumberEditText, Gravity.START);
			AndroidUtils.setTextHorizontalGravity(phoneEditText, Gravity.START);
			AndroidUtils.setTextHorizontalGravity(webSiteEditText, Gravity.START);
			AndroidUtils.setTextHorizontalGravity(descriptionEditText, Gravity.START);

			EditPoiData data = getData();
			if (data == null) {
				return;
			}
			Map<String, String> tagValues = data.getTagValues();
			streetEditText.setText(tagValues.get(OSMSettings.OSMTagKey.ADDR_STREET.getValue()));
			houseNumberEditText.setText(tagValues.get(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue()));
			phoneEditText.setText(tagValues.get(OSMSettings.OSMTagKey.PHONE.getValue()));
			webSiteEditText.setText(tagValues.get(OSMSettings.OSMTagKey.WEBSITE.getValue()));
			descriptionEditText.setText(tagValues.get(OSMSettings.OSMTagKey.DESCRIPTION.getValue()));
		}
	}

	class AddOpeningHoursHolder extends RecyclerView.ViewHolder {

		private final View button;

		public AddOpeningHoursHolder(@NonNull View itemView) {
			super(itemView);
			button = itemView.findViewById(R.id.addOpeningHoursButton);
		}

		public void bindView() {
			button.setOnClickListener(v -> editPoiListener.onAddNewItem(getAdapterPosition(), TYPE_ADD_OPENING_HOURS));
		}
	}

	class OpenTimeListHolder extends RecyclerView.ViewHolder {

		private final ImageView clockIconImageView;
		private final TextView daysTextView;
		private final LinearLayout timeListContainer;
		private final ImageButton deleteItemImageButton;
		private final Button addTimeSpanButton;

		public OpenTimeListHolder(@NonNull View itemView) {
			super(itemView);
			clockIconImageView = itemView.findViewById(R.id.clockIconImageView);
			daysTextView = itemView.findViewById(R.id.daysTextView);
			timeListContainer = itemView.findViewById(R.id.timeListContainer);
			deleteItemImageButton = itemView.findViewById(R.id.deleteItemImageButton);
			addTimeSpanButton = itemView.findViewById(R.id.addTimeSpanButton);
		}

		public void bindView(@NonNull OpenHoursItem openHoursItem) {
			OpeningHoursParser.OpeningHours openingHours = openingHoursAdapter.getOpeningHours();
			int position = openHoursItem.position();
			clockIconImageView.setImageDrawable(openingHoursAdapter.getClockDrawable());

			if (openingHours.getRules().get(position) instanceof OpeningHoursParser.BasicOpeningHourRule rule) {
				StringBuilder stringBuilder = new StringBuilder();
				rule.appendDaysString(stringBuilder);

				daysTextView.setText(stringBuilder.toString());
				daysTextView.setOnClickListener(v -> {
					OpeningHoursDaysDialogFragment fragment =
							OpeningHoursDaysDialogFragment.createInstance(rule, position);
					fragment.show(getManager(), "OpenTimeDialogFragment");
				});

				TIntArrayList startTimes = rule.getStartTimes();
				TIntArrayList endTimes = rule.getEndTimes();
				for (int i = 0; i < startTimes.size(); i++) {
					View timeFromToLayout = LayoutInflater.from(activity)
							.inflate(R.layout.time_from_to_layout, timeListContainer, false);
					TextView openingTextView = timeFromToLayout.findViewById(R.id.openingTextView);
					openingTextView.setText(Algorithms.formatMinutesDuration(startTimes.get(i)));

					TextView closingTextView = timeFromToLayout.findViewById(R.id.closingTextView);
					closingTextView.setText(Algorithms.formatMinutesDuration(endTimes.get(i)));

					openingTextView.setTag(i);
					openingTextView.setOnClickListener(v -> {
						int index = (int) v.getTag();
						OpeningHoursHoursDialogFragment.createInstance(rule, position, true, index)
								.show(getManager(), "OpeningHoursHoursDialogFragment");
					});
					closingTextView.setTag(i);
					closingTextView.setOnClickListener(v -> {
						int index = (int) v.getTag();
						OpeningHoursHoursDialogFragment.createInstance(rule, position, false, index)
								.show(getManager(), "OpeningHoursHoursDialogFragment");
					});

					ImageButton deleteTimeSpanImageButton = timeFromToLayout
							.findViewById(R.id.deleteTimespanImageButton);
					deleteTimeSpanImageButton.setImageDrawable(openingHoursAdapter.getDeleteDrawable());
					int timeSpanPosition = i;
					deleteTimeSpanImageButton.setOnClickListener(v -> {
						if (startTimes.size() == 1) {
							openingHours.getRules().remove(position);
						} else {
							rule.deleteTimeRange(timeSpanPosition);
						}
						openingHoursAdapter.updateHoursData();
						notifyDataSetChanged();
					});
					timeListContainer.addView(timeFromToLayout);
				}

				deleteItemImageButton.setVisibility(View.GONE);
				addTimeSpanButton.setVisibility(View.VISIBLE);
				addTimeSpanButton.setOnClickListener(v -> OpeningHoursHoursDialogFragment.createInstance(rule, position, true,
						startTimes.size()).show(getManager(),
						"TimePickerDialogFragment"));
			} else if (openingHours.getRules().get(position) instanceof OpeningHoursParser.UnparseableRule) {
				daysTextView.setText(openingHours.getRules().get(position).toRuleString());
				timeListContainer.removeAllViews();

				deleteItemImageButton.setVisibility(View.VISIBLE);
				deleteItemImageButton.setImageDrawable(openingHoursAdapter.getDeleteDrawable());
				deleteItemImageButton.setOnClickListener(v -> {
					openingHours.getRules().remove(position);
					openingHoursAdapter.updateHoursData();
					notifyDataSetChanged();
				});
				addTimeSpanButton.setVisibility(View.GONE);
			}
		}

		private FragmentManager getManager() {
			return editPoiListener.getChildFragmentManager();
		}

	}


	public interface EditPoiListener {
		void onAddNewItem(int position, int buttonType);

		InputFilter[] getLengthLimit();

		FragmentManager getChildFragmentManager();

		default boolean isFragmentResumed() {
			return false;
		}

		default boolean isBasicTagsInitialized() {
			return false;
		}
	}
}

