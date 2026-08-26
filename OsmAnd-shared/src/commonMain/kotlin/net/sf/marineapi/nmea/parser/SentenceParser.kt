/*
 * SentenceParser.java
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
package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.*

import kotlin.math.pow
import kotlin.math.round

import kotlin.collections.ArrayList

/**
 *
 *
 * Base class for all NMEA 0183 sentence parsers. Contains generic methods such
 * as data field setters and getters, data formatting, validation etc.
 *
 *
 * NMEA 0183 data is transmitted in form of ASCII Strings that are called
 * *sentences*. Each sentence starts with a '$', a two letter
 * *talker ID*, a three letter *sentence ID*, followed by a number
 * of comma separated *data fields*, *optional checksum* and a
 * carriage return/line feed terminator (`CR/LF`). Sentence may
 * contain up to 82 characters including the `CR/LF`. If data for
 * certain field is not available, the field value is simply omitted, but the
 * commas that would delimit it are still sent, with no space between them.
 *
 *
 * Sentence structure:<br></br>
 * `$<talker-id><sentence-id>,<field #0>,<field #1>,...,<field #n>*<checksum><cr/lf>
` *
 *
 *
 * For more details, see [NMEA Revealed](http://catb.org/gpsd/NMEA.html) by Eric S. Raymond.
 *
 *
 * This class can also be used to implement and integrate parsers not provided
 * by in the library. See [SentenceFactory] for more instructions.
 *
 * @author Kimmo Tuukkanen
 */
open class SentenceParser : Sentence {
    // The first character which will be '$' most of the times but could be '!'.
    private var beginChar: Char

    // The first two characters after '$'.
    private var talkerId: TalkerId

    // The next three characters after talker id.
    private val sentenceId: String

    // actual data fields (sentence id and checksum omitted)
    private var fields: MutableList<String?>? = ArrayList()

    /**
     * Creates a new instance of SentenceParser. Validates the input String and
     * resolves talker id and sentence type.
     *
     * @param nmea A valid NMEA 0183 sentence
     * @throws IllegalArgumentException If the specified sentence is invalid or
     * if sentence type is not supported.
     */
    constructor(nmea: String) {
        if (!SentenceValidator.isValid(nmea)) {
            val msg = "Invalid data [$nmea]"
            throw IllegalArgumentException(msg)
        }
        beginChar = nmea[0]
        talkerId = TalkerId.parse(nmea)
        sentenceId = SentenceId.parseStr(nmea)
        val begin = nmea.indexOf(Sentence.FIELD_DELIMITER) + 1
        val end = Checksum.index(nmea)
        val csv = nmea.substring(begin, end)
        val values = csv.split(Sentence.FIELD_DELIMITER.toString().toRegex()).toTypedArray()
        fields!!.addAll(listOf(*values))
    }

    /**
     * Creates a new empty sentence with specified begin char, talker id,
     * sentence id and number of fields.
     *
     * @param begin Begin char, $ or !
     * @param tid TalkerId to set
     * @param sid SentenceId to set
     * @param size Number of sentence data fields
     */
    protected constructor(begin: Char, tid: TalkerId?, sid: SentenceId, size: Int) : this(
        begin,
        tid,
        sid.toString(),
        size
    )

    /**
     * Creates a new empty sentence with specified begin char, talker id,
     * sentence id and number of fields.
     *
     * @param begin The begin character, e.g. '$' or '!'
     * @param talker TalkerId to set
     * @param type Sentence id as String, e.g. "GGA or "GLL".
     * @param size Number of sentence data fields
     */
    protected constructor(begin: Char, talker: TalkerId?, type: String?, size: Int) {
        require(size >= 0) { "Size cannot be negative." }
        requireNotNull(talker) { "Talker ID must be specified" }
        require((type != null) && ("" != type)) { "Sentence ID must be specified" }
        beginChar = begin
        talkerId = talker
        sentenceId = type
        val values = Array(size) { "" }
        fields!!.addAll(values.toList())
    }

    /**
     * Creates a new instance of SentenceParser. Parser may be constructed only
     * if parameter `nmea` contains a valid NMEA 0183 sentence of the
     * specified `type`.
     *
     *
     * For example, GGA sentence parser should specify "GGA" as the type.
     *
     * @param nmea NMEA 0183 sentence String
     * @param type Expected type of the sentence in `nmea` parameter
     * @throws IllegalArgumentException If the specified sentence is not a valid
     * or is not of expected type.
     */
    constructor(nmea: String, type: String?) : this(nmea) {
        require((type != null) && ("" != type)) { "Sentence type must be specified." }
        val sid = getSentenceId()
        if (sid != type) {
            val msg = "Sentence id mismatch; expected [$type], found [$sid]."
            throw IllegalArgumentException(msg)
        }
    }

    /**
     * Creates a new empty sentence with specified talker and sentence IDs.
     *
     * @param talker Talker type Id, e.g. "GP" or "LC".
     * @param type Sentence type Id, e.g. "GGA or "GLL".
     * @param size Number of data fields
     */
    constructor(talker: TalkerId?, type: String?, size: Int) : this(Sentence.BEGIN_CHAR, talker, type, size)

    /**
     * Creates a new instance of SentenceParser with specified sentence data.
     * Type of the sentence is checked against the specified expected sentence
     * type id.
     *
     * @param nmea Sentence String
     * @param type Sentence type enum
     */
    internal constructor(nmea: String, type: SentenceId) : this(nmea, type.toString())

    /**
     * Creates a new instance of SentenceParser without any data.
     *
     * @param tid Talker id to set in sentence
     * @param sid Sentence id to set in sentence
     * @param size Number of data fields following the sentence id field
     */
    internal constructor(tid: TalkerId?, sid: SentenceId, size: Int) : this(tid, sid.toString(), size)

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is SentenceParser) {
            return other.toString() == toString()
        }
        return false
    }

    override fun getBeginChar(): Char {
        return beginChar
    }

    override fun getFieldCount(): Int {
        return if (fields == null) {
            0
        } else fields!!.size
    }

    override fun getSentenceId(): String {
        return sentenceId
    }

    override fun getTalkerId(): TalkerId {
        return talkerId
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun isAISSentence(): Boolean {
        val types = arrayOf("VDO", "VDM")
        return listOf(*types).contains(getSentenceId())
    }

    override fun isProprietary(): Boolean {
        return TalkerId.P == getTalkerId()
    }

    override fun isValid(): Boolean {
        return SentenceValidator.isValid(toString())
    }

    override fun reset() {
        for (i in fields!!.indices) {
            fields!![i] = ""
        }
    }

    override fun setBeginChar(ch: Char) {
        if (ch != Sentence.BEGIN_CHAR && ch != Sentence.ALTERNATIVE_BEGIN_CHAR) {
            val msg = "Invalid begin char; expected '$' or '!'"
            throw IllegalArgumentException(msg)
        }
        beginChar = ch
    }

    override fun setTalkerId(id: TalkerId) {
        talkerId = id
    }

    override fun toSentence(): String {
        val s = toString()
        if (!SentenceValidator.isValid(s)) {
            val msg = "Validation failed [${toString()}]"
            throw IllegalStateException(msg)
        }
        return s
    }

    override fun toSentence(maxLength: Int): String {
        val s = toSentence()
        if (s.length > maxLength) {
            val msg = "Sentence max length exceeded $maxLength"
            throw IllegalStateException(msg)
        }
        return s
    }

    override fun toString(): String {
        val sb = StringBuilder(Sentence.MAX_LENGTH)
        sb.append(talkerId.toString())
        sb.append(sentenceId)
        for (field in fields!!) {
            sb.append(Sentence.FIELD_DELIMITER)
            sb.append(field ?: "")
        }
        val checksum = Checksum.xor(sb.toString())
        sb.append(Sentence.CHECKSUM_DELIMITER)
        sb.append(checksum)
        sb.insert(0, beginChar)
        return sb.toString()
    }

    /**
     * Parse a single character from the specified sentence field.
     *
     * @param index Data field index in sentence
     * @return Character contained in the field
     * @throws ParseException If field contains more
     * than one character
     */
    fun getCharValue(index: Int): Char {
        val `val` = getStringValue(index)
        if (`val`.length > 1) {
            val msg = "Expected char, found String [`val`]"
            throw ParseException(msg)
        }
        return `val`[0]
    }

    /**
     * Parse double value from the specified sentence field.
     *
     * @param index Data field index in sentence
     * @return Field as parsed by [Double.parseDouble]
     */
    fun getDoubleValue(index: Int): Double {
        val value: Double = try {
            getStringValue(index).toDouble()
        } catch (ex: NumberFormatException) {
            throw ParseException("Field does not contain double value", ex)
        }
        return value
    }

    /**
     * Parse integer value from the specified sentence field.
     *
     * @param index Field index in sentence
     * @return Field parsed by [Integer.parseInt]
     */
    protected fun getIntValue(index: Int): Int {
        val value: Int = try {
            getStringValue(index).toInt()
        } catch (ex: NumberFormatException) {
            throw ParseException("Field does not contain integer value", ex)
        }
        return value
    }

    /**
     * Get contents of a data field as a String. Field indexing is zero-based.
     * The address field (e.g. `$GPGGA`) and checksum at the end are
     * not considered as a data fields and cannot therefore be fetched with this
     * method.
     *
     *
     * Field indexing, let i = 1: <br></br>
     * `$&lt;id&gt;,&lt;i&gt;,&lt;i+1&gt;,&lt;i+2&gt;,...,&lt;i+n&gt;*&lt;checksum&gt;`
     *
     * @param index Field index
     * @return Field value as String
     * @throws DataNotAvailableException If the field is
     * empty
     */
    fun getStringValue(index: Int): String {
        val value = fields!![index]
        if (value == null || "" == value) {
            throw DataNotAvailableException("Data not available")
        }
        return value
    }

    /**
     * Tells is if the field specified by the given index contains a value.
     *
     * @param index Field index
     * @return True if field contains value, otherwise false.
     */
    protected fun hasValue(index: Int): Boolean {
        return fields!!.size > index && fields!![index] != null && fields!![index]!!.isNotEmpty()
    }

    /**
     * Set a character in specified field.
     *
     * @param index Field index
     * @param value Value to set
     */
    protected fun setCharValue(index: Int, value: Char) {
        setStringValue(index, value.toString())
    }

    /**
     * Set degrees value, e.g. course or heading.
     *
     * @param index Field index where to insert value
     * @param deg The degrees value to set
     * @throws IllegalArgumentException If degrees value out of range [0..360]
     */
    protected fun setDegreesValue(index: Int, deg: Double) {
        require(!(deg < 0 || deg > 360)) { "Value out of bounds [0..360]" }
        setDoubleValue(index, deg, 3, 1)
    }

    /**
     * Set double value in specified field. Value is set "as-is" without any
     * formatting or rounding.
     *
     * @param index Field index
     * @param value Value to set
     * @see .setDoubleValue
     */
    fun setDoubleValue(index: Int, value: Double) {
        setStringValue(index, value.toString())
    }

    /**
     * Set double value in specified field, with given number of digits before
     * and after the decimal separator ('.'). When necessary, the value is
     * padded with leading zeros and/or rounded to meet the requested number of
     * digits.
     *
     * @param index Field index
     * @param value Value to set
     * @param leading Number of digits before decimal separator
     * @param decimals Maximum number of digits after decimal separator
     * @see .setDoubleValue
     */
    fun setDoubleValue(
        index: Int, value: Double, leading: Int,
        decimals: Int
    ) {
        val factor = 10.0.pow(decimals)
        val rounded = round(value * factor) / factor
        val parts = rounded.toString().split('.')
        val intPart = parts[0].padStart(leading, '0')
        val decPart = if (parts.size > 1) parts[1].padEnd(decimals, '0') else "".padEnd(decimals, '0')
        setStringValue(index, if (decimals > 0) "$intPart.$decPart" else intPart)
    }

    /**
     * Sets the number of sentence data fields. Increases or decreases the
     * fields array, values in fields not affected by the change remain
     * unchanged. Does nothing if specified new size is equal to count returned
     * by [.getFieldCount].
     *
     * @param size Number of data fields, must be greater than zero.
     */
    fun setFieldCount(size: Int) {
        require(size >= 1) { "Number of fields must be greater than zero." }
        if (size < fields!!.size) {
            fields = fields!!.subList(0, size)
        } else if (size > fields!!.size) {
            for (i in fields!!.size until size) {
                fields!!.add("")
            }
        }
    }

    /**
     * Set integer value in specified field.
     *
     * @param index Field index
     * @param value Value to set
     */
    protected fun setIntValue(index: Int, value: Int) {
        setStringValue(index, value.toString())
    }

    /**
     * Set integer value in specified field, with specified minimum number of
     * digits. Leading zeros are added to value if when necessary.
     *
     * @param index Field index
     * @param value Value to set
     * @param leading Number of digits to use.
     */
    fun setIntValue(index: Int, value: Int, leading: Int) {
        val str = value.toString()
        setStringValue(index, if (str.length < leading) str.padStart(leading, '0') else str)
    }

    /**
     * Set String value in specified data field.
     *
     * @param index Field index
     * @param value String to set, `null` converts to empty String.
     */
    fun setStringValue(index: Int, value: String?) {
        fields!![index] = value ?: ""
    }

    /**
     * Replace multiple fields with given String array, starting at the
     * specified index. If parameter `first` is zero, all sentence
     * fields are replaced.
     *
     *
     * If the length of `newFields` does not fit in the sentence
     * field count or it contains less values, fields are removed or added
     * accordingly. As the result, total number of fields may increase or
     * decrease. Thus, if the sentence field count must not change, you may need
     * to add empty Strings to `newFields` in order to preserve the
     * original number of fields. Also, all existing values after
     * `first` are lost.
     *
     * @param first Index of first field to set
     * @param newFields Array of Strings to set
     */
    fun setStringValues(first: Int, newFields: Array<String?>) {
        val temp: MutableList<String?> = ArrayList()
        temp.addAll(fields!!.subList(0, first))
        for (field in newFields) {
            temp.add(field ?: "")
        }
        fields!!.clear()
        fields = temp
    }

    /**
     * Returns all field values, starting from the specified index towards the
     * end of sentence.
     *
     * @param first Index of first field to get.
     * @return Array of String values
     */
    protected fun getStringValues(first: Int): Array<String> {
        return fields!!.subList(first, fields!!.size).toTypedArray() as Array<String>
    }
}