package net.osmand.shared.data

/**
 * Postcode formats by country, to bring what was typed to the form the address index stores.
 *
 * A copy of `net.osmand.data.Postcode` in OsmAnd-java, which stays there for android and tools.
 * The replacement is expanded here rather than handed to the regex engine, so that a group that did
 * not take part in the match gives nothing on every platform, as `java.util.regex` does.
 */
object Postcode {

	//	© CC BY 3.0 2016 GeoNames.org
	//	with adaptations
	private val rules: Map<String, List<String>> = mapOf(
		"Algeria" to                                      listOf("(?i)(?:DZ-?)?(\\d{5})", "$1"),
		"Andorra" to                                      listOf("(?i)(?:AD-?)?(\\d{3})", "$1"),
		"Argentina" to                                    listOf("(?i)(?:AR-?)?([A-Z]\\d{4}[A-Z]{3}|\\d{4})", "$1"),
		"Armenia" to                                      listOf("(?i)(?:AM-?)?(\\d{6})", "$1"),
		"Australia-oceania" to                            listOf("(?i)(?:AU-?)?(\\d{4})", "$1"),
		"Austria" to                                      listOf("(?i)(?:AT-?)?(\\d{4})", "$1"),
		"Azerbaijan" to                                   listOf("(?i)(?:AZ-?)?(\\d{4})", "$1"),
		"Bahrain" to                                      listOf("(?i)(?:BH-?)?(\\d{3}\\d?)", "$1"),
		"Bangladesh" to                                   listOf("(?i)(?:BD-?)?(\\d{4})", "$1"),
		"Barbados" to                                     listOf("(?i)(?:BB-?)?(\\d{5})", "$1"),
		"Belarus" to                                      listOf("(?i)(?:BY-?)?(\\d{6})", "$1"),
		"Belgium" to                                      listOf("(?i)(?:BE-?)?(\\d{4})", "$1"),
		"Bermuda" to                                      listOf("(?i)(?:BM-?)?([A-Z]{2})\\W*(\\d{2})", "$1$2"),
		"Bosnia-herzegovina" to                           listOf("(?i)(?:BA-?)?(\\d{5})", "$1"),
		"Brazil" to                                       listOf("(?i)(?:BR-?)?(\\d{5})\\W*(\\d{3})", "$1-$2"),
		"Brunei" to                                       listOf("(?i)(?:BN-?)?([A-Z]{2})\\W*(\\d{4})", "$1$2"),
		"Bulgaria" to                                     listOf("(?i)(?:BG-?)?(\\d{4})", "$1"),
		"Cambodia" to                                     listOf("(?i)(?:KH-?)?(\\d{5})", "$1"),
		"Canada" to                                       listOf("(?i)(?:CA-?)?([ABCEGHJKLMNPRSTVXY]\\d[ABCEGHJKLMNPRSTVWXYZ])\\W*(\\d[ABCEGHJKLMNPRSTVWXYZ]\\d)$", "$1 $2"),
		"Cape-verde" to                                   listOf("(?i)(?:CV-?)?(\\d{4})", "$1"),
		"Chile" to                                        listOf("(?i)(?:CL-?)?(\\d{7})", "$1"),
		"China" to                                        listOf("(?i)(?:CN-?)?(\\d{6})", "$1"),
		"Christmas-island" to                             listOf("(?i)(?:CX-?)?(\\d{4})", "$1"),
		"Costa-rica" to                                   listOf("(?i)(?:CR-?)?(\\d{4})", "$1"),
		"Croatia" to                                      listOf("(?i)(?:HR-?)?(\\d{5})", "$1"),
		"Cuba" to                                         listOf("(?i)(?:C[PU]-?)?(\\d{5})", "$1"),
		"Cyprus" to                                       listOf("(?i)(?:CY-?)?(\\d{4})", "$1"),
		"Czech-republic" to                               listOf("(?i)(?:CZ-?)?(\\d{5})", "$1"),
		"Denmark" to                                      listOf("(?i)(?:DK-?)?(\\d{4})", "$1"),
		"Dominican-republic" to                           listOf("(?i)(?:DO-?)?(\\d{5})", "$1"),
		"Ecuador" to                                      listOf("(?i)(?:EC-?)?(\\d{6})", "$1"),
		"Egypt" to                                        listOf("(?i)(?:EG-?)?(\\d{5})", "$1"),
		"El-salvador" to                                  listOf("(?i)(?:SV-?)?(\\d{4})", "$1"),
		"Estonia" to                                      listOf("(?i)(?:EE-?)?(\\d{5})", "$1"),
		"Ethiopia" to                                     listOf("(?i)(?:ET-?)?(\\d{4})", "$1"),
		"Faroe-islands" to                                listOf("(?i)(?:FO-?)?(\\d{3})", "$1"),
		"Finland" to                                      listOf("(?i)(?:FI-?)?(\\d{5})", "$1"),
		"France" to                                       listOf("(?i)(?:FR-?)?(\\d{5})", "$1"),
		"French-guiana" to                                listOf("(?i)(?:GF-?)?((97|98)3\\d{2})", "$1"),
		"French-southern-and-antarctic-lands" to          listOf("(?i)(?:PF-?)?((97|98)7\\d{2})", "$1"),
		"GB" to                                           listOf("(?i)(?:UK-?)?([A-Z]{1,2}[0-9]{1,2}[A-Z]?)\\W*([0-9][A-Z]{2})", "$1 $2"),
		"Georgia" to                                      listOf("(?i)(?:GE-?)?(\\d{4})", "$1"),
		"Germany" to                                      listOf("(?i)(?:DE-?)?(\\d{5})", "$1"),
		"Greece" to                                       listOf("(?i)(?:GR-?)?(\\d{5})", "$1"),
		"Greenland" to                                    listOf("(?i)(?:GL-?)?(\\d{4})", "$1"),
		"Guadeloupe" to                                   listOf("(?i)(?:GP-?)?((97|98)\\d{3})", "$1"),
		"Guatemala" to                                    listOf("(?i)(?:GT-?)?(\\d{5})", "$1"),
		"Guinea-bissau" to                                listOf("(?i)(?:GW-?)?(\\d{4})", "$1"),
		"Haiti" to                                        listOf("(?i)(?:HT-?)?(\\d{4})", "$1"),
		"Honduras" to                                     listOf("(?i)(?:HN-?)?([A-Z]{2})\\W*(\\d{4})", "$1$2"),
		"Hungary" to                                      listOf("(?i)(?:HU-?)?(\\d{4})", "$1"),
		"Iceland" to                                      listOf("(?i)(?:IS-?)?(\\d{3})", "$1"),
		"India" to                                        listOf("(?i)(?:IN-?)?(\\d{6})", "$1"),
		"Indonesia" to                                    listOf("(?i)(?:ID-?)?(\\d{5})", "$1"),
		"Iran" to                                         listOf("(?i)(?:IR-?)?(\\d{10})", "$1"),
		"Iraq" to                                         listOf("(?i)(?:IQ-?)?(\\d{5})", "$1"),
		"Israel" to                                       listOf("(?i)(?:IL-?)?(\\d{5})", "$1"),
		"Italy" to                                        listOf("(?i)(?:IT-?)?(\\d{5})", "$1"),
		"Japan" to                                        listOf("(?i)(?:JP-?)?(\\d{7})", "$1"),
		"Jordan" to                                       listOf("(?i)(?:JO-?)?(\\d{5})", "$1"),
		"Kazakhstan" to                                   listOf("(?i)(?:KZ-?)?(\\d{6})", "$1"),
		"Kenya" to                                        listOf("(?i)(?:KE-?)?(\\d{5})", "$1"),
		"Kuwait" to                                       listOf("(?i)(?:KW-?)?(\\d{5})", "$1"),
		"Kyrgyzstan" to                                   listOf("(?i)(?:KG-?)?(\\d{6})", "$1"),
		"Laos" to                                         listOf("(?i)(?:LA-?)?(\\d{5})", "$1"),
		"Latvia" to                                       listOf("(?i)(?:LV-?)?(\\d{4})", "$1"),
		"Lebanon" to                                      listOf("(?i)(?:LB-?)?(\\d{4}(\\d{4})?)", "$1"),
		"Lesotho" to                                      listOf("(?i)(?:LS-?)?(\\d{3})", "$1"),
		"Liberia" to                                      listOf("(?i)(?:LR-?)?(\\d{4})", "$1"),
		"Liechtenstein" to                                listOf("(?i)(?:LI-?)?(\\d{4})", "$1"),
		"Lithuania" to                                    listOf("(?i)(?:LT-?)?(\\d{5})", "$1"),
		"Luxembourg" to                                   listOf("(?i)(?:LU-?)?(\\d{4})", "$1"),
		"Macedonia" to                                    listOf("(?i)(?:MK-?)?(\\d{4})", "$1"),
		"Madagascar" to                                   listOf("(?i)(?:MG-?)?(\\d{3})", "$1"),
		"Malaysia" to                                     listOf("(?i)(?:MY-?)?(\\d{5})", "$1"),
		"Maldives" to                                     listOf("(?i)(?:MV-?)?(\\d{5})", "$1"),
		"Malta" to                                        listOf("(?i)(?:MT-?)?([A-Z]{3})\\W*(\\d{4})", "$1 $2"),
		"Martinique" to                                   listOf("(?i)(?:MQ-?)?(\\d{5})", "$1"),
		"Mayotte" to                                      listOf("(?i)(?:YT-?)?(\\d{5})", "$1"),
		"Mexico" to                                       listOf("(?i)(?:MX-?)?(\\d{5})", "$1"),
		"Moldova" to                                      listOf("(?i)(?:MD-?)?(\\d{4})", "$1"),
		"Monaco" to                                       listOf("(?i)(?:MC-?)?(\\d{5})", "$1"),
		"Mongolia" to                                     listOf("(?i)(?:MN-?)?(\\d{6})", "$1"),
		"Montenegro" to                                   listOf("(?i)(?:ME-?)?(\\d{5})", "$1"),
		"Morocco" to                                      listOf("(?i)(?:MA-?)?(\\d{5})", "$1"),
		"Mozambique" to                                   listOf("(?i)(?:MZ-?)?(\\d{4})", "$1"),
		"Myanmar" to                                      listOf("(?i)(?:MM-?)?(\\d{5})", "$1"),
		"Nepal" to                                        listOf("(?i)(?:NP-?)?(\\d{5})", "$1"),
		"Netherlands" to                                  listOf("(?i)(?:NL-?)?(\\d{4})\\W*([A-Z]{2})", "$1$2"),
		"New-zealand" to                                  listOf("(?i)(?:NZ-?)?(\\d{4})", "$1"),
		"Nicaragua" to                                    listOf("(?i)(?:NI-?)?(\\d{7})", "$1"),
		"Niger" to                                        listOf("(?i)(?:NE-?)?(\\d{4})", "$1"),
		"Nigeria" to                                      listOf("(?i)(?:NG-?)?(\\d{6})", "$1"),
		"North-korea" to                                  listOf("(?i)(?:KP-?)?(\\d{6})", "$1"),
		"Norway" to                                       listOf("(?i)(?:NO-?)?(\\d{4})", "$1"),
		"Oman" to                                         listOf("(?i)(?:OM-?)?(\\d{3})", "$1"),
		"Pakistan" to                                     listOf("(?i)(?:PK-?)?(\\d{5})", "$1"),
		"Papua-new-guinea" to                             listOf("(?i)(?:PG-?)?(\\d{3})", "$1"),
		"Paraguay" to                                     listOf("(?i)(?:PY-?)?(\\d{4})", "$1"),
		"Philippines" to                                  listOf("(?i)(?:PH-?)?(\\d{4})", "$1"),
		"Poland" to                                       listOf("(?i)(?:PL-?)?(\\d{5})", "$1"),
		"Portugal" to                                     listOf("(?i)(?:PT-?)?(\\d{7})", "$1"),
		"Puerto-rico" to                                  listOf("(?i)(?:PR-?)?(\\d{9})", "$1"),
		"Reunion" to                                      listOf("(?i)(?:RE-?)?((97|98)(4|7|8)\\d{2})", "$1"),
		"Romania" to                                      listOf("(?i)(?:RO-?)?(\\d{6})", "$1"),
		"Russia" to                                       listOf("(?i)(?:RU-?)?(\\d{6})", "$1"),
		"Saint-helena-ascension-and-tristan-da-cunha" to  listOf("(?i)(?:SH-?)?(STHL)\\W*(1ZZ)", "$1 $2"),
		"Saint-pierre-and-miquelon" to                    listOf("(?i)(?:PM-?)?(97500)", "$1"),
		"San-marino" to                                   listOf("(?i)(?:SM-?)?(4789\\d)", "$1"),
		"Saudi-arabia" to                                 listOf("(?i)(?:SA-?)?(\\d{5})", "$1"),
		"Senegal" to                                      listOf("(?i)(?:SN-?)?(\\d{5})", "$1"),
		"Serbia" to                                       listOf("(?i)(?:RS-?)?(\\d{6})", "$1"),
		"Singapore" to                                    listOf("(?i)(?:SG-?)?(\\d{6})", "$1"),
		"Slovakia" to                                     listOf("(?i)(?:SK-?)?(\\d{5})", "$1"),
		"Slovenia" to                                     listOf("(?i)(?:SI-?)?(\\d{4})", "$1"),
		"Somalia" to                                      listOf("(?i)(?:SO-?)?([A-Z]{2})\\W*(\\d{5})", "$1$2"),
		"South-africa" to                                 listOf("(?i)(?:ZA-?)?(\\d{4})", "$1"),
		"South-korea" to                                  listOf("(?i)(?:KR-?)?(?:SEOUL)?(\\d{3})\\W*(\\d{2,3})", "$1$2"),
		"Spain" to                                        listOf("(?i)(?:ES-?)?(\\d{5})", "$1"),
		"Sri-lanka" to                                    listOf("(?i)(?:LK-?)?(\\d{5})", "$1"),
		"Sudan" to                                        listOf("(?i)(?:SD-?)?(\\d{5})", "$1"),
		"Swaziland" to                                    listOf("(?i)(?:SZ-?)?([A-Z]\\d{3})", "$1"),
		"Sweden" to                                       listOf("(?i)(?:SE-?)?(\\d{5})", "$1"),
		"Switzerland" to                                  listOf("(?i)(?:CH-?)?(\\d{4})", "$1"),
		"Taiwan" to                                       listOf("(?i)(?:TW-?)?(\\d{5})", "$1"),
		"Tajikistan" to                                   listOf("(?i)(?:TJ-?)?(\\d{6})", "$1"),
		"Thailand" to                                     listOf("(?i)(?:TH-?)?(\\d{5})", "$1"),
		"Tunisia" to                                      listOf("(?i)(?:TN-?)?(\\d{4})", "$1"),
		"Turkey" to                                       listOf("(?i)(?:TR-?)?(\\d{5})", "$1"),
		"Turkmenistan" to                                 listOf("(?i)(?:TM-?)?(\\d{6})", "$1"),
		"Turks-and-caicos-islands" to                     listOf("(?i)(?:TC-?)?(TKCA)\\W*(1ZZ)", "$1 $2"),
		"Virgin-islands-us" to                            listOf("(?i)(?:VI-?)?(\\d{5})\\W*(-\\d{4})?", "$1$2"),
		"Ukraine" to                                      listOf("(?i)(?:UA-?)?(\\d{2})\\W*(\\d{3})", "$1$2"),
		"Us" to                                           listOf("(?i)(?:US-?)?(\\d{5})\\W*(-\\d{4})?", "$1$2"),
		"Uruguay" to                                      listOf("(?i)(?:UY-?)?(\\d{5})", "$1"),
		"Uzbekistan" to                                   listOf("(?i)(?:UZ-?)?(\\d{6})", "$1"),
		"Venezuela" to                                    listOf("(?i)(?:VE-?)?(\\d{4})", "$1"),
		"Vietnam" to                                      listOf("(?i)(?:VN-?)?(\\d{6})", "$1"),
		"Zambia" to                                       listOf("(?i)(?:ZM-?)?(\\d{5})", "$1")
	)

	private fun isCountryKnown(country: String): Boolean = rules.containsKey(country)

	private fun getPattern(country: String): Regex = Regex(rules.getValue(country)[0])

	fun normalize(postcode: String, country: String): String {
		val upper = postcode.uppercase()
		var result = upper
		if (isCountryKnown(country)) {
			val replacement = rules.getValue(country)[1]
			val res = getPattern(country).replace(upper) { m -> expand(replacement, m) }
			result = res.replace("null", "")
		}
		return result
	}

	fun looksLikePostcodeStart(s: String, country: String): Boolean {
		var result = false
		if (isCountryKnown(country)) {
			result = getPattern(country).containsMatchIn(s)
		}
		result = result || LOOKS_LIKE_POSTCODE.matches(s)
		return result
	}

	private val LOOKS_LIKE_POSTCODE = Regex("(.+\\d+.*|.*\\d+.+)")

	/** `$n` of [template] replaced by group n of [m]; the templates above use nothing else. */
	private fun expand(template: String, m: MatchResult): String {
		val sb = StringBuilder()
		var i = 0
		while (i < template.length) {
			val c = template[i]
			if (c == '$' && i + 1 < template.length && template[i + 1].isDigit()) {
				sb.append(m.groups[template[i + 1] - '0']?.value ?: "")
				i += 2
			} else {
				sb.append(c)
				i++
			}
		}
		return sb.toString()
	}
}
