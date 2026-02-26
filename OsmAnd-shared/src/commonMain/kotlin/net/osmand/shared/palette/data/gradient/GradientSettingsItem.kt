package net.osmand.shared.palette.data.gradient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradientSettingsItem(
	@SerialName("type_name") val typeName: String,
	@SerialName("palette_name") val paletteName: String,
	@SerialName("index") val index: Int
)