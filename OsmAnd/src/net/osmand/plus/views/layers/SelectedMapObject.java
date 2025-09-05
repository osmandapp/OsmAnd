package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;

public record SelectedMapObject(@NonNull Object object, @Nullable IContextMenuProvider provider) {}
