package net.osmand.shared.aistracker

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.utils.io.readText
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.osmand.shared.util.LoggerFactory
import net.sf.marineapi.ais.event.AbstractAISMessageListener
import net.sf.marineapi.ais.message.AISMessage01
import net.sf.marineapi.ais.message.AISMessage02
import net.sf.marineapi.ais.message.AISMessage03
import net.sf.marineapi.ais.message.AISMessage04
import net.sf.marineapi.ais.message.AISMessage05
import net.sf.marineapi.ais.message.AISMessage09
import net.sf.marineapi.ais.message.AISMessage18
import net.sf.marineapi.ais.message.AISMessage19
import net.sf.marineapi.ais.message.AISMessage21
import net.sf.marineapi.ais.message.AISMessage24
import net.sf.marineapi.ais.message.AISMessage27
import net.sf.marineapi.nmea.event.SentenceListener
import net.sf.marineapi.nmea.parser.SentenceFactory
import net.sf.marineapi.nmea.sentence.AISSentence

open class AisMessageListener {
    private val dataListener: AisDataListener
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        private var networkJob: Job? = null
        private val listeners = mutableListOf<SentenceListener>()

    // For Simulation (File)
    protected constructor(dataListener: AisDataListener) {
        this.dataListener = dataListener
        initListeners()
    }

    // For TCP
    constructor(dataListener: AisDataListener, serverIp: String, serverPort: Int) {
        this.dataListener = dataListener
        startTcpConnection(serverIp, serverPort)
    }

    // For UDP
    constructor(dataListener: AisDataListener, udpPort: Int) {
        this.dataListener = dataListener
        startUdpConnection(udpPort)
    }

    private fun startTcpConnection(serverIp: String, serverPort: Int) {
        initListeners()

        networkJob = scope.launch {
            val selectorManager = SelectorManager(Dispatchers.IO)
            while (isActive) {
                var socket: Socket? = null
                try {
                    LoggerFactory.getLogger("AisMessageListener").debug("TCP connection starting")
                    socket = aSocket(selectorManager).tcp().connect(serverIp, serverPort)
                    socket.socketContext.also { it.invokeOnCompletion { } } // Avoid crash
                    val readChannel = socket.openReadChannel()
                    while (isActive) {
                        val line = readChannel.readUTF8Line() ?: break
                        processLine(line)
                    }
                } catch (e: Exception) {
                    LoggerFactory.getLogger("AisMessageListener").error("TCP exception: ${e.message}")
                } finally {
                    socket?.close()
                }
                delay(10000) // reconnect delay
            }
        }
    }

    private fun startUdpConnection(udpPort: Int) {
        initListeners()

        networkJob = scope.launch {
            val selectorManager = SelectorManager(Dispatchers.IO)
            while (isActive) {
                var socket: BoundDatagramSocket? = null
                try {
                    LoggerFactory.getLogger("AisMessageListener").debug("UDP listener starting on port $udpPort")
                    socket = aSocket(selectorManager).udp().bind(InetSocketAddress("0.0.0.0", udpPort))
                    while (isActive) {
                        val datagram = socket.receive()
                        val text = datagram.packet.readText()
                        text.lineSequence().forEach { line ->
                            val trimmed = line.trimEnd('\r')
                            if (trimmed.isNotEmpty()) {
                                processLine(trimmed)
                            }
                        }
                    }
                } catch (e: Exception) {
                    LoggerFactory.getLogger("AisMessageListener").error("UDP exception: ${e.message}")
                } finally {
                    socket?.close()
                }
                delay(10000) // reconnect delay
            }
        }
    }

    protected fun processLine(line: String) {
        try {
            var data = line
            if (data.startsWith("\\")) {
                if (data.contains("\\!")) {
                    data = "!" + data.substringAfter("\\!")
                } else if (data.contains("\\$")) {
                    data = "$" + data.substringAfter("\\$")
                }
            }

            val sentence = SentenceFactory.instance.createParser(data)
            if (sentence is AISSentence) {
                for (listener in listeners) {
                    listener.sentenceRead(net.sf.marineapi.nmea.event.SentenceEvent(this, sentence))
                }
            }
        } catch (_: Exception) {
            // ignore parse error
        }
    }

    private fun initListeners() {
        AisListener01()
        AisListener02()
        AisListener03()
        AisListener04()
        AisListener05()
        AisListener09()
        AisListener18()
        AisListener19()
        AisListener21()
        AisListener24()
        AisListener27()
    }

    private fun removeListeners() {
        listeners.clear()
    }

    fun stopListener() {
        networkJob?.cancel()
        removeListeners()
        try {
            
        } catch (e: Exception) {
            // ignore
        }
        scope.cancel()
    }

    fun checkTcpSocket(): Boolean {
        return networkJob?.isActive == true
    }

    protected open fun handleAisMessage(aisType: Int, obj: Any) {
        var ais: AisObject? = null
        var msgType = 0
        var mmsi = 0
        var timeStamp = 0
        var imo = 0
        var heading = AisObjectConstants.INVALID_HEADING
        var navStatus = AisObjectConstants.INVALID_NAV_STATUS
        var manInd = AisObjectConstants.INVALID_MANEUVER_INDICATOR
        var shipType = AisObjectConstants.INVALID_SHIP_TYPE
        var dimensionToBow = AisObjectConstants.INVALID_DIMENSION
        var dimensionToStern = AisObjectConstants.INVALID_DIMENSION
        var dimensionToPort = AisObjectConstants.INVALID_DIMENSION
        var dimensionToStarboard = AisObjectConstants.INVALID_DIMENSION
        var etaMon = AisObjectConstants.INVALID_ETA
        var etaDay = AisObjectConstants.INVALID_ETA
        var etaHour = AisObjectConstants.INVALID_ETA_HOUR
        var etaMin = AisObjectConstants.INVALID_ETA_MIN
        var altitude = AisObjectConstants.INVALID_ALTITUDE
        var aidType = AisObjectConstants.UNSPECIFIED_AID_TYPE
        var draught = AisObjectConstants.INVALID_DRAUGHT
        var cog = AisObjectConstants.INVALID_COG
        var sog = AisObjectConstants.INVALID_SOG
        var lat = AisObjectConstants.INVALID_LAT
        var lon = AisObjectConstants.INVALID_LON
        var rot = AisObjectConstants.INVALID_ROT
        var callSign: String? = null
        var shipName: String? = null
        var destination: String? = null

        when (aisType) {
            1 -> {
                val aisMsg01 = obj as AISMessage01
                mmsi = aisMsg01.mMSI
                msgType = aisMsg01.messageType
                navStatus = aisMsg01.navigationalStatus
                manInd = aisMsg01.manouverIndicator
                if (aisMsg01.hasTimeStamp()) { timeStamp = aisMsg01.timeStamp }
                if (aisMsg01.hasTrueHeading()) { heading = aisMsg01.trueHeading }
                if (aisMsg01.hasCourseOverGround()) { cog = aisMsg01.courseOverGround }
                if (aisMsg01.hasSpeedOverGround()) { sog = aisMsg01.speedOverGround }
                if (aisMsg01.hasLatitude()) { lat = aisMsg01.latitudeInDegrees }
                if (aisMsg01.hasLongitude()) { lon = aisMsg01.longitudeInDegrees }
                if (aisMsg01.hasRateOfTurn()) { rot = aisMsg01.rateOfTurn }
                ais = AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading, cog, sog, lat, lon, rot)
            }
            2 -> {
                val aisMsg02 = obj as AISMessage02
                mmsi = aisMsg02.mMSI
                msgType = aisMsg02.messageType
                navStatus = aisMsg02.navigationalStatus
                manInd = aisMsg02.manouverIndicator
                if (aisMsg02.hasTimeStamp()) { timeStamp = aisMsg02.timeStamp }
                if (aisMsg02.hasTrueHeading()) { heading = aisMsg02.trueHeading }
                if (aisMsg02.hasCourseOverGround()) { cog = aisMsg02.courseOverGround }
                if (aisMsg02.hasSpeedOverGround()) { sog = aisMsg02.speedOverGround }
                if (aisMsg02.hasLatitude()) { lat = aisMsg02.latitudeInDegrees }
                if (aisMsg02.hasLongitude()) { lon = aisMsg02.longitudeInDegrees }
                if (aisMsg02.hasRateOfTurn()) { rot = aisMsg02.rateOfTurn }
                ais = AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading, cog, sog, lat, lon, rot)
            }
            3 -> {
                val aisMsg03 = obj as AISMessage03
                mmsi = aisMsg03.mMSI
                msgType = aisMsg03.messageType
                navStatus = aisMsg03.navigationalStatus
                manInd = aisMsg03.manouverIndicator
                if (aisMsg03.hasTimeStamp()) { timeStamp = aisMsg03.timeStamp }
                if (aisMsg03.hasTrueHeading()) { heading = aisMsg03.trueHeading }
                if (aisMsg03.hasCourseOverGround()) { cog = aisMsg03.courseOverGround }
                if (aisMsg03.hasSpeedOverGround()) { sog = aisMsg03.speedOverGround }
                if (aisMsg03.hasLatitude()) { lat = aisMsg03.latitudeInDegrees }
                if (aisMsg03.hasLongitude()) { lon = aisMsg03.longitudeInDegrees }
                if (aisMsg03.hasRateOfTurn()) { rot = aisMsg03.rateOfTurn }
                ais = AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading, cog, sog, lat, lon, rot)
            }
            4 -> {
                val aisMsg04 = obj as AISMessage04
                mmsi = aisMsg04.mMSI
                msgType = aisMsg04.messageType
                if (aisMsg04.hasLatitude()) { lat = aisMsg04.latitudeInDegrees }
                if (aisMsg04.hasLongitude()) { lon = aisMsg04.longitudeInDegrees }
                ais = AisObject(mmsi, msgType, lat, lon)
            }
            5 -> {
                val aisMsg05 = obj as AISMessage05
                mmsi = aisMsg05.mMSI
                msgType = aisMsg05.messageType
                imo = aisMsg05.iMONumber
                callSign = aisMsg05.callSign
                shipName = aisMsg05.name
                shipType = aisMsg05.typeOfShipAndCargoType
                dimensionToBow = aisMsg05.bow
                dimensionToStern = aisMsg05.stern
                dimensionToPort = aisMsg05.port
                dimensionToStarboard = aisMsg05.starboard
                draught = aisMsg05.maximumDraught
                destination = aisMsg05.destination
                etaMon = aisMsg05.eTAMonth
                etaDay = aisMsg05.eTADay
                etaHour = aisMsg05.eTAHour
                etaMin = aisMsg05.eTAMinute
                ais = AisObject(mmsi, msgType, imo, callSign, shipName, shipType, dimensionToBow,
                        dimensionToStern, dimensionToPort, dimensionToStarboard, draught,
                        destination, etaMon, etaDay, etaHour, etaMin)
            }
            9 -> {
                val aisMsg09 = obj as AISMessage09
                mmsi = aisMsg09.mMSI
                msgType = aisMsg09.messageType
                timeStamp = aisMsg09.timeStamp
                cog = aisMsg09.courseOverGround
                sog = aisMsg09.speedOverGround.toDouble()
                altitude = aisMsg09.altitude
                if (aisMsg09.hasLatitude()) { lat = aisMsg09.latitudeInDegrees }
                if (aisMsg09.hasLongitude()) { lon = aisMsg09.longitudeInDegrees }
                ais = AisObject(mmsi, msgType, timeStamp, altitude, cog, sog, lat, lon)
            }
            18 -> {
                val aisMsg18 = obj as AISMessage18
                mmsi = aisMsg18.mMSI
                msgType = aisMsg18.messageType
                if (aisMsg18.hasTimeStamp()) { timeStamp = aisMsg18.timeStamp }
                if (aisMsg18.hasTrueHeading()) { heading = aisMsg18.trueHeading }
                if (aisMsg18.hasCourseOverGround()) { cog = aisMsg18.courseOverGround }
                if (aisMsg18.hasSpeedOverGround()) { sog = aisMsg18.speedOverGround }
                if (aisMsg18.hasLatitude()) { lat = aisMsg18.latitudeInDegrees }
                if (aisMsg18.hasLongitude()) { lon = aisMsg18.longitudeInDegrees }
                ais = AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading, cog, sog, lat, lon, rot)
            }
            19 -> {
                val aisMsg19 = obj as AISMessage19
                mmsi = aisMsg19.mMSI
                msgType = aisMsg19.messageType
                shipType = aisMsg19.typeOfShipAndCargoType
                dimensionToBow = aisMsg19.bow
                dimensionToStern = aisMsg19.stern
                dimensionToPort = aisMsg19.port
                dimensionToStarboard = aisMsg19.starboard
                if (aisMsg19.hasTimeStamp()) { timeStamp = aisMsg19.timeStamp }
                if (aisMsg19.hasTrueHeading()) { heading = aisMsg19.trueHeading }
                if (aisMsg19.hasCourseOverGround()) { cog = aisMsg19.courseOverGround }
                if (aisMsg19.hasSpeedOverGround()) { sog = aisMsg19.speedOverGround }
                if (aisMsg19.hasLatitude()) { lat = aisMsg19.latitudeInDegrees }
                if (aisMsg19.hasLongitude()) { lon = aisMsg19.longitudeInDegrees }
                ais = AisObject(mmsi, msgType, timeStamp, heading, cog, sog, lat, lon,
                        shipType, dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard)
            }
            21 -> {
                val aisMsg21 = obj as AISMessage21
                mmsi = aisMsg21.mMSI
                msgType = aisMsg21.messageType
                dimensionToBow = aisMsg21.bow
                dimensionToStern = aisMsg21.stern
                dimensionToPort = aisMsg21.port
                dimensionToStarboard = aisMsg21.starboard
                aidType = aisMsg21.aidType
                if (aisMsg21.hasLatitude()) { lat = aisMsg21.latitudeInDegrees }
                if (aisMsg21.hasLongitude()) { lon = aisMsg21.longitudeInDegrees }
                ais = AisObject(mmsi, msgType, lat, lon, aidType,
                        dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard)
            }
            24 -> {
                val aisMsg24 = obj as AISMessage24
                mmsi = aisMsg24.mMSI
                msgType = aisMsg24.messageType
                callSign = aisMsg24.callSign
                shipName = aisMsg24.name
                shipType = aisMsg24.typeOfShipAndCargoType
                dimensionToBow = aisMsg24.bow
                dimensionToStern = aisMsg24.stern
                dimensionToPort = aisMsg24.port
                dimensionToStarboard = aisMsg24.starboard
                ais = AisObject(mmsi, msgType, imo, callSign, shipName, shipType, dimensionToBow,
                        dimensionToStern, dimensionToPort, dimensionToStarboard, draught,
                        null, etaMon, etaDay, etaHour, etaMin)
            }
            27 -> {
                val aisMsg27 = obj as AISMessage27
                mmsi = aisMsg27.mMSI
                msgType = aisMsg27.messageType
                navStatus = aisMsg27.navigationalStatus
                manInd = aisMsg27.manouverIndicator
                if (aisMsg27.hasTimeStamp()) { timeStamp = aisMsg27.timeStamp }
                if (aisMsg27.hasTrueHeading()) { heading = aisMsg27.trueHeading }
                if (aisMsg27.hasCourseOverGround()) { cog = aisMsg27.courseOverGround }
                if (aisMsg27.hasSpeedOverGround()) { sog = aisMsg27.speedOverGround }
                if (aisMsg27.hasLatitude()) { lat = aisMsg27.latitudeInDegrees }
                if (aisMsg27.hasLongitude()) { lon = aisMsg27.longitudeInDegrees }
                if (aisMsg27.hasRateOfTurn()) { rot = aisMsg27.rateOfTurn }
                ais = AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading, cog, sog, lat, lon, rot)
            }
            else -> {
                LoggerFactory.getLogger("AisMessageListener").error("handleAisMessage() invalid argument aisType: \$aisType")
                return
            }
        }
        if (ais != null) {
            dataListener.onAisObjectReceived(ais)
        }
    }

    private fun initEmbeddedLister(aisType: Int, listener: SentenceListener) {
        
        listeners.add(listener)
    }

    private inner class AisListener01 : AbstractAISMessageListener<AISMessage01>(1) {
        init { initEmbeddedLister(1, this) }
        override fun onMessage(msg: AISMessage01?) = handleAisMessage(1, msg!!)
    }

    private inner class AisListener02 : AbstractAISMessageListener<AISMessage02>(2) {
        init { initEmbeddedLister(2, this) }
        override fun onMessage(msg: AISMessage02?) = handleAisMessage(2, msg!!)
    }

    private inner class AisListener03 : AbstractAISMessageListener<AISMessage03>(3) {
        init { initEmbeddedLister(3, this) }
        override fun onMessage(msg: AISMessage03?) = handleAisMessage(3, msg!!)
    }

    private inner class AisListener04 : AbstractAISMessageListener<AISMessage04>(4) {
        init { initEmbeddedLister(4, this) }
        override fun onMessage(msg: AISMessage04?) = handleAisMessage(4, msg!!)
    }

    private inner class AisListener05 : AbstractAISMessageListener<AISMessage05>(5) {
        init { initEmbeddedLister(5, this) }
        override fun onMessage(msg: AISMessage05?) = handleAisMessage(5, msg!!)
    }

    private inner class AisListener09 : AbstractAISMessageListener<AISMessage09>(9) {
        init { initEmbeddedLister(9, this) }
        override fun onMessage(msg: AISMessage09?) = handleAisMessage(9, msg!!)
    }

    private inner class AisListener18 : AbstractAISMessageListener<AISMessage18>(18) {
        init { initEmbeddedLister(18, this) }
        override fun onMessage(msg: AISMessage18?) = handleAisMessage(18, msg!!)
    }

    private inner class AisListener19 : AbstractAISMessageListener<AISMessage19>(19) {
        init { initEmbeddedLister(19, this) }
        override fun onMessage(msg: AISMessage19?) = handleAisMessage(19, msg!!)
    }

    private inner class AisListener21 : AbstractAISMessageListener<AISMessage21>(21) {
        init { initEmbeddedLister(21, this) }
        override fun onMessage(msg: AISMessage21?) = handleAisMessage(21, msg!!)
    }

    private inner class AisListener24 : AbstractAISMessageListener<AISMessage24>(24) {
        init { initEmbeddedLister(24, this) }
        override fun onMessage(msg: AISMessage24?) = handleAisMessage(24, msg!!)
    }

    private inner class AisListener27 : AbstractAISMessageListener<AISMessage27>(27) {
        init { initEmbeddedLister(27, this) }
        override fun onMessage(msg: AISMessage27?) = handleAisMessage(27, msg!!)
    }
}

interface AisDataListener {
    fun onAisObjectReceived(ais: AisObject)
}
