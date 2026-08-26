/*
 * AISMessage01Parser.java
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

import net.sf.marineapi.ais.message.AISMessage01
import net.sf.marineapi.ais.util.Sixbit

/**
 * AIS Message 1 implementation: Position report.
 *
 * <pre>
 * Field  Name                                      Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	  messageID                               	   6	(   1,   6)
 * 2	  repeatIndicator                         	   2	(   7,   8)
 * 3	  userID                                  	  30	(   9,  38)
 * 4	  navigationalStatus                      	   4	(  39,  42)
 * 5	  rateOfTurn                              	   8	(  43,  50)
 * 6	  speedOverGround                         	  10	(  51,  60)
 * 7	  positionAccuracy                        	   1	(  61,  61)
 * 8	  longitude                               	  28	(  62,  89)
 * 9	  latitude                                	  27	(  90, 116)
 * 10	  courseOverGround                        	  12	( 117, 128)
 * 11	  trueHeading                             	   9	( 129, 137)
 * 12	  timeStamp                               	   6	( 138, 143)
 * 13	  specialManoeuvre                        	   2	( 144, 145)
 * 14	  spare                                   	   3	( 146, 148)
 * 15	  raimFlag                                	   1	( 149, 149)
 * 16	  communicationState                      	  19	( 150, 168)
 * ---- +
 * sum 168
</pre> *
 *
 * @author Lázár József
 */
internal class AISMessage01Parser
/**
 * Constructor.
 *
 * @param content Six-bit message content to parse.
 */
    (content: Sixbit) : AISPositionReportParser(content), AISMessage01