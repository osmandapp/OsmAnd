/*
 * SentenceValidator.java
 * Copyright (C) 2010 Kimmo Tuukkanen
 *
 * This file is part of Java Marine API.
 * <http://ktuukkan.github.io/marine-api/>
 *
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.nmea.sentence



/**
 * SentenceValidator for detecting and validation of sentence Strings.
 *
 * @author Kimmo Tuukkanen
 */
object SentenceValidator {
    private val reChecksum = Regex(
        "^[$|!]{1}[A-Z0-9]{3,10}[,][\\x20-\\x7F]*[*][A-F0-9]{2}(\\r|\\n|\\r\\n|\\n\\r){0,1}$"
    )
    private val reNoChecksum = Regex(
        "^[$|!]{1}[A-Z0-9]{3,10}[,][\\x20-\\x7F]*(\\r|\\n|\\r\\n|\\n\\r){0,1}$"
    )

    /**
     *
     *
     * Tells if the specified String matches the NMEA 0183 sentence format.
     *
     *
     * String is considered as a sentence if it meets the following criteria:
     *
     *  * First character is '$' or '!'
     *  * Begin char is followed by upper-case sentence ID (3 to 10 chars)
     *  * Sentence ID is followed by a comma and an arbitrary number of
     * printable ASCII characters (payload data)
     *  * Data is followed by '*' and a two-char hex checksum (may be omitted)
     *
     *
     *
     * Notice that format matching is not strict; although NMEA 0183 defines a
     * maximum length of 80 chars, the sentence length is not checked. This is
     * due to fact that it seems quite common that devices violate this rule,
     * some perhaps deliberately, some by mistake. Thus, assuming the formatting
     * is otherwise valid, it is not feasible to strictly validate length and
     * discard sentences that just exceed the 80 chars limit.
     *
     *
     * @param nmea String to inspect
     * @return true if recognized as sentence, otherwise false.
     */
    fun isSentence(nmea: String?): Boolean {
        if (nmea == null || "" == nmea) {
            return false
        }
        return if (Checksum.index(nmea) == nmea.length) {
            reNoChecksum.matches(nmea)
        } else reChecksum.matches(nmea)
    }

    /**
     * Tells if the specified String is a valid NMEA 0183 sentence. String is
     * considered as valid sentence if it passes the [.isSentence]
     * test and contains correct checksum. Sentences without checksum are
     * validated only by checking the general sentence characteristics.
     *
     * @param nmea String to validate
     * @return `true` if valid, otherwise `false`.
     */
    fun isValid(nmea: String): Boolean {
        var isValid = false
        if (isSentence(nmea)) {
            var i: Int = nmea.indexOf(Sentence.CHECKSUM_DELIMITER)
            isValid = if (i > 0) {
                val sum = nmea.substring(++i, nmea.length)
                sum == Checksum.calculate(nmea)
            } else {
                // no checksum
                true
            }
        }
        return isValid
    }
}