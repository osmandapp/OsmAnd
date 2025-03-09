package net.osmand.plus.settings.vehiclesize;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.vehiclesize.containers.Assets;
import net.osmand.plus.base.containers.Limits;

public record SizeData(@NonNull Assets assets, @NonNull Limits<Float> limits) { }
