package net.osmand.osm.io;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

public class NetworkUtilsTest {

	private HttpServer server;
	private int serverPort;
	private File tempGpxFile;
	private final String sampleGpxContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx creator=\"OsmAnd\"><trk></trk></gpx>";

	@Before
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		serverPort = server.getAddress().getPort();
		server.start();

		tempGpxFile = File.createTempFile("test_track", ".gpx");
		try (FileOutputStream fos = new FileOutputStream(tempGpxFile)) {
			fos.write(sampleGpxContent.getBytes(StandardCharsets.UTF_8));
		}
	}

	@After
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
		if (tempGpxFile != null && tempGpxFile.exists()) {
			tempGpxFile.delete();
		}
	}

	@Test
	public void testUploadFileBasicAuthMultipart() throws Exception {
		AtomicReference<String> capturedMethod = new AtomicReference<>();
		AtomicReference<String> capturedQuery = new AtomicReference<>();
		AtomicReference<String> capturedContentType = new AtomicReference<>();
		AtomicReference<String> capturedAuth = new AtomicReference<>();
		AtomicReference<byte[]> capturedBody = new AtomicReference<>();

		server.createContext("/api/0.6/gpx/create", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				capturedMethod.set(exchange.getRequestMethod());
				capturedQuery.set(exchange.getRequestURI().getQuery());
				capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
				capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (InputStream is = exchange.getRequestBody()) {
					byte[] buf = new byte[4096];
					int n;
					while ((n = is.read(buf)) != -1) {
						baos.write(buf, 0, n);
					}
				}
				capturedBody.set(baos.toByteArray());

				byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create";
		Map<String, String> additionalData = new LinkedHashMap<>();
		additionalData.put("description", "Test track description");
		additionalData.put("tags", "bicycle, road");
		additionalData.put("visibility", "identifiable");

		String result = NetworkUtils.uploadFile(url, tempGpxFile, "testuser:testpass", null, "file", false, additionalData);
		Assert.assertNull("Upload should succeed without error", result);

		Assert.assertEquals("POST", capturedMethod.get());
		Assert.assertNull("URL query parameters should be empty/null, metadata must be in body", capturedQuery.get());
		Assert.assertTrue("Content-Type should be multipart",
				capturedContentType.get() != null && capturedContentType.get().startsWith("multipart/form-data; boundary="));
		Assert.assertTrue("Authorization should be Basic",
				capturedAuth.get() != null && capturedAuth.get().startsWith("Basic "));

		String bodyText = new String(capturedBody.get(), StandardCharsets.UTF_8);
		Assert.assertTrue("Body should contain description part",
				bodyText.contains("name=\"description\"") && bodyText.contains("Test track description"));
		Assert.assertTrue("Body should contain tags part",
				bodyText.contains("name=\"tags\"") && bodyText.contains("bicycle, road"));
		Assert.assertTrue("Body should contain visibility part",
				bodyText.contains("name=\"visibility\"") && bodyText.contains("identifiable"));
		Assert.assertTrue("Body should contain file part",
				bodyText.contains("name=\"file\"; filename=\"" + tempGpxFile.getName() + "\""));
		Assert.assertTrue("Body should contain file content",
				bodyText.contains(sampleGpxContent));
	}

	@Test
	public void testEscapeQuotedValue() {
		Assert.assertEquals("track.gpx", NetworkUtils.escapeQuotedValue("track.gpx"));
		Assert.assertEquals("my %22best%22 track.gpx", NetworkUtils.escapeQuotedValue("my \"best\" track.gpx"));
		Assert.assertEquals("a%0D%0Ab", NetworkUtils.escapeQuotedValue("a\r\nb"));
	}

	@Test
	public void testUploadFileSkipsNullFields() throws Exception {
		AtomicReference<byte[]> capturedBody = new AtomicReference<>();

		server.createContext("/api/0.6/gpx/create_null", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (InputStream is = exchange.getRequestBody()) {
					byte[] buf = new byte[4096];
					int n;
					while ((n = is.read(buf)) != -1) {
						baos.write(buf, 0, n);
					}
				}
				capturedBody.set(baos.toByteArray());

				byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create_null";
		Map<String, String> additionalData = new LinkedHashMap<>();
		additionalData.put("description", null);
		additionalData.put("tags", "");
		additionalData.put("visibility", "private");

		String result = NetworkUtils.uploadFile(url, tempGpxFile, "user:pass", null, "file", false, additionalData);
		Assert.assertNull("Upload should succeed", result);

		String bodyText = new String(capturedBody.get(), StandardCharsets.UTF_8);
		Assert.assertFalse("Null field should be omitted", bodyText.contains("name=\"description\""));
		Assert.assertTrue("Empty field should be sent", bodyText.contains("name=\"tags\"\r\n\r\n\r\n"));
		Assert.assertTrue("Non-null field should be sent",
				bodyText.contains("name=\"visibility\"") && bodyText.contains("private"));
	}

	@Test
	public void testUploadFileGzipBasicAuth() throws Exception {
		AtomicReference<String> capturedContentType = new AtomicReference<>();
		AtomicReference<byte[]> capturedBody = new AtomicReference<>();

		server.createContext("/api/0.6/gpx/create", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (InputStream is = exchange.getRequestBody()) {
					byte[] buf = new byte[4096];
					int n;
					while ((n = is.read(buf)) != -1) {
						baos.write(buf, 0, n);
					}
				}
				capturedBody.set(baos.toByteArray());

				byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create";
		Map<String, String> additionalData = new LinkedHashMap<>();
		additionalData.put("description", "Gzip track");
		additionalData.put("visibility", "trackable");

		String result = NetworkUtils.uploadFile(url, tempGpxFile, null, null, "file", true, additionalData);
		Assert.assertNull("Upload should succeed", result);

		String bodyText = new String(capturedBody.get(), StandardCharsets.ISO_8859_1);
		Assert.assertTrue("Body should contain .gz filename",
				bodyText.contains("filename=\"" + tempGpxFile.getName() + ".gz\""));
		Assert.assertTrue("Body should contain metadata",
				bodyText.contains("name=\"description\"") && bodyText.contains("Gzip track"));
		Assert.assertTrue("Body should contain visibility",
				bodyText.contains("name=\"visibility\"") && bodyText.contains("trackable"));

		String closingBoundary = "\r\n--CowMooCowMooCowCowCow--\r\n";
		Assert.assertTrue("Body should end with closing boundary", bodyText.endsWith(closingBoundary));
		int fileStart = bodyText.indexOf("\r\n\r\n", bodyText.indexOf("filename=")) + 4;
		int fileEnd = bodyText.length() - closingBoundary.length();
		byte[] gzipped = Arrays.copyOfRange(capturedBody.get(), fileStart, fileEnd);
		try (InputStream gis = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
			ByteArrayOutputStream unzipped = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int n;
			while ((n = gis.read(buf)) != -1) {
				unzipped.write(buf, 0, n);
			}
			Assert.assertEquals("Gzipped file should be complete", sampleGpxContent,
					new String(unzipped.toByteArray(), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void testUploadFileOAuthMultipart() throws Exception {
		AtomicReference<String> capturedMethod = new AtomicReference<>();
		AtomicReference<String> capturedQuery = new AtomicReference<>();
		AtomicReference<String> capturedContentType = new AtomicReference<>();
		AtomicReference<String> capturedAuth = new AtomicReference<>();
		AtomicReference<byte[]> capturedBody = new AtomicReference<>();

		server.createContext("/api/0.6/gpx/create", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				capturedMethod.set(exchange.getRequestMethod());
				capturedQuery.set(exchange.getRequestURI().getQuery());
				capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
				capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (InputStream is = exchange.getRequestBody()) {
					byte[] buf = new byte[4096];
					int n;
					while ((n = is.read(buf)) != -1) {
						baos.write(buf, 0, n);
					}
				}
				capturedBody.set(baos.toByteArray());

				byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(
				"test-key", "test-secret", new OsmOAuthAuthorizationClient.OsmApi(),
				"http://127.0.0.1/callback", "write_gpx");
		client.setAccessToken(new OAuth2AccessToken("mock-oauth-token-12345"));

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create";
		Map<String, String> additionalData = new LinkedHashMap<>();
		additionalData.put("description", "OAuth uploaded trace");
		additionalData.put("tags", "hiking, mountain");
		additionalData.put("visibility", "identifiable");

		String result = NetworkUtils.uploadFile(url, tempGpxFile, null, client, "file", false, additionalData);
		Assert.assertNull("Upload should succeed", result);

		Assert.assertEquals("POST", capturedMethod.get());
		Assert.assertNull("URL query parameters should NOT exist in OAuth request", capturedQuery.get());
		Assert.assertTrue("Content-Type should be multipart",
				capturedContentType.get() != null && capturedContentType.get().startsWith("multipart/form-data; boundary="));
		Assert.assertTrue("Authorization should be Bearer token",
				capturedAuth.get() != null && capturedAuth.get().contains("Bearer mock-oauth-token-12345"));

		String bodyText = new String(capturedBody.get(), StandardCharsets.UTF_8);
		Assert.assertTrue("OAuth body must contain description",
				bodyText.contains("name=\"description\"") && bodyText.contains("OAuth uploaded trace"));
		Assert.assertTrue("OAuth body must contain tags",
				bodyText.contains("name=\"tags\"") && bodyText.contains("hiking, mountain"));
		Assert.assertTrue("OAuth body must contain visibility",
				bodyText.contains("name=\"visibility\"") && bodyText.contains("identifiable"));
		Assert.assertTrue("OAuth body must contain file part",
				bodyText.contains("name=\"file\"; filename=\"" + tempGpxFile.getName() + "\""));
		Assert.assertTrue("OAuth body must contain sample GPX content",
				bodyText.contains(sampleGpxContent));
	}

	@Test
	public void testUploadFileOAuthGzip() throws Exception {
		AtomicReference<String> capturedMethod = new AtomicReference<>();
		AtomicReference<String> capturedQuery = new AtomicReference<>();
		AtomicReference<String> capturedContentType = new AtomicReference<>();
		AtomicReference<byte[]> capturedBody = new AtomicReference<>();

		server.createContext("/api/0.6/gpx/create", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				capturedMethod.set(exchange.getRequestMethod());
				capturedQuery.set(exchange.getRequestURI().getQuery());
				capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (InputStream is = exchange.getRequestBody()) {
					byte[] buf = new byte[4096];
					int n;
					while ((n = is.read(buf)) != -1) {
						baos.write(buf, 0, n);
					}
				}
				capturedBody.set(baos.toByteArray());

				byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(
				"test-key", "test-secret", new OsmOAuthAuthorizationClient.OsmApi(),
				"http://127.0.0.1/callback", "write_gpx");
		client.setAccessToken(new OAuth2AccessToken("mock-oauth-token-gzip"));

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create";
		Map<String, String> additionalData = new LinkedHashMap<>();
		additionalData.put("description", "OAuth Gzip Trace");
		additionalData.put("tags", "running");
		additionalData.put("visibility", "trackable");

		String result = NetworkUtils.uploadFile(url, tempGpxFile, null, client, "file", true, additionalData);
		Assert.assertNull("Upload should succeed", result);

		Assert.assertEquals("POST", capturedMethod.get());
		Assert.assertNull("No query string in URL", capturedQuery.get());
		Assert.assertTrue("Content-Type should be multipart",
				capturedContentType.get() != null && capturedContentType.get().startsWith("multipart/form-data; boundary="));

		String bodyText = new String(capturedBody.get(), StandardCharsets.ISO_8859_1);
		Assert.assertTrue("Body should contain .gz filename",
				bodyText.contains("filename=\"" + tempGpxFile.getName() + ".gz\""));
		Assert.assertTrue("Body should contain description",
				bodyText.contains("name=\"description\"") && bodyText.contains("OAuth Gzip Trace"));
		Assert.assertTrue("Body should contain tags",
				bodyText.contains("name=\"tags\"") && bodyText.contains("running"));
		Assert.assertTrue("Body should contain visibility",
				bodyText.contains("name=\"visibility\"") && bodyText.contains("trackable"));
	}

	@Test
	public void testUploadFileServerErrorResponse() throws Exception {
		server.createContext("/api/0.6/gpx/create_error", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "visibility: is not included in the list".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
				exchange.sendResponseHeaders(400, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create_error";
		String result = NetworkUtils.uploadFile(url, tempGpxFile, "user:pass", null, "file", false, null);
		Assert.assertNotNull("Upload should return error", result);
		Assert.assertEquals("visibility: is not included in the list", result);
	}

	@Test
	public void testUploadFileHtmlErrorResponseIsNotReturned() throws Exception {
		server.createContext("/api/0.6/gpx/create_html_error", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "<html><body><h1>502 Bad Gateway</h1></body></html>".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/html");
				exchange.sendResponseHeaders(502, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create_html_error";
		String result = NetworkUtils.uploadFile(url, tempGpxFile, "user:pass", null, "file", false, null);
		Assert.assertEquals("Bad Gateway", result);
	}

	@Test
	public void testUploadFileLongPlainTextErrorIsTruncated() throws Exception {
		StringBuilder longMessage = new StringBuilder();
		for (int i = 0; i < 2000; i++) {
			longMessage.append('x');
		}
		server.createContext("/api/0.6/gpx/create_long_error", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = longMessage.toString().getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(400, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create_long_error";
		String result = NetworkUtils.uploadFile(url, tempGpxFile, "user:pass", null, "file", false, null);
		Assert.assertNotNull(result);
		Assert.assertTrue("Error should be truncated", result.length() <= 503);
		Assert.assertTrue(result.endsWith("..."));
	}

	@Test
	public void testUploadFilePlainTextErrorWithTurkishLocale() throws Exception {
		server.createContext("/api/0.6/gpx/create_tr_error", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "visibility: is not included in the list".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "TEXT/PLAIN");
				exchange.sendResponseHeaders(400, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});

		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("tr", "TR"));
			String url = "http://127.0.0.1:" + serverPort + "/api/0.6/gpx/create_tr_error";
			String result = NetworkUtils.uploadFile(url, tempGpxFile, "user:pass", null, "file", false, null);
			Assert.assertEquals("visibility: is not included in the list", result);
		} finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void testUploadFileErrorWithoutReasonPhraseIsNotSuccess() throws Exception {
		try (ServerSocket rawServer = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
			Thread serverThread = new Thread(() -> {
				try (Socket socket = rawServer.accept()) {
					InputStream in = socket.getInputStream();
					ByteArrayOutputStream request = new ByteArrayOutputStream();
					int b;
					// consume the whole chunked request before answering
					while ((b = in.read()) != -1) {
						request.write(b);
						if (request.toString("ISO-8859-1").endsWith("\r\n0\r\n\r\n")) {
							break;
						}
					}
					OutputStream out = socket.getOutputStream();
					out.write(("HTTP/1.1 502 \r\nContent-Type: text/html\r\nContent-Length: 0\r\n"
							+ "Connection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
					out.flush();
				} catch (IOException ignored) {
				}
			});
			serverThread.start();

			String url = "http://127.0.0.1:" + rawServer.getLocalPort() + "/api/0.6/gpx/create";
			String result = NetworkUtils.uploadFile(url, tempGpxFile, "user:pass", null, "file", false, null);
			serverThread.join(5000);
			Assert.assertEquals("HTTP 502", result);
		}
	}
}
