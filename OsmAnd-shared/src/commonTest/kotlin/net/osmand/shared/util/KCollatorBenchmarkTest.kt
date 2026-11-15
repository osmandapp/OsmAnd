package net.osmand.shared.util

import net.osmand.shared.api.KStringMatcherMode
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * Head to head benchmark of [KCollatorStringMatcher] against a literal port of the java matcher,
 * the one that asks a collator whether two stretches of text are equal once per starting position
 * and once per length. Runs on every target, so the same workload can be compared between JVM,
 * Android and Kotlin/Native.
 *
 * This is the measurement behind the decision not to port the java algorithm as it stands. On the
 * JVM a collator call is a call; on Kotlin/Native there is no collator in the standard library, so
 * a literal port has to reach for `NSString.compare`, and every call converts a Kotlin string,
 * allocates an NSString and crosses into Foundation. [LiteralCollatorMatcher] is that port, and it
 * is here rather than in the module because nothing should use it.
 *
 * The two do not answer identically - the platform collator of a literal port is `NSString` on iOS
 * and `java.text.Collator` on the jvm, and neither treats separators the way the other does - so
 * the hit counts are printed rather than asserted. They are close enough that the two are doing
 * comparable amounts of work.
 *
 * **Disabled on purpose.** Measuring takes seconds and the timings are noise in a normal test run.
 * What the matcher has to answer is covered by `CollatorCompatTest` in OsmAnd-java, which does run.
 *
 * To measure, remove the `@Ignore` below and put it back afterwards.
 *
 * On the JVM:
 * ```
 * ./gradlew :OsmAnd-shared:jvmTest --tests "*KCollatorBenchmarkTest"
 * ```
 *
 * On Kotlin/Native you must use the **release** binary. The default `iosSimulatorArm64Test` task
 * builds `debugTest`, which is linked without LLVM optimisations:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * xcrun simctl spawn --standalone <simulator-udid> \
 *   OsmAnd-shared/build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.util.KCollatorBenchmarkTest.*'
 * ```
 *
 * Measured over 14 queries and 304 road names, microseconds per `matches` call, best of 3 rounds,
 * the native side on a release binary on an iPhone 17 Pro simulator:
 * ```
 *                            jvm key   jvm collator    native key   native collator
 * CHECK_CONTAINS                 0.37          27.77          0.77             35.42
 * CHECK_STARTS_FROM_SPACE        0.29           0.80          0.76              1.28
 * CHECK_ONLY_STARTS_WITH         0.30           0.44          0.77              0.95
 * ```
 * The contains mode is where the java algorithm pays for its double loop over positions and
 * lengths: around 150 collator calls per name, against one key per name here. The jvm collator
 * column lands within 2% of the real `CollatorStringMatcher`, which `CollatorBenchmarkTest` in
 * OsmAnd-java measures on the same corpus, so this port stands in for it faithfully.
 *
 * Worth knowing for the next port: a call into Foundation costs about what a `java.text.Collator`
 * call costs, so a literal port would not have been dramatically worse on iOS than on the jvm.
 * What is slower on Kotlin/Native is the key itself, about twice the jvm, which is why the speedup
 * there is 45x and on the jvm 75x. Building the key allocates; the collator call does not.
 *
 * Nothing here asserts a timing, so a slow machine can never turn the build red.
 */
@Ignore
class KCollatorBenchmarkTest {

	@Test
	fun matchingNames() {
		val names = NAMES.split('\n')
		println("")
		println("### matching ${QUERIES.size} queries against ${names.size} names")
		println("    best of $MEASURED_ROUNDS rounds after $WARMUP_ROUNDS warmup rounds")
		for (mode in MODES) {
			var keyHits = 0
			val keyTime = measure {
				keyHits = 0
				for (query in QUERIES) {
					val matcher = KCollatorStringMatcher(query, mode)
					for (name in names) {
						if (matcher.matches(name)) {
							keyHits++
						}
					}
				}
			}
			var literalHits = 0
			val literalTime = measure {
				literalHits = 0
				for (query in QUERIES) {
					val matcher = LiteralCollatorMatcher(query, mode)
					for (name in names) {
						if (matcher.matches(name)) {
							literalHits++
						}
					}
				}
			}
			val calls = QUERIES.size * names.size
			println(
				"  ${mode.name.padEnd(37)} key ${format(keyTime)} ms" +
						"   collator ${format(literalTime)} ms" +
						"   speedup ${format(literalTime / keyTime)}x" +
						"   per call ${format(keyTime * 1000 / calls)} vs ${format(literalTime * 1000 / calls)} us" +
						"   hits $keyHits/$literalHits"
			)
		}
		println("")
	}

	private fun measure(block: () -> Unit): Double {
		repeat(WARMUP_ROUNDS) { block() }
		var best = Double.MAX_VALUE
		repeat(MEASURED_ROUNDS) {
			val mark = TimeSource.Monotonic.markNow()
			block()
			val elapsed = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (elapsed < best) {
				best = elapsed
			}
		}
		return best
	}

	private fun format(value: Double): String {
		val scaled = (value * 100).toLong()
		return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}".padStart(9)
	}

	/**
	 * `net.osmand.CollatorStringMatcher` as it stands, on top of [primaryCollator]: the same loops
	 * over positions and lengths, each step cutting a substring and asking the collator.
	 */
	private class LiteralCollatorMatcher(part: String, private val mode: KStringMatcherMode) {

		private val collator = primaryCollator()
		private val part = KCollationKey.lowercaseAndAlignChars(part)

		fun matches(name: String): Boolean = cmatches(name, part)

		private fun cmatches(fullName: String, part: String): Boolean {
			if (fullName.indexOf('-') != -1 && cmatches(fullName.replace("-", ""), part)) {
				return true
			}
			val searchIn = KCollationKey.lowercaseAndAlignChars(fullName)
			return when (mode) {
				KStringMatcherMode.CHECK_CONTAINS -> ccontains(searchIn, part)
				KStringMatcherMode.CHECK_EQUALS_FROM_SPACE -> cstartsWith(searchIn, part, true, true, true)
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE -> cstartsWith(searchIn, part, true, true, false)
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING -> cstartsWith(searchIn, part, false, true, false)
				KStringMatcherMode.CHECK_ONLY_STARTS_WITH -> cstartsWith(searchIn, part, true, false, false)
				KStringMatcherMode.CHECK_EQUALS -> cstartsWith(searchIn, part, false, false, true)
				KStringMatcherMode.MULTISEARCH -> cstartsWith(part, searchIn, true, true, true)
			}
		}

		private fun ccontains(base: String, part: String): Boolean {
			if (base.length <= part.length) {
				return collator.equals(base, part)
			}
			for (pos in 0..base.length - part.length + 1) {
				val temp = base.substring(pos, minOf(pos + part.length * 2, base.length))
				for (length in temp.length downTo 0) {
					if (collator.equals(temp.substring(0, length), part)) {
						return true
					}
				}
			}
			return false
		}

		private fun cstartsWith(
			searchIn: String,
			theStart: String,
			checkBeginning: Boolean,
			checkSpaces: Boolean,
			equals: Boolean
		): Boolean {
			val searchInLength = searchIn.length
			val startLength = theStart.length
			if (startLength == 0) {
				return true
			}
			if (startLength > searchInLength) {
				return false
			}
			if (checkBeginning && collator.equals(searchIn.substring(0, startLength), theStart)) {
				if (!equals) {
					return true
				}
				if (startLength == searchInLength || isSpace(searchIn[startLength])) {
					return true
				}
			}
			if (checkSpaces) {
				for (i in 1..searchInLength - startLength) {
					if (!isWordStart(searchIn, i, theStart)) {
						continue
					}
					if (!collator.equals(searchIn.substring(i, i + startLength), theStart)) {
						continue
					}
					if (!equals) {
						return true
					}
					if (i + startLength == searchInLength || isSpace(searchIn[i + startLength])) {
						return true
					}
				}
			}
			if (!checkBeginning && !checkSpaces && equals) {
				return collator.equals(searchIn, theStart)
			}
			return false
		}

		private fun isWordStart(searchIn: String, index: Int, part: String): Boolean {
			if (!isSpace(searchIn[index - 1])) {
				return false
			}
			val current = searchIn[index]
			if (!isSpace(current)) {
				return true
			}
			return current == '-' && part.length > 1 && part[0] == '-' && part[1].isDigit()
		}

		private fun isSpace(c: Char): Boolean = !c.isLetter() && !c.isDigit()
	}

	companion object {

		private const val WARMUP_ROUNDS = 2
		private const val MEASURED_ROUNDS = 3

		private val MODES = listOf(
			KStringMatcherMode.CHECK_CONTAINS,
			KStringMatcherMode.CHECK_STARTS_FROM_SPACE,
			KStringMatcherMode.CHECK_ONLY_STARTS_WITH
		)

		private val QUERIES = listOf(
			"a", "st", "str", "stras", "strasse", "ring", "haupt", "bahn", "weg 12",
			"\u0443\u043b", "\u0443\u043b\u0438\u0446\u0430", "\u043b\u0435\u043d\u0438\u043d\u0430", "\u043f\u0440\u043e\u0441\u043f\u0435\u043a\u0442", "42"
		)

		/** Road names of the obf files the routing tests ship with, so both platforms match the same text. */
		private const val NAMES = """S6
Semmering-Schnellstraße
L118
Wiener Straße
Edlach-Schönebnerweg
Blumberger Damm
Edlachweg
Каширское шоссе
Шипиловский проезд
улица Борисовские Пруды
Highway 161 Service Road
Valley View Lane
President George Bush Turnpike
PGBT
North Macarthur Boulevard
Las Colinas Boulevard;Riverside Drive
I 635 West
Den Haag;Utrecht
A10
3
Amsterdam-Bos en Lommer;-Slotermeer
S104
4
S103;S104
E 22
Einsteinweg
Amsterdam-Geuzenveld
RING A10
Baywater Drive
us:tx
TX 289
289
Preston Road
Interstate Highway 635 Frontage Road
Olympus Boulevard
us:i
I 635 (TX)
I 635
635
Lyndon B Johnson Freeway
North MacArthur Boulevard
President George Bush Turnpike South
Silver Creek Drive
Lorimar Drive
Las Colinas Boulevard
Warener Straße
Bos en Lommerplein
Bos en Lommer
Bos en Lommerweg
N201
Cruquiusweg
Hoofdweg
Spieringweg
Kruisweg
B 22
NEW 41
||B 22
A 6
E 50
Lyndon Baines Johnson Freeway
President George Bush Turnpike North
Interstate Highway 635 Service Road
DFW Airport NORTH ENTRY
Valley Ranch Parkway East
I 635 East
DFW Airport NORTH ENTRY;Las Colinas Boulevard;MacArthur Boulevard
Buffalo Boulevard
Las Colinas Boulevard North
La Villita Boulevard
Colwell Boulevard
Riverside Drive
Las Colinas Ridge
Las Colinas Boulevard;MacArthur Boulevard
I-635 Service Road
MacArthur Boulevard
Wiltzanghlaan
Leeuwarden;Zaanstad
Haarlem
S103
Groene Wissel: Amsterdam-Sloterdijk
http://www.wandelzoekpagina.nl/groene_wissels/lijst.php
lwn
102570
Admiraal de Ruijterweg
Mercer Parkway
Ringweg Zuid
S108
Amstelveenseweg
Amsterdam-Oud Zuid
8
Zaanstad;Den Haag
Den Haag;Zaanstad
Groningen;Utrecht
E 19
Amersfoort;Utrecht
black
de:Brückenradweg
Ruta de los Peregrinos - parte Alemania
Szlak Pielgrzymi - część Niemcy
http://www.brueckenradweg.de
OS-HB west
Bundesrepublik Deutschland
.
OS-HB ost
white_4
white_2
de:Pilgerroute (D7)
D7
Wilhelm-Kaisen-Brücke
OS-HB O
rcn
La route des pèlerins - portion Allemagne
EV3 Pilgerweg - Teil Deutschland
Brückenradweg Ostroute
ncn
28211
EVPTD
Pyhiinvaeltajan reitti - osa Saksa
OS-HB W
icn
27991
27917
28099
Brückenradweg Westroute
28059
GEEST
de:Geestweg
Geest
Geestweg
Herrlichkeit
http://www.jakobswege-norddeutschland.de/43360/55001.html
Weser
http://weser-radweg.de/
BahnRadRoute Weser - Lippe
nwn
blue
28089
Pèlerinage_de_Saint-Jacques-de-Compostelle
Jakobsweg via Baltica, Niedersachsen (Mitte)
WESER
27987
Gelbe stilisierte Jakobsmuschel auf blauem Grund
de:BahnRadRoute Weser-Lippe
shell_modern
Weserradweg
Information Weserbund e.V.
Usedom - Stralsund - Rostock - Lübeck - Hamburg - Bremen - Osnabrück
Jw vB
de:Jakobsweg
de:Weserradweg
27865
WL
350 km
Vom Zusammenfluss von Werra und Fulda bei Hann. Münden bis zur Mündung in die Nordsee bei Bremerhaven dann weiter bis Cuxhaven
Altonaer Straße
Spreeweg
Oud Zuid
Zuiderhof|Olympisch Stadion;Centrum|Den Haag;Zaanstad|Den Haag;Zaanstad|Utrecht;Amersfoort
Amsterdam-Oud Zuid;-Buitenveldert
B 28
Deutsche Alleenstraße, Abschnitt 8 (Freudenstadt - Reichenau)
Schwarzwald-Bäderstraße
B 28 [Straßburg - Herrenberg B 14]
B 28 (Herrenberg B 14 - Straßburg]
Stuttgarter Straße
L 409
Grüntaler Straße
105 km
7057279
Schwarzwälder Höhenradweg Ost
SHO
Pforzheim > Neuhausen > Althengstett (Calw) > Sulz am Eck (Wildberg) > Horb-Altheim > Waldachtal-Lützenhardt > Freudenstadt
white_3
Ortsstraße
6月17日通り
B 5
六月十七日大街
B 2
Straße des 17. Juni
B 2;B 5
רחוב 17 ביוני
ulica 17 Czerwca
Улица 17 Июня
Großer Stern
Hofjägerallee
Bierweg
Äußere Bayreuther Straße
Thurn-und-Taxis-Straße
B 403
Lingener Straße
Wietmarscher Straße
Liststiege
Rad-Hauptroute Süd
lcn
Senator für Umwelt, Bau und Verkehr
HR
28243
28203
D9
Porta
[D9] Weser-Romantische Straße [Niedersachsen: Bremen - Porta]
Bremen
Martinistraße
Rad-Hauptroute Mitte
28003
Tiefer
S 172
Nürnberger Straße
Prag;Pirna|Prag;Pirna|Chemnitz;Leipzig;Universität|Chemnitz;Leipzig;Universität
Fritz-Foerster-Platz
A 4; Meißen; Freiberg; Freital
B 170
Bergstraße
Pirna|Dippoldiswalde;Universität|Dippoldiswalde;Universität
A 17; Prag; Dippoldiswalde; Universität
Zentrum
Universität|Universität|Meißen;Freiberg;Freital|Meißen;Freiberg;Freital
Pirna
Meißen;Freiberg|Meißen;Freiberg|Zentrum|Zentrum
Zellescher Weg
Platz der Vereinten Nationen
RN9
Autopista Ernesto Guevara
L 353
Roldán;Carcarañá;Córdoba
Autopista Ernesto Che Guevara
RP34-S
Miguel Galindo
Colectora de autopista
Enlace hacia Miguel Galindo
Funes;Kentucky;Pérez
Meißen;Freiberg|Meißen;Freiberg|Zentrum|Zentrum;Pirna
K 4725
Ziegelstraße
Oldenzaa;Bad Bentheim
B 213;B 403
Neuenhaus
Coevorden;Neuenhaus
B 213
Lingen;Lohne
Osttangente
L 67
Wietmarschen
Pérez;Kentucky
Funes
Rosario
Ruta Provincial Secundaria 34
N219
1e Tochtweg
Nieuwerkerk a/d IJssel
17
Oorder Weg
Lingen
Schielandweg
Capelle a/d IJssel;Rotterdam
A20
Balgebrückstraße
бул. Ген. Скобелев
bul. Gen. Skobelev
бул. Христо Ботев
bul. Hristo Botev
E 25
Хан Аспарух
Han Asparuh
Europalaan
Nieuwerkerk a/d IJssel;Zevenhuizen;Rotterdam-Nesselande
Utrecht;Gouda
Reichsstraße
Blommesteinsingel
Plaswijckweg
Weerestein
Waterkers
Burgemeester van Reenensingel
South Kent Des Moines Road
Waterruit
WA 99
Pacific Highway South
WA 509
WA 99;WA 509
WA 516
Groen van Prinsterersingel
Steubenplatz
Olympische Straße
N208
Leidsestraat
Hyacinthenlaan
N207
Leimuiderweg
Oberspreestraße
Berliner Außenring
6126
Spindlersfelder Straße
L 1008
Invalidenstraße
Senatsverwaltung für Stadtentwicklung Berlin
rwn
blue bar on white ground with 5
Wanderweg an der Panke von der Quelle (Bernau im Land Brandenburg) bis zur Mündung (Land Berlin)
Nord-Süd-Weg (Pankeweg)
blauer Balken auf weiß mit 5
white
5
blue_bar
28611"""
	}
}
