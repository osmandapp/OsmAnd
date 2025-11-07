package net.osmand.plus.plugins.aistracker;

import android.util.Log;

import androidx.annotation.NonNull;

import net.sf.marineapi.ais.event.AbstractAISMessageListener;
import net.sf.marineapi.ais.message.*;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.SentenceId;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class AisMessageListener {

    private Timer timer;
    private DatagramSocket udpSocket;
    private Socket tcpSocket;
    private InputStream tcpStream;
    private InputStream fileStream;
    private SentenceReader sentenceReader = null;
    private Stack<SentenceListener> listeners = new Stack<>();
    private AisDataListener dataListener;

    public interface AisDataListener {
        void onAisObjectReceived(@NonNull AisObject ais);
    }

    public AisMessageListener(@NonNull AisDataListener dataListener, int udpPort) {
        initMembers(dataListener);
        try {
            udpSocket = new DatagramSocket(udpPort);
            udpSocket.setReuseAddress(true);
            initListeners();
            Log.d("AisMessageListener","new UDP listener, Port " + udpPort);
        }
        catch (Exception e) {
            Log.e("AisMessageListener","exception: " + e.getMessage());
            udpSocket = null;
        }
    }

    public AisMessageListener(@NonNull AisDataListener dataListener, @NonNull File file) {
        initMembers(dataListener);
        try {
            fileStream = new FileInputStream(file);
            initListeners();
        } catch (Exception e) {
            Log.e("AisMessageListener", "exception: " + e.getMessage());
            fileStream = null;
        }
    }

    public AisMessageListener(@NonNull AisDataListener dataListener, @NonNull String serverIp, int serverPort) {
        initMembers(dataListener);
        TimerTask taskCheckNetworkConnection = new TimerTask() {
            @Override
            public void run() {
                Log.d("AisMessageListener", "timer task taskCheckNetworkConnection running");
                if ((tcpSocket == null) || (!tcpSocket.isConnected())) {
                    try {
                        tcpSocket = new Socket();
                        tcpSocket.setTcpNoDelay(true);
                        tcpSocket.setReuseAddress(true);
                        // tcpSocket.connect(new InetSocketAddress(InetAddress.getByName(serverIp), serverPort), 5000);
                        tcpSocket.connect(new InetSocketAddress(InetAddress.getByName(serverIp), serverPort));
                        tcpStream = tcpSocket.getInputStream();
                        initListeners();
                        Log.d("AisMessageListener","new TCP listener");
                    }
                    catch (IOException e) {
                        Log.e("AisMessageListener","exception: " + e.getMessage());
                        tcpStream = null;
                        tcpSocket = null;
                    }
                }
            }
        };
        this.timer = new Timer();
        timer.schedule(taskCheckNetworkConnection, 1000, 30000);
    }

    private void initMembers(@NonNull AisDataListener dataListener) {
        this.dataListener = dataListener;
        this.udpSocket = null;
        this.tcpSocket = null;
        this.tcpStream = null;
        this.fileStream = null;
        this.listeners = new Stack<>();
    }

    private void initListeners() throws IOException {
        if (tcpStream != null) {
            sentenceReader = new SentenceReader(tcpStream);
        }
        if (fileStream != null) {
            sentenceReader = new SentenceReader(fileStream);
        }
        if (udpSocket != null) {
            sentenceReader = new SentenceReader(udpSocket);
        }
        if (sentenceReader != null) {
            new AisListener01();
            new AisListener02();
            new AisListener03();
            new AisListener04();
            new AisListener05();
            new AisListener09();
            new AisListener18();
            new AisListener19();
            new AisListener21();
            new AisListener24();
            new AisListener27();
            sentenceReader.start();
        } else {
            Log.e("AisMessageListener", "sentenceReader not initialized");
        }
    }

	private void removeListeners() {
		if (sentenceReader != null) {
			sentenceReader.stop();
			sentenceReader = null;
		}
		listeners = new Stack<>();
	}

    public void stopListener() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        removeListeners();
        if (tcpSocket != null) {
            Log.d("AisMessageListener","stopListener (TCP)");
            try {
                if (tcpSocket.isConnected()) {
                    tcpSocket.close();
                }
                if (tcpStream != null) {
                    tcpStream.close();
                }
            } catch (Exception ignore) { }
        }
        if (udpSocket != null) {
            Log.d("AisMessageListener","stopListener (UDP)");
            if (udpSocket.isConnected()) {
                udpSocket.disconnect();
            }
            udpSocket.close();
        }
    }

    public boolean checkTcpSocket() {
        return (tcpSocket != null) && (tcpStream != null);
    }

    protected void handleAisMessage(int aisType, Object obj) {
        AisObject ais = null;
        int msgType = 0;
        int mmsi = 0;
        int timeStamp = 0;
        int imo = 0;
        int heading = AisObjectConstants.INVALID_HEADING;
        int navStatus = AisObjectConstants.INVALID_NAV_STATUS;
        int manInd = AisObjectConstants.INVALID_MANEUVER_INDICATOR;
        int shipType = AisObjectConstants.INVALID_SHIP_TYPE;
        int dimensionToBow = AisObjectConstants.INVALID_DIMENSION;
        int dimensionToStern = AisObjectConstants.INVALID_DIMENSION;
        int dimensionToPort = AisObjectConstants.INVALID_DIMENSION;
        int dimensionToStarboard = AisObjectConstants.INVALID_DIMENSION;
        int etaMon = AisObjectConstants.INVALID_ETA;
        int etaDay = AisObjectConstants.INVALID_ETA;
        int etaHour = AisObjectConstants.INVALID_ETA_HOUR;
        int etaMin = AisObjectConstants.INVALID_ETA_MIN;
        int altitude = AisObjectConstants.INVALID_ALTITUDE;
        int aidType = AisObjectConstants.UNSPECIFIED_AID_TYPE;
        double draught = AisObjectConstants.INVALID_DRAUGHT;
        double cog = AisObjectConstants.INVALID_COG;
        double sog = AisObjectConstants.INVALID_SOG;
        double lat = AisObjectConstants.INVALID_LAT;
        double lon = AisObjectConstants.INVALID_LON;
        double rot = AisObjectConstants.INVALID_ROT;
        String callSign = null;
        String shipName = null;
        String destination = null;

        switch (aisType) {
            case 1: AISMessage01 aisMsg01 = (AISMessage01)obj; // position report class A
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg01.getMMSI()
                        + " Type: " + aisMsg01.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg01);
                mmsi = aisMsg01.getMMSI();
                msgType = aisMsg01.getMessageType();
                navStatus = aisMsg01.getNavigationalStatus();
                manInd = aisMsg01.getManouverIndicator();
                if (aisMsg01.hasTimeStamp()) { timeStamp = aisMsg01.getTimeStamp(); }
                if (aisMsg01.hasTrueHeading()) { heading = aisMsg01.getTrueHeading(); }
                if (aisMsg01.hasCourseOverGround()) { cog = aisMsg01.getCourseOverGround(); }
                if (aisMsg01.hasSpeedOverGround()) { sog = aisMsg01.getSpeedOverGround(); }
                if (aisMsg01.hasLatitude()) { lat = aisMsg01.getLatitudeInDegrees(); }
                if (aisMsg01.hasLongitude()) { lon = aisMsg01.getLongitudeInDegrees(); }
                if (aisMsg01.hasRateOfTurn()) { rot = aisMsg01.getRateOfTurn(); }
                ais = new AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading,
                        cog, sog, lat, lon, rot);
                break;

            case 2: AISMessage02 aisMsg02 = (AISMessage02)obj; // position report class A
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg02.getMMSI()
                        + " Type: " + aisMsg02.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg02);
                mmsi = aisMsg02.getMMSI();
                msgType = aisMsg02.getMessageType();
                navStatus = aisMsg02.getNavigationalStatus();
                manInd = aisMsg02.getManouverIndicator();
                if (aisMsg02.hasTimeStamp()) { timeStamp = aisMsg02.getTimeStamp(); }
                if (aisMsg02.hasTrueHeading()) { heading = aisMsg02.getTrueHeading(); }
                if (aisMsg02.hasCourseOverGround()) { cog = aisMsg02.getCourseOverGround(); }
                if (aisMsg02.hasSpeedOverGround()) { sog = aisMsg02.getSpeedOverGround(); }
                if (aisMsg02.hasLatitude()) { lat = aisMsg02.getLatitudeInDegrees(); }
                if (aisMsg02.hasLongitude()) { lon = aisMsg02.getLongitudeInDegrees(); }
                if (aisMsg02.hasRateOfTurn()) { rot = aisMsg02.getRateOfTurn(); }
                ais = new AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading,
                        cog, sog, lat, lon, rot);
                break;

            case 3: AISMessage03 aisMsg03 = (AISMessage03)obj; // position report class A
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg03.getMMSI()
                        + " Type: " + aisMsg03.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg03);
                mmsi = aisMsg03.getMMSI();
                msgType = aisMsg03.getMessageType();
                navStatus = aisMsg03.getNavigationalStatus();
                manInd = aisMsg03.getManouverIndicator();
                if (aisMsg03.hasTimeStamp()) { timeStamp = aisMsg03.getTimeStamp(); }
                if (aisMsg03.hasTrueHeading()) { heading = aisMsg03.getTrueHeading(); }
                if (aisMsg03.hasCourseOverGround()) { cog = aisMsg03.getCourseOverGround(); }
                if (aisMsg03.hasSpeedOverGround()) { sog = aisMsg03.getSpeedOverGround(); }
                if (aisMsg03.hasLatitude()) { lat = aisMsg03.getLatitudeInDegrees(); }
                if (aisMsg03.hasLongitude()) { lon = aisMsg03.getLongitudeInDegrees(); }
                if (aisMsg03.hasRateOfTurn()) { rot = aisMsg03.getRateOfTurn(); }
                ais = new AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading,
                        cog, sog, lat, lon, rot);
                break;

            case 4: AISMessage04 aisMsg04 = (AISMessage04)obj; // base station report
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg04.getMMSI()
                        + " Type: " + aisMsg04.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg04);
                mmsi = aisMsg04.getMMSI();
                msgType = aisMsg04.getMessageType();
                if (aisMsg04.hasLatitude()) { lat = aisMsg04.getLatitudeInDegrees(); }
                if (aisMsg04.hasLongitude()) { lon = aisMsg04.getLongitudeInDegrees(); }
                ais = new AisObject(mmsi, msgType, lat, lon);
                break;

            case 5: AISMessage05 aisMsg05 = (AISMessage05)obj; // static and voyage related data
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg05.getMMSI()
                        + " Type: " + aisMsg05.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg05);
                mmsi = aisMsg05.getMMSI();
                msgType = aisMsg05.getMessageType();
                imo = aisMsg05.getIMONumber();
                callSign = aisMsg05.getCallSign();
                shipName = aisMsg05.getName();
                shipType = aisMsg05.getTypeOfShipAndCargoType();
                dimensionToBow = aisMsg05.getBow();
                dimensionToStern = aisMsg05.getStern();
                dimensionToPort = aisMsg05.getPort();
                dimensionToStarboard = aisMsg05.getStarboard();
                draught = aisMsg05.getMaximumDraught();
                destination = aisMsg05.getDestination();
                etaMon = aisMsg05.getETAMonth();
                etaDay = aisMsg05.getETADay();
                etaHour = aisMsg05.getETAHour();
                etaMin = aisMsg05.getETAMinute();
                ais = new AisObject(mmsi, msgType, imo, callSign, shipName, shipType, dimensionToBow,
                        dimensionToStern, dimensionToPort, dimensionToStarboard, draught,
                        destination, etaMon, etaDay, etaHour, etaMin);
                break;

            case 9: AISMessage09 aisMsg09 = (AISMessage09)obj; // SAR aircraft position report
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg09.getMMSI()
                        + " Type: " + aisMsg09.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg09);
                mmsi = aisMsg09.getMMSI();
                msgType = aisMsg09.getMessageType();
                timeStamp = aisMsg09.getTimeStamp();
                cog = aisMsg09.getCourseOverGround();
                sog = aisMsg09.getSpeedOverGround();
                altitude = aisMsg09.getAltitude();
                if (aisMsg09.hasLatitude()) { lat = aisMsg09.getLatitudeInDegrees(); }
                if (aisMsg09.hasLongitude()) { lon = aisMsg09.getLongitudeInDegrees(); }
                ais = new AisObject(mmsi, msgType, timeStamp, altitude, cog, sog, lat, lon);
                break;

            case 18: AISMessage18 aisMsg18 = (AISMessage18)obj; // basic class B position report
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg18.getMMSI()
                        + " Type: " + aisMsg18.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg18);
                mmsi = aisMsg18.getMMSI();
                msgType = aisMsg18.getMessageType();
                if (aisMsg18.hasTimeStamp()) { timeStamp = aisMsg18.getTimeStamp(); }
                if (aisMsg18.hasTrueHeading()) { heading = aisMsg18.getTrueHeading(); }
                if (aisMsg18.hasCourseOverGround()) { cog = aisMsg18.getCourseOverGround(); }
                if (aisMsg18.hasSpeedOverGround()) { sog = aisMsg18.getSpeedOverGround(); }
                if (aisMsg18.hasLatitude()) { lat = aisMsg18.getLatitudeInDegrees(); }
                if (aisMsg18.hasLongitude()) { lon = aisMsg18.getLongitudeInDegrees(); }
                ais = new AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading,
                        cog, sog, lat, lon, rot);
                break;

            case 19: AISMessage19 aisMsg19 = (AISMessage19)obj; // extended class B position report
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg19.getMMSI()
                        + " Type: " + aisMsg19.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg19);
                mmsi = aisMsg19.getMMSI();
                msgType = aisMsg19.getMessageType();
                shipType = aisMsg19.getTypeOfShipAndCargoType();
                dimensionToBow = aisMsg19.getBow();
                dimensionToStern = aisMsg19.getStern();
                dimensionToPort = aisMsg19.getPort();
                dimensionToStarboard = aisMsg19.getStarboard();
                if (aisMsg19.hasTimeStamp()) { timeStamp = aisMsg19.getTimeStamp(); }
                if (aisMsg19.hasTrueHeading()) { heading = aisMsg19.getTrueHeading(); }
                if (aisMsg19.hasCourseOverGround()) { cog = aisMsg19.getCourseOverGround(); }
                if (aisMsg19.hasSpeedOverGround()) { sog = aisMsg19.getSpeedOverGround(); }
                if (aisMsg19.hasLatitude()) { lat = aisMsg19.getLatitudeInDegrees(); }
                if (aisMsg19.hasLongitude()) { lon = aisMsg19.getLongitudeInDegrees(); }
                ais = new AisObject(mmsi, msgType, timeStamp, heading, cog, sog, lat, lon,
                        shipType, dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard);
                break;

            case 21: AISMessage21 aisMsg21 = (AISMessage21)obj; // aid-to-navigation report
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg21.getMMSI()
                        + " Type: " + aisMsg21.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg21);
                mmsi = aisMsg21.getMMSI();
                msgType = aisMsg21.getMessageType();
                dimensionToBow = aisMsg21.getBow();
                dimensionToStern = aisMsg21.getStern();
                dimensionToPort = aisMsg21.getPort();
                dimensionToStarboard = aisMsg21.getStarboard();
                aidType = aisMsg21.getAidType();
                if (aisMsg21.hasLatitude()) { lat = aisMsg21.getLatitudeInDegrees(); }
                if (aisMsg21.hasLongitude()) { lon = aisMsg21.getLongitudeInDegrees(); }
                ais = new AisObject(mmsi, msgType, lat, lon, aidType,
                        dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard);
                break;

            case 24: AISMessage24 aisMsg24 = (AISMessage24)obj; // static data report (like type 5)
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg24.getMMSI()
                        + " Type: " + aisMsg24.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg24);
                mmsi = aisMsg24.getMMSI();
                msgType = aisMsg24.getMessageType();
                callSign = aisMsg24.getCallSign();
                shipName = aisMsg24.getName();
                shipType = aisMsg24.getTypeOfShipAndCargoType();
                dimensionToBow = aisMsg24.getBow();
                dimensionToStern = aisMsg24.getStern();
                dimensionToPort = aisMsg24.getPort();
                dimensionToStarboard = aisMsg24.getStarboard();
                ais = new AisObject(mmsi, msgType, imo, callSign, shipName, shipType, dimensionToBow,
                        dimensionToStern, dimensionToPort, dimensionToStarboard, draught,
                        null, etaMon, etaDay, etaHour, etaMin);
                break;

            case 27: AISMessage27 aisMsg27 = (AISMessage27)obj; // long range broadcast message
                Log.d("AisMessageListener","handleAisMessage() MMSI: " + aisMsg27.getMMSI()
                        + " Type: " + aisMsg27.getMessageType());
                Log.d("AisMessageListener","handleAisMessage()" + aisMsg27);
                mmsi = aisMsg27.getMMSI();
                msgType = aisMsg27.getMessageType();
                navStatus = aisMsg27.getNavigationalStatus();
                manInd = aisMsg27.getManouverIndicator();
                if (aisMsg27.hasTimeStamp()) { timeStamp = aisMsg27.getTimeStamp(); }
                if (aisMsg27.hasTrueHeading()) { heading = aisMsg27.getTrueHeading(); }
                if (aisMsg27.hasCourseOverGround()) { cog = aisMsg27.getCourseOverGround(); }
                if (aisMsg27.hasSpeedOverGround()) { sog = aisMsg27.getSpeedOverGround(); }
                if (aisMsg27.hasLatitude()) { lat = aisMsg27.getLatitudeInDegrees(); }
                if (aisMsg27.hasLongitude()) { lon = aisMsg27.getLongitudeInDegrees(); }
                if (aisMsg27.hasRateOfTurn()) { rot = aisMsg27.getRateOfTurn(); }
                ais = new AisObject(mmsi, msgType, timeStamp, navStatus, manInd, heading,
                        cog, sog, lat, lon, rot);
                break;

            default:
                Log.e("AisMessageListener","handleAisMessage() invalid argument aisType: "+ aisType);
                return;
        }
        dataListener.onAisObjectReceived(ais);
    }

    private void initEmbeddedLister(int aisType, @NonNull SentenceListener listener) {
        //AisMessageListener.this.sentenceReader.addSentenceListener(listener); // listen to all (!) NMEA messages
        AisMessageListener.this.sentenceReader.addSentenceListener(listener, SentenceId.VDM);
        AisMessageListener.this.sentenceReader.addSentenceListener(listener, SentenceId.VDO);
        AisMessageListener.this.listeners.push(listener);
        Log.d("AisMessageListener","Listener Type " + aisType + " started");
    }

    private class AisListener01 extends AbstractAISMessageListener<AISMessage01> {
        public AisListener01() { initEmbeddedLister(1, this); }
        @Override
        public void onMessage(AISMessage01 msg) {
            handleAisMessage(1, msg);
        }
    }

    private class AisListener02 extends AbstractAISMessageListener<AISMessage02> {
        public AisListener02() { initEmbeddedLister(2, this); }
        @Override
        public void onMessage(AISMessage02 msg) {
            handleAisMessage(2, msg);
        }
    }

    private class AisListener03 extends AbstractAISMessageListener<AISMessage03> {
        public AisListener03() { initEmbeddedLister(3, this); }
        @Override
        public void onMessage(AISMessage03 msg) {
            handleAisMessage(3, msg);
        }
    }

    private class AisListener04 extends AbstractAISMessageListener<AISMessage04> {
        public AisListener04() { initEmbeddedLister(4, this); }
        @Override
        public void onMessage(AISMessage04 msg) {
            handleAisMessage(4, msg);
        }
    }

    private class AisListener05 extends AbstractAISMessageListener<AISMessage05> {
        public AisListener05() { initEmbeddedLister(5, this); }
        @Override
        public void onMessage(AISMessage05 msg) {
            handleAisMessage(5, msg);
        }
    }

    private class AisListener09 extends AbstractAISMessageListener<AISMessage09> {
        public AisListener09() { initEmbeddedLister(9, this); }
        @Override
        public void onMessage(AISMessage09 msg) {
            handleAisMessage(9, msg);
        }
    }

    private class AisListener18 extends AbstractAISMessageListener<AISMessage18> {
        public AisListener18() { initEmbeddedLister(18, this); }
        @Override
        public void onMessage(AISMessage18 msg) {
            handleAisMessage(18, msg);
        }
    }

    private class AisListener19 extends AbstractAISMessageListener<AISMessage19> {
        public AisListener19() { initEmbeddedLister(19, this); }
        @Override
        public void onMessage(AISMessage19 msg) {
            handleAisMessage(19, msg);
        }
    }

    private class AisListener21 extends AbstractAISMessageListener<AISMessage21> {
        public AisListener21() { initEmbeddedLister(21, this); }
        @Override
        public void onMessage(AISMessage21 msg) {
            handleAisMessage(21, msg);
        }
    }

    private class AisListener24 extends AbstractAISMessageListener<AISMessage24> {
        public AisListener24() { initEmbeddedLister(24, this); }
        @Override
        public void onMessage(AISMessage24 msg) {
            handleAisMessage(24, msg);
        }
    }

    private class AisListener27 extends AbstractAISMessageListener<AISMessage27> {
        public AisListener27() { initEmbeddedLister(27, this); }
        @Override
        public void onMessage(AISMessage27 msg) {
            handleAisMessage(27, msg);
        }
    }
}
