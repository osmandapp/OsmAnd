/* 
 * AISSentence.java
 * Copyright (C) 2015 Lázár József
 * 
 * This file is part of Java Marine API.
 * <http://ktuukkan.github.io/marine-api/>
 * 
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.nmea.sentence

/**
 *
 *
 * Base interface for all AIS sentences (Automatic Identification System).
 * Notice that &quot;AIS&quot; does not refer to NMEA sentence type, but another
 * system/standard that transmits it's messages using NMEA 0183.
 *
 *
 *
 * AIS sentences are parsed in two phases and they all share the same NMEA
 * sentence layout, so there is no dedicated interfaces for each AIS sentence
 * type (VDM, VDO etc).
 *
 *
 * Example:<br></br>
 * `!AIVDM,1,1,,B,177KQJ5000G?tO`K>RA1wUbN0TKH,0*5C`
 *
 * @author Lázár József, Kimmo Tuukkanen
 */
interface AISSentence : Sentence {

    /**
     * Number of fragments in the currently accumulating message.
     *
     * @return number of fragments.
     */
    fun getNumberOfFragments(): Int

    /**
     * Returns the fragment number of this sentence (1-based).
     *
     * @return fragment index
     */
    fun getFragmentNumber(): Int

    /**
     * Returns the sequential message ID for multi-sentence messages.
     *
     * @return sequential message ID
     */
    fun getMessageId(): String?

    /**
     * Returns the radio channel information of the messsage.
     *
     * @return radio channel id
     */
    fun getRadioChannel(): String?

    /**
     * Returns the raw 6-bit decoded message.
     *
     * @return message body
     */
    fun getPayload(): String?

    /**
     * Returns the number of fill bits required to pad the data payload to a 6
     * bit boundary, ranging from 0 to 5.
     *
     * Equivalently, subtracting 5 from this tells how many least significant
     * bits of the last 6-bit nibble in the data payload should be ignored.
     *
     * @return number of fill bits
     */
    fun getFillBits(): Int

    /**
     * Tells if the AIS message is being delivered over multiple sentences.
     *
     * @return true if this sentence is part of a sequence
     */
    fun isFragmented(): Boolean

    /**
     * Tells if this is the first fragment in message sequence.
     *
     * @return true if first fragment in sequence
     */
    fun isFirstFragment(): Boolean

    /**
     * Tells if this is the last fragment in message sequence.
     *
     * @return true if last part of a sequence
     */
    fun isLastFragment(): Boolean

    /**
     *
     *
     * Tells if given sentence is part of message sequence.
     *
     *
     *
     * Sentences are considered to belong in same sequence when the given
     * sentence meets the following conditions:
     *
     *
     *  * Same number of fragments, higher fragment #, same channel and same
     * message id
     *  * Same number of fragments, next fragment #, and either same channel or
     * same message id
     *
     *
     * @param sentence AISSentence to compare with.
     * @return true if this and given sentence belong in same sequence
     */
    fun isPartOfMessage(sentence: AISSentence?): Boolean}