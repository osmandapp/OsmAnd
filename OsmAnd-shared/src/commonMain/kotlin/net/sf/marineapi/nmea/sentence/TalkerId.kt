/* 
 * TalkerId.java
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

import kotlin.IllegalArgumentException
import kotlin.String
import kotlin.require

/**
 * The enumeration of Talker IDs, i.e. the first two characters of sentence
 * address field. For example, `GP` in `$GPGGA`. Proprietary
 * sentences are identified by single character [.P].
 *
 *
 * The IDs marked as *obsolete* are deprecated and not recommended for
 * use in new implementations. However, they are not annotated with `&commat;Deprecated` to avoid unnecessary compiler warnings when working with
 * legacy systems/data.
 *
 *
 * Notice that this enum also contains some non-standard IDs to enable parsing
 * certain proprietary sentences that do not use [.P] identifier.
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.sentence.SentenceId
 */
enum class TalkerId {
    /** AIS Mobile Class A or B AIS Station  */
    AI,

    /** AIS Independent AIS Base Station (NMEA 4.0)  */
    AB,

    /** AIS Dependent AIS Base Station (NMEA 4.0)  */
    AD,

    /** AIS Aids to Navigation Station (NMEA 4.0)  */
    AN,

    /** AIS Receiving Station (NMEA 4.0)  */
    AR,

    /** AIS Limited Base Station (NMEA 4.0)  */
    AS,

    /** AIS Transmitting Station (NMEA 4.0)  */
    AT,

    /** AIS Simplex Repeater Station (NMEA 4.0)  */
    AX,

    /** AIS Base station (obsolete since NMEA 4.0)  */
    BS,

    /** Heading Track Controller/Autopilot: General  */
    AG,

    /** Heading Track Controller/Autopilot: Magnetic  */
    AP,

    /** BeiDou satellite navigation system (Chinese)  */
    BD,

    /** Bilge Systems  */
    BI,

    /** Bridge Navigational Watch Alarm System  */
    BN,

    /** Central Alarm Management  */
    CA,

    /** Computer - Programmed Calculator (obsolete)  */
    CC,

    /** Communications - Digital Selective Calling (DSC)  */
    CD,

    /** Computer - Memory Data (obsolete)  */
    CM,

    /** Channel Pilot (Navicom Dynamics proprietary)  */
    CP,

    /** Data Receiver  */
    CR,

    /** Communications - Satellite  */
    CS,

    /** Communications - Radio-Telephone (MF/HF)  */
    CT,

    /** Communications - Radio-Telephone (VHF)  */
    CV,

    /** Communications - Scanning Receiver  */
    CX,

    /** DECCA Navigation (obsolete)  */
    DE,

    /** Direction Finder  */
    DF,

    /** Velocity Sensor, Speed Log, Water, Magnetic  */
    DM,

    /** Dynamic Position  */
    DP,

    /** Duplex Repeater Station  */
    DU,

    /** Electronic Chart System (ECS)  */
    EC,

    /** Electronic Chart Display &amp; Information System (ECDIS)  */
    EI,

    /** Emergency Position Indicating Beacon (EPIRB)  */
    EP,

    /** Engine Room Monitoring Systems  */
    ER,

    /** Fire Door Controller/Monitoring Point  */
    FD,

    /** Fire Extinguisher System  */
    FE,

    /** Fire Detection Point  */
    FR,

    /** Fire Sprinkler System  */
    FS,

    /** Galileo satellite navigation system (European)  */
    GA,

    /** BeiDou satellite navigation system (Chinese)  */
    GB,

    /** Gas Finder (Boreal)  */
    GF,

    /** Indian Regional Navigation Satellite System (IRNSS)  */
    GI,

    /** GLONASS Receiver  */
    GL,

    /** Global Navigation Satellite System (GNSS)  */
    GN,

    /** Global Positioning System (GPS)  */
    GP,

    /** Quasi Zenith Satellite System (QXSS, Japanese)  */
    GQ,

    /** Heading Sensors: Compass, Magnetic  */
    HC,

    /** Hull Door Controller/Monitoring Panel  */
    HD,

    /** Heading Sensors: Gyro, North Seeking  */
    HE,

    /** Heading Sensors: Fluxgate  */
    HF,

    /** Heading Sensors: Gyro, Non-North Seeking  */
    HN,

    /** Hull Stress Monitoring  */
    HS,

    /** Integrated Instrumentation  */
    II,

    /** Integrated Navigation  */
    IN,

    /** Automation: Alarm and monitoring system (reserved for future use)  */
    JA,

    /** Automation: Reefer Monitoring System (reserved for future use)  */
    JB,

    /** Automation: Power Management System (reserved for future use)  */
    JC,

    /** Automation: Propulsion Control System (reserved for future use)  */
    JD,

    /** Automation: Engine Control Console (reserved for future use)  */
    JE,

    /** Automation: Propulsion Boiler (reserved for future use)  */
    JF,

    /** Automation: Auxiliary Boiler (reserved for future use)  */
    JG,

    /** Automation: Electronic Governor System (reserved for future use)  */
    JH,

    /** Loran A (obsolete)  */
    LA,

    /** Loran C (obsolete)  */
    LC,

    /** Microwave Positioning System (obsolete)  */
    MP,

    /** Multiplexer  */
    MX,

    /** Navigation Light Controller  */
    NL,

    /** OpenPlotter calculated  */
    OC,

    /** OMEGA Navigation System (obsolete)  */
    OM,

    /** Distress Alarm System (obsolete)  */
    OS,

    /** Proprietary Code  */
    P,

    /** QZSS regional GPS augmentation system (Japan)  */
    QZ,

    /** Radar and/or Radar Plotting  */
    RA,

    /** Record Book (reserved for future use)  */
    RB,

    /** Propulsion Machinery Including Remote Control  */
    RC,

    /** Rudder Angle Indicator(reserved for future use)  */
    RI,

    /** Indian Regional Navigation Satellite System (IRNSS)  */
    IR,

    /** AIS - NMEA 4.0 Physical Shore AIS Station  */
    SA,

    /** Steering Control System/Device (reserved for future use)  */
    SC,

    /** Sounder, depth  */
    SD,

    /** Steering Gear / Steering Engine  */
    SG,

    /** Electronic Positioning System, other/general  */
    SN,

    /** Sounder, Scanning  */
    SS,

    /** Raymarine SeaTalk ($STALK)  */
    ST,

    /** Track Control System (reserved for future use)  */
    TC,

    /** Turn Rate Indicator  */
    TI,

    /** TRANSIT Navigation System  */
    TR,

    /** Microprocessor Controller  */
    UP,

    /** VHF Data Exchange System: ASM  */
    VA,

    /** Velocity Sensor: Doppler, other/general  */
    VD,

    /** Velocity Sensor: Speed Log, Water, Magnetic  */
    VM,

    /** Voyage Data Recorder  */
    VR,

    /** VHF Data Exchange System: Satellite  */
    VS,

    /** VHF Data Exchange System: Terrestrial  */
    VT,

    /** Velocity Sensor: Speed Log, Water, Mechanical  */
    VW,

    /** Weather Instruments  */
    WI,

    /** Water Level Detection Systems  */
    WL,

    /** Wärtsilä proprietary  */
    WV,

    /** Transducer - Temperature (obsolete)  */
    YC,

    /** Transducer - Displacement, Angular or Linear (obsolete)  */
    YD,

    /** Transducer - Frequency (obsolete)  */
    YF,

    /** Transducer - Level (obsolete)  */
    YL,

    /** Transducer - Pressure (obsolete)  */
    YP,

    /** Transducer - Flow Rate (obsolete)  */
    YR,

    /** Transducer - Tachometer (obsolete)  */
    YT,

    /** Transducer - Volume (obsolete)  */
    YV,

    /** Transducer  */
    YX,

    /** Timekeeper - Atomic Clock  */
    ZA,

    /** Timekeeper - Chronometer  */
    ZC,

    /** Timekeeper - Quartz  */
    ZQ,

    /** Timekeeper - Radio Update, WWV or WWVH  */
    ZV;

    companion object {
        /**
         * Parses the Talker ID from specified sentence String and returns the
         * corresponding TalkerId enum using the [TalkerId.valueOf] method.
         *
         * @param nmea Sentence String
         * @return TalkerId enum
         * @throws IllegalArgumentException If specified String is not recognized as
         * NMEA sentence
         */
        fun parse(nmea: String): TalkerId {
            require(SentenceValidator.isSentence(nmea)) { "String is not a sentence" }
            val tid = if (nmea.startsWith("\$P")) "P" else nmea.substring(1, 3)
            return TalkerId.valueOf(tid)
        }
    }
}