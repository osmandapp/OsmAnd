/*
 * AISMessage04Parser.java
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

import net.sf.marineapi.ais.message.AISMessage04
import net.sf.marineapi.ais.util.Sixbit

/**
 * AIS Message 4 implementation: Base Station Report.
 *
 * <pre>
 * Field Name                                           Bits    (from, to )
 * ------------------------------------------------------------------------
 * 1	 messageID                        		       	   6	(   1,   6)
 * 2	 repeatIndicator                         		   2	(   7,   8)
 * 3	 userID                                  		  30	(   9,  38)
 * 4	 utcYear                        		          14	(  39,  52)
 * 5	 utcMonth                            	    	   4	(  53,  56)
 * 6	 utcDay                                  		   5	(  57,  61)
 * 7	 utcHour                                 		   5	(  62,  66)
 * 8	 utcMinute                              		   6	(  67,  72)
 * 9	 utcSecond                          	    	   6	(  73,  78)
 * 10	 positionAccuracy                    	    	   1	(  79,  79)
 * 11	 longitude                               		  28	(  80, 107)
 * 12	 latitude                              		  	  27	( 108, 134)
 * 13	 typeOfElectronicPositionFixingDevice    		   4	( 135, 138)
 * 14	 transmissionControlForLongRangeBroadcastMessage   1	( 139, 139)
 * 15	 spare                                   	 	   9	( 140, 148)
 * 16	 raimFlag                              		  	   1	( 149, 149)
 * 17	 communicationState                      		  19	( 150, 168)
 * ---- +
 * sum 168
</pre> *
 *
 * @author Lázár József
 */
internal class AISMessage04Parser
/**
 * Constructor.
 *
 * @param content Six-bit message content to parse.
 */
    (content: Sixbit) : AISUTCParser(content), AISMessage04