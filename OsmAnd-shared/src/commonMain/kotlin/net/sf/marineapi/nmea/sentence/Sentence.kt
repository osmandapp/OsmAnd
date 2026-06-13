/*
 * Sentence.java
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

interface Sentence {
    /**
     * Get the sentence begin character. Although most of the sentences start
     * with '$', some of them use '!' as begin character.
     *
     * @return Sentence begin char, e.g. "$" or "!".
     */
    fun getBeginChar(): Char

    /**
     * Returns the current number of data fields in sentence, excluding ID field
     * and checksum.
     *
     * @return Data field count
     */
    fun getFieldCount(): Int

    /**
     * Get the sentence ID that specifies the sentence type and data it holds.
     * ID is the last three characters in address field. For example, in case of
     * `$GPGGA` the method returns [SentenceId.GGA].
     *
     * @return Sentence id String, e.g. "GLL" or "GGA".
     * @see SentenceId
     */
    fun getSentenceId(): String?

    /**
     * Gets the talker ID of the sentence. Talker ID is the next two characters
     * after `$` in sentence address field. For example, in case of
     * `$GPGGA`, the method returns [TalkerId.GP].
     *
     * @return Talker id enum.
     */
    fun getTalkerId(): TalkerId?

    /**
     * Tells if this is an AIS sentence.
     *
     * @return True if AIS sentence, otherwise false.
     */
    fun isAISSentence(): Boolean

    /**
     * Tells if the sentence is of proprietary format.
     *
     * @return True if proprietary, otherwise false.
     */
    fun isProprietary(): Boolean

    /**
     * Tells if the sentence formatting matches NMEA 0183 format.
     *
     * @return True if validly formatted, otherwise false.
     */
    fun isValid(): Boolean

    /**
     * Resets the sentence contents, i.e. removes all existing values from data
     * fields. After resetting, address field remains as is and checksum is
     * calculated according to empty data fields.
     */
    fun reset()

    /**
     * Set the sentence begin character. Although most of the sentences start
     * with '$', some of them use '!' as begin character.
     *
     * @param ch Sentence begin char to set ('$' or '!')
     * @see Sentence.BEGIN_CHAR
     *
     * @see Sentence.ALTERNATIVE_BEGIN_CHAR
     */
    fun setBeginChar(ch: Char)

    /**
     * Set the talker ID of the sentence. Typically, the ID might be changed if
     * the sentence is to be sent from a computer to an NMEA device.
     *
     * @param id TalkerId to set
     */
    fun setTalkerId(id: TalkerId)

    /**
     * Formats and validates the String representation of sentence. Throws an
     * exception if result is not considered a valid sentence. As validation is
     * done by [net.sf.marineapi.nmea.sentence.SentenceValidator], notice
     * that resulting sentence length is not checked. To also validate the
     * length, use [.toSentence].
     *
     * @see .toString
     * @return Sentence as String, equal to `toString()`.
     * @throws IllegalStateException If formatting results in invalid sentence.
     */
    fun toSentence(): String?

    /**
     * Formats and validates the sentence like [.toSentence], but checks
     * also that resulting String does not exceed specified length.
     *
     * @param maxLength Maximum String length
     * @return Sentence as String, equal to `toString()`.
     * @throws IllegalStateException If formatting results in invalid sentence
     * or specified maximum length is exceeded.
     * @see .toSentence
     * @see .MAX_LENGTH
     */
    fun toSentence(maxLength: Int): String?

    /**
     * Returns the String representation of the sentence, without line
     * terminator `CR/LR`. Checksum is calculated and appended at the
     * end of the sentence, but no validation is done. Use [.toSentence]
     * to also validate the result.
     *
     * @return String representation of sentence
     */
    override fun toString(): String

    companion object {
        /**
         * Alternative sentence begin character used in VDO and VDM sentences.
         */
        const val ALTERNATIVE_BEGIN_CHAR = '!'

        /**
         * Sentence begin character
         */
        const val BEGIN_CHAR = '$'

        /**
         * Checksum field delimiter char
         */
        const val CHECKSUM_DELIMITER = '*'

        /**
         * Sentence data fields delimiter char
         */
        const val FIELD_DELIMITER = ','

        /**
         * Maximum length of NMEA 0183 sentences, including `BEGIN_CHAR`
         * and `TERMINATOR`.
         */
        const val MAX_LENGTH = 82

        /**
         * Sentence terminator `&lt;CR&gt;&lt;LF&gt;`.
         */
        const val TERMINATOR = "\r\n"
    }
}