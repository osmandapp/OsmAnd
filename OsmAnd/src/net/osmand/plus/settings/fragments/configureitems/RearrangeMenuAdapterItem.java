package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.fragments.configureitems.RearrangeMenuItemsAdapter.AdapterItemType;

public class RearrangeMenuAdapterItem {

	protected final AdapterItemType itemType;
	protected final Object value;

	public RearrangeMenuAdapterItem(@NonNull AdapterItemType itemType, @NonNull Object value) {
		this.itemType = itemType;
		this.value = value;
	}
}
