package net.osmand.ais.simulate // Correct package name updated

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

// Data class to hold our vessel's information
data class MyVessel(
    var lat: Double,
    var lon: Double,
    var speed: Double, // Knots
    var course: Double // Degrees
)

class MainActivity : AppCompatActivity() {

    private lateinit var serverButton: Button
    private lateinit var statusTextView: TextView

    private var serverJob: Job? = null
    private var isServerRunning = false

    private val UDP_PORT = 10110
    private val UDP_HOST = "127.0.0.1"
    private val NMEA_SAMPLE_FILE = "AIS-nmea-sample.txt"
    private val DELAY_MS = 1000L

    // Initialize our vessel's data (using Kyiv's location as a starting point)
    private val myVessel = MyVessel(
        lat = 52.37113,
        lon = 4.89201,
        speed = 10.5,
        course = 45.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverButton = findViewById(R.id.serverButton)
        statusTextView = findViewById(R.id.statusTextView)

        serverButton.setOnClickListener {
            if (isServerRunning) {
                stopServer()
            } else {
                startServer()
            }
        }
    }

    private fun startServer() {
        serverJob = lifecycleScope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                val aisLines = readAisSampleFile(this@MainActivity)
                if (aisLines.isEmpty()) {
                    updateUi("Error: Could not read '$NMEA_SAMPLE_FILE' or file is empty.", "Start Server")
                    return@launch
                }

                socket = DatagramSocket()
                val destination = InetAddress.getByName(UDP_HOST)
                isServerRunning = true
                updateUi("Broadcasting from file & own location to $UDP_HOST:$UDP_PORT", "Stop Server")

                var fileLineIndex = 0
                var aisMessageCounter = 0
                while (isActive) {
                    // 1. Send one line of AIS data from the file
                    val aisMessage = (aisLines[fileLineIndex] + "\r\n").toByteArray()
                    Log.i("net.osmand.ais.simulate", "MSG: " + aisLines[fileLineIndex]);
                    socket.send(DatagramPacket(aisMessage, aisMessage.size, destination, UDP_PORT))

                    fileLineIndex = (fileLineIndex + 1) % aisLines.size
                    aisMessageCounter++
                    delay(DELAY_MS)

                    // 2. Inject our own location every 5 messages
                    if (aisMessageCounter % 5 == 0) {
                        // Simulate movement
                        myVessel.lon += 0.0005

                        val myLocationSentence = generateGPRMC(myVessel)
                        val myLocationMessage = (myLocationSentence + "\r\n").toByteArray()
                        socket.send(DatagramPacket(myLocationMessage, myLocationMessage.size, destination, UDP_PORT))
                        Log.i("net.osmand.ais.simulate", "MSG: " + myLocationSentence);
                        // Add an extra delay for our own position message
                        delay(DELAY_MS)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateUi("Error: ${e.message}", "Start Server")
            } finally {
                socket?.close()
                isServerRunning = false
                updateUi("Server is Inactive", "Start Server")
            }
        }
    }

    private fun stopServer() {
        serverJob?.cancel()
    }

    // --- NMEA and File Helper Functions ---

    private fun calculateChecksum(sentence: String): String {
        var checksum = 0
        // The checksum is a simple XOR of all characters between '$' and '*'
        for (char in sentence.substring(1)) {
            checksum = checksum xor char.code
        }
        return checksum.toString(16).uppercase().padStart(2, '0')
    }

    private fun generateGPRMC(vesselData: MyVessel): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("HHmmss.SS", Locale.US)
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")
        val time = timeFormat.format(now)

        val latDeg = floor(abs(vesselData.lat)).toInt()
        val latMin = (abs(vesselData.lat) - latDeg) * 60
        // Format: DDMM.MMMM,N/S
        val latitude = "%02d".format(latDeg) +
                String.format(Locale.US, "%07.4f", latMin) +
                "," + if (vesselData.lat >= 0) 'N' else 'S'

        val lonDeg = floor(abs(vesselData.lon)).toInt()
        val lonMin = (abs(vesselData.lon) - lonDeg) * 60
        // Format: DDDMM.MMMM,E/W
        val longitude = "%03d".format(lonDeg) +
                String.format(Locale.US, "%07.4f", lonMin) +
                "," + if (vesselData.lon >= 0) 'E' else 'W'

        val speed = String.format(Locale.US, "%.2f", vesselData.speed)
        val course = String.format(Locale.US, "%.2f", vesselData.course)

        val dateFormat = SimpleDateFormat("ddMMyy", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = dateFormat.format(now)

        val sentenceWithoutChecksum = "\$GPRMC,$time,A,$latitude,$longitude,$speed,$course,$date,,,A"
        val checksum = calculateChecksum(sentenceWithoutChecksum)
        return "$sentenceWithoutChecksum*$checksum"
    }

    private fun readAisSampleFile(context: Context): List<String> {
        return try {
            context.assets.open(NMEA_SAMPLE_FILE).bufferedReader().readLines()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun updateUi(status: String, buttonText: String) {
        withContext(Dispatchers.Main) {
            statusTextView.text = status
            serverButton.text = buttonText
        }
    }
}