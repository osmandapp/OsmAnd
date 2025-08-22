package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import java.text.DecimalFormat
import java.util.Locale

object PopulationRowBehaviour : DefaultPoiAdditionalRowBehaviour() {

    override fun applyCustomRules(
	    params: PoiRowParams
    ) {
		super.applyCustomRules(params)
		with(params) {
		    val formatted = try {
			    val number = value.toInt()
			    val formatter = DecimalFormat.getInstance(Locale.US) as DecimalFormat
			    val symbols = formatter.decimalFormatSymbols
			    symbols.groupingSeparator = ' '
			    formatter.decimalFormatSymbols = symbols
			    formatter.format(number)
		    } catch (e: NumberFormatException) {
			    value
		    }
		    builder.setText(formatted)
	    }
    }
}