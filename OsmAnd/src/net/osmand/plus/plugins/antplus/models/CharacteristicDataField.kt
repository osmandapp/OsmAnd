package net.osmand.plus.plugins.antplus.models

import androidx.annotation.StringRes

data class CharacteristicDataField(
    @StringRes val nameId: Int,
    val value: String,
    @StringRes val unitNameId: Int
)
