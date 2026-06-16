/*
 * AISMessageParser.java
 * Copyright (C) 2015 Lázár József
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
package net.sf.marineapi.ais.parser

import net.sf.marineapi.ais.message.AISMessage
import net.sf.marineapi.ais.util.Sixbit
import net.sf.marineapi.ais.util.Violation
import net.sf.marineapi.nmea.sentence.AISSentence

/**
 * Base class for all AIS messages.
 *
 * @author Lázár József, Kimmo Tuukkanen
 */
open class AISMessageParser : AISMessage {
    private var decoder: Sixbit? = null
    private var message = ""
    private var fillBits = 0
    private var lastFragmentNr = 0
    private val fViolations: MutableList<Violation> = ArrayList()

    /**
     * Default constructor. Message content musts be appended before using
     * the parser.
     *
     * @see .append
     */
    constructor()

    /**
     * Construct a parser with given AIS sentences. The result will parser for
     * common AIS fields (type, repeat and MMSI), from which the actual message
     * type can be determined for further parsing.
     *
     * @param sentences Single AIS sentence or a sequence of sentences.
     */
    constructor(vararg sentences: AISSentence) {
        var index = 1
        for (s in sentences) {
            require(!(s.isFragmented() && s.getFragmentNumber() != index++)) { "Incorrect order of AIS sentences" }
            this.append(s.getPayload(), s.getFragmentNumber(), s.getFillBits())
        }
        decoder = Sixbit(message, fillBits)
    }

    /**
     * Constructor with six-bit content decoder.
     *
     * @param sb A non-empty six-bit decoder.
     */
    constructor(sb: Sixbit) {
        require(sb.length() > 0) { "Sixbit decoder is empty!" }
        decoder = sb
    }

    /**
     * Constructor with six-bit content decoder.
     *
     * @param sb A non-empty six-bit decoder.
     * @param len Expected message length (bits)
     */
    protected constructor(sb: Sixbit, len: Int) : this(sb) {
        require(sb.length() == len) {
            "Wrong message length: ${sb.length()} (expected $len)"
        }
    }

    /**
     * Constructor with six-bit content decoder.
     *
     * @param sb A non-empty six-bit decoder.
     * @param min Expected minimum length of message (bits)
     * @param max Expected maximum length of message (bits)
     */
    protected constructor(sb: Sixbit, min: Int, max: Int) : this(sb) {
        require(!(sb.length() < min || sb.length() > max)) {
            "Wrong message length: ${sb.length()} (expected $min - $max)"
        }
    }

    /**
     * Add a new rule violation to this message
     * @param v Violation to add
     */
    protected fun addViolation(v: Violation) {
        fViolations.add(v)
    }

    /**
     * Returns the number of violations.
     *
     * @return Number of violations.
     */
    val nrOfViolations: Int
        get() = fViolations.size

    /**
     * Returns list of discoverd data violations.
     *
     * @return Number of violations.
     */
    val violations: List<Violation>
        get() = fViolations
    override val messageType: Int
        get() = sixbit.getInt(FROM[MESSAGE_TYPE], TO[MESSAGE_TYPE])
    override val repeatIndicator: Int
        get() = sixbit.getInt(FROM[REPEAT_INDICATOR], TO[REPEAT_INDICATOR])
    override val mMSI: Int
        get() = sixbit.getInt(FROM[MMSI], TO[MMSI])

    /**
     * Returns the six-bit decoder of message.
     *
     * @return Sixbit decoder.
     * @throws IllegalStateException When message payload has not been appended
     * or Sixbit decoder has not been provided as constructor parameter.
     */
    val sixbit: Sixbit
        get() {
            check(!(decoder == null && message.isEmpty())) { "Message is empty!" }
            return if (decoder == null) Sixbit(message, fillBits) else decoder!!
        }

    /**
     * Append a paylod fragment to combine messages devivered over multiple
     * sentences.
     *
     * @param fragment Data fragment in sixbit encoded format
     * @param fragmentIndex Fragment number within the fragments sequence (1-based)
     * @param fillBits Number of additional fill-bits
     */
    fun append(fragment: String?, fragmentIndex: Int, fillBits: Int) {
        require(!(fragment == null || fragment.isEmpty())) { "Message fragment cannot be null or empty" }
        require(!(fragmentIndex < 1 || fragmentIndex != lastFragmentNr + 1)) { "Invalid fragment index or sequence order" }
        require(fillBits >= 0) { "Fill bits cannot be negative" }
        lastFragmentNr = fragmentIndex
        message += fragment
        this.fillBits = fillBits // we always use the last
    }

    companion object {
        // Common AIS message part
        private const val MESSAGE_TYPE = 0
        private const val REPEAT_INDICATOR = 1
        private const val MMSI = 2
        private val FROM = intArrayOf(0, 6, 8)
        private val TO = intArrayOf(6, 8, 38)
    }
}