package net.osmand.plus.widgets.chips;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.widgets.chips.ChipsAdapter.OnSelectChipListener;

import java.util.List;

public class HorizontalChipsView extends RecyclerView {

	private final ChipsAdapter adapter;
	private final ChipsDataHolder holder = new ChipsDataHolder();

	public HorizontalChipsView(@NonNull Context context) {
		this(context, null);
	}

	public HorizontalChipsView(@NonNull Context context,
	                           @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HorizontalChipsView(@NonNull Context context,
	                           @Nullable AttributeSet attrs,
	                           int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
		setClipToPadding(false);

		DefaultAttributes defAttrs = DefaultAttributes.createInstance(context, attrs, defStyleAttr);
		adapter = new ChipsAdapter(context, holder, defAttrs);
		setAdapter(adapter);
	}

	public void setItems(@NonNull List<ChipItem> chips) {
		holder.setItems(chips);
	}

	public void setSelected(@Nullable ChipItem chip) {
		holder.setSelected(chip);
	}

	public void setOnSelectChipListener(@Nullable OnSelectChipListener listener) {
		adapter.setOnSelectChipListener(listener);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void notifyDataSetChanged() {
		adapter.notifyDataSetChanged();
	}

	@Nullable
	public ChipItem getChipById(@NonNull String id) {
		return holder.getItemById(id);
	}

	@Nullable
	public ChipItem findChipByTag(@NonNull Object tag) {
		return holder.getItemByTag(tag);
	}

	public void scrollTo(@NonNull ChipItem chip) {
		scrollToPosition(holder.indexOf(chip));
	}

	public void smoothScrollTo(@NonNull ChipItem chip) {
		smoothScrollToPosition(holder.indexOf(chip));
	}
}
