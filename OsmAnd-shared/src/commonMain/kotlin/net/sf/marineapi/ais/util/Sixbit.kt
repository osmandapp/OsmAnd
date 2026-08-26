/*
 * Sixbit.java
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
package net.sf.marineapi.ais.util

/**
 * AIS characters are encoded as 6 bit values concatenated into a bit array.
 * This class implements the higher level access to this bit array, storing
 * and retrieveing characters, integers, etc.
 *
 * @author Lázár József
 */
class Sixbit(payload: String?, fillBits: Int) {
    /**
     * Returns the full message String in sixbit encoded format.
     *
     * @return Sixbit encoded String.
     */
    val payload: String

    private val fBitVector: BitVector
    private val fFillBits: Int // Number of padding bits at end

    /**
     * Constructor.
     *
     * @param payload 6-bit encoded String
     * @param fillBits Fill bits to be added
     */
    init {
        require(!(payload == null || payload.isEmpty())) { "Message payload cannot be null or empty" }
        require(fillBits >= 0) { "Fill bits cannot be negative" }
        this.payload = payload
        require(isValidString(this.payload)) { "Invalid payload characters" }
        fBitVector = BitVector(payload.length * BITS_PER_CHAR)
        for (i in payload.indices) {
            val c = payload[i]
            val b = transportToBinary(c)
            convert(b, i * BITS_PER_CHAR, BITS_PER_CHAR)
        }
        fFillBits = fillBits
    }

    private fun convert(value: Int, from: Int, length: Int) {
        var value1 = value
        var length1 = length
        var index = from + BITS_PER_CHAR - 1
        while (value1.toLong() != 0L && length1 > 0) {
            if (value1 % 2L != 0L) fBitVector.set(index)
            index--
            value1 = value1 ushr 1
            length1--
        }
    }

    /**
     * Returns a [BitVector] for specified range.
     *
     * @param from Start index
     * @param to End index
     * @return BitVector for specified range.
     */
    operator fun get(from: Int, to: Int): BitVector {
        return fBitVector[from, to]
    }

    private fun isValidCharacter(ascii: Char): Boolean {
        return ascii.code in 0x30..0x77 && (ascii.code <= 0x57 || ascii.code >= 0x60)
    }

    private fun isValidString(bits: String): Boolean {
        var valid = true
        for (element in bits) {
            if (!isValidCharacter(element)) {
                valid = false
                break
            }
        }
        return valid
    }

    /**
     * Returns the payload length.
     *
     * @return Number of payload bits.
     */
    fun length(): Int {
        return payload.length * BITS_PER_CHAR - fFillBits
    }

    /**
     * Decode a transport character to a binary value.
     *
     * @param ascii character to decode
     * @return decoded value in 6-bit binary representation
     */
    private fun transportToBinary(ascii: Char): Int {
        require(isValidCharacter(ascii)) { "Invalid transport character: $ascii" }
        return if (ascii.code < 0x60) ascii.code - 0x30 else ascii.code - 0x38
    }

    /** Decode a binary value to a content character.
     *
     * @param value to be decoded
     * @return corresponding ASCII value (0x20-0x5F)
     *
     * This function returns the content character as encoded in binary value.
     * See table 44 (page 100) of Rec. ITU-R M.1371-4.
     *
     * This function is used to convert binary data to ASCII. This is
     * different from the 6-bit ASCII to binary conversion for VDM
     * messages; it is used for strings within the datastream itself.
     * eg. Ship Name, Callsign and Destination.
     */
    private fun binaryToContent(value: Int): Char {
        return if (value < 0x20) (value + 0x40).toChar() else value.toChar()
    }

    /**
     * Return bit as boolean from the bit vector.
     *
     * @param index start index of bit
     * @return Boolean value for specified index
     */
    fun getBoolean(index: Int): Boolean {
        return fBitVector.getBoolean(index)
    }

    /**
     * Returns the requested bits interpreted as an integer (MSB first) from the message.
     *
     * @param from begin index (inclusive)
     * @param to end index (inclusive)
     * @return unsigned int value
     */
    fun getInt(from: Int, to: Int): Int {
        return fBitVector.getUInt(from, to)
    }

    /**
     * Get 8-bit integer value.
     *
     * @param from Start index
     * @param to End index
     * @return Integer value
     */
    fun getAs8BitInt(from: Int, to: Int): Int {
        return fBitVector.getAs8BitInt(from, to)
    }

    /**
     * Get 17-bit integer value.
     *
     * @param from Start index
     * @param to End index
     * @return Integer value
     */
    fun getAs17BitInt(from: Int, to: Int): Int {
        return fBitVector.getAs17BitInt(from, to)
    }

    /**
     * Get 18-bit integer value.
     *
     * @param from Start index
     * @param to End index
     * @return Integer value
     */
    fun getAs18BitInt(from: Int, to: Int): Int {
        return fBitVector.getAs18BitInt(from, to)
    }

    /**
     * Get 27-bit integer value.
     *
     * @param from Start index
     * @param to End index
     * @return Integer value
     */
    fun getAs27BitInt(from: Int, to: Int): Int {
        return fBitVector.getAs27BitInt(from, to)
    }

    /**
     * Get 28-bit integer value.
     *
     * @param from Start index
     * @param to End index
     * @return Integer value
     */
    fun getAs28BitInt(from: Int, to: Int): Int {
        return fBitVector.getAs28BitInt(from, to)
    }

    /**
     * Return string from bit vector
     *
     * @param fromIndex begin index (inclusive)
     * @param toIndex end index (inclusive)
     * @return String value
     */
    fun getString(fromIndex: Int, toIndex: Int): String {
        val sb = StringBuilder()
        var i = fromIndex
        while (i < toIndex) {
            val value = getInt(i, i + BITS_PER_CHAR)
            sb.append(binaryToContent(value))
            i += BITS_PER_CHAR
        }
        return stripAtSigns(sb.toString())
    }

    /**
     * Strips the @ characters from specified String.
     */
    private fun stripAtSigns(orig: String): String {
        var end = orig.length - 1
        for (i in orig.length - 1 downTo 0) {
            if (orig[i] != '@') {
                end = i
                break
            }
        }
        return orig.substring(0, end + 1)
    }

    companion object {
        /** Number of bits per character  */
        const val BITS_PER_CHAR = 6
    }
}