/*
 * AISMessage24Parser.java
 * Copyright (C) 2016 Henri Laurent
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

import net.sf.marineapi.ais.message.AISMessage24
import net.sf.marineapi.ais.util.ShipType
import net.sf.marineapi.ais.util.Sixbit

/**
 *
 * AIS Message 24 implementation: Ship Static Data - Class B
 *
 * Equivalent of a Type 5 message for ships using Class B equipment.
 * Also used to associate an MMSI with a name on either class A or class B equipment.
 *
 * According to the standard, both the A and B parts are supposed to be 168 bits.
 * A parts are often transmitted with only 160 bits, omitting the spare 7 bits at the end.
 *
 * May be in part A or part B format
 *
 *
 * <pre>
 * Part A
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                               	   6	(   1,   6)
 * 2	  repeatIndicator                         	   2	(   7,   8)
 * 3	  userID                                  	  30	(   9,  40)
 * 5	  name                                    	 120	( 41,  160)
 * 6     spare                                        8    ( 161, 168)
 * ---- +
 * sum 168
 *
 * Part B
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                               	   6	(   1,   6)
 * 2	  repeatIndicator                         	   2	(   7,   8)
 * 3	  userID                                  	  30	(   9,  40)
 * 4	  shiptype                                	   8	(  41,  48)
 * 5     vendorid                                    18    (  49,  66)
 * 6     model                                        4    ( 67,   70)
 * 7     serial                                      20    ( 71,   90)
 * 8     callsign                                    42    ( 90,  132)
 * 9	  dimension                               	  30	( 133, 162)
 * 15	  spare                                   	   1	( 163, 168)
 * ---- +
 * sum 168
</pre> *
 *
 * @author Henri Laurent
 */
internal class AISMessage24Parser(content: Sixbit) : AISMessageParser(content, 160, 168), AISMessage24 {
    override var partNumber: Int = content.getInt(FROM_A[PARTNUMBER], TO_A[PARTNUMBER])
    override var name: String? = null
    override var typeOfShipAndCargoType = 0
    override var vendorId: String? = null
    override var unitModelCode = 0
    override var serialNumber = 0
    override var callSign: String? = null
    override var bow = 0
    override var stern = 0
    override var port = 0
    override var starboard = 0

    /**
     * Constructor.
     *
     * @param content Six-bit message content.
     */
    init {
        partNumber = content.getInt(FROM_A[PARTNUMBER], TO_A[PARTNUMBER])
        if (partNumber == 0 && (content.length() == 160 || content.length() == 168)) {
            // Part A
            name = content.getString(FROM_A[NAME], TO_A[NAME])
        } else if (partNumber == 1 && content.length() == 168) {
            //Part B
            typeOfShipAndCargoType = content.getInt(FROM_B[TYPEOFSHIPANDCARGO], TO_B[TYPEOFSHIPANDCARGO])
            vendorId = content.getString(FROM_B[VENDORID], TO_B[VENDORID])
            unitModelCode = content.getInt(FROM_B[UNITMODELCODE], TO_B[UNITMODELCODE])
            serialNumber = content.getInt(FROM_B[SERIALNUMBER], TO_B[SERIALNUMBER])
            callSign = content.getString(FROM_B[CALLSIGN], TO_B[CALLSIGN])
            bow = content.getInt(FROM_B[BOW], TO_B[BOW])
            stern = content.getInt(FROM_B[STERN], TO_B[STERN])
            port = content.getInt(FROM_B[PORT], TO_B[PORT])
            starboard = content.getInt(FROM_B[STARBOARD], TO_B[STARBOARD])
        } else {
            throw IllegalArgumentException("Invalid part number or message length")
        }
    }

    override fun toString(): String {
        var result = "\tName:      $name"
        result = """$result
	Type:      ${ShipType.shipTypeToString(typeOfShipAndCargoType)}"""
        result = """$result
	Vendor id:      $vendorId"""
        result = """$result
	Unit Model Code:      $unitModelCode"""
        result = """$result
	Serial Number:      $serialNumber"""
        result = """$result
	Call sign: $callSign"""
        val dim = "Bow: $bow, Stern: $stern, Port: $port, Starboard: $starboard [m]"
        result = "$result\n\tDim:       $dim"
        return result
    }

    companion object {
        private const val PARTNUMBER = 0

        // Part A
        private const val NAME = 1

        // Part B
        private const val TYPEOFSHIPANDCARGO = 1
        private const val VENDORID = 2
        private const val UNITMODELCODE = 3
        private const val SERIALNUMBER = 4
        private const val CALLSIGN = 5
        private const val BOW = 6
        private const val STERN = 7
        private const val PORT = 8
        private const val STARBOARD = 9
        private val FROM_A = intArrayOf(38, 40, 160)
        private val TO_A = intArrayOf(40, 160, 168)
        private val FROM_B = intArrayOf(38, 40, 48, 66, 70, 90, 132, 141, 150, 156)
        private val TO_B = intArrayOf(40, 48, 66, 70, 90, 132, 141, 150, 156, 162)
    }
}