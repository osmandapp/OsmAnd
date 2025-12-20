package net.osmand.plus.download.local.dialogs.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.download.local.MultipleLocalItem;

import java.util.List;

public class LocalItemsCollection {

	@NonNull
	public final List<Object> items;
	@Nullable
	public final MultipleLocalItem currentFolder;

	public LocalItemsCollection(@NonNull List<Object> items, @Nullable MultipleLocalItem currentFolder) {
		this.items = items;
		this.currentFolder = currentFolder;
	}

	public boolean isRootFolder() {
		return currentFolder == null;
	}
}
