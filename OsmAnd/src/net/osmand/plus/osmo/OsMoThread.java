package net.osmand.plus.osmo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.PlatformUtil;
import net.osmand.plus.osmo.OsMoService.SessionInfo;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class OsMoThread {
//	private static String TRACKER_SERVER = "srv.osmo.mobi";
//	private static int TRACKER_PORT = 3245;

	protected final static Log log = PlatformUtil.getLog(OsMoThread.class);
	private static final long HEARTBEAT_DELAY = 100;
	private static final long HEARTBEAT_FAILED_DELAY = 10000;
	private static final long LIMIT_OF_FAILURES_RECONNECT = 10;
	private static final long SELECT_TIMEOUT = 500;
	private static int HEARTBEAT_MSG = 3;
	private Handler serviceThread;

	private int failures = 0;
	private int activeConnectionId = 0;
	private boolean stopThread;
	private boolean reconnect;
	private Selector selector;

	private List<OsMoSender> listSenders;
	private List<OsMoReactor> listReactors;

	private int authorized = 0; // 1 - send, 2 - authorized
	private OsMoService service;
	private SessionInfo token = null;
	private SocketChannel activeChannel;
	private ByteBuffer pendingSendCommand;
	private String readCommand = "";
	private ByteBuffer pendingReadCommand = ByteBuffer.allocate(2048);
	private LinkedList<String> queueOfMessages = new LinkedList<String>();
	
	
	

	public OsMoThread(OsMoService service, List<OsMoSender> listSenders, List<OsMoReactor> listReactors) {
		this.service = service;
		this.listSenders = listSenders;
		this.listReactors = listReactors;
		// start thread to receive events from OSMO
		HandlerThread h = new HandlerThread("OSMo Service");
		h.start();
		serviceThread = new Handler(h.getLooper());
		serviceThread.post(new Runnable() {

			@Override
			public void run() {
				try {
					initConnection();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		scheduleHeartbeat(HEARTBEAT_DELAY);
	}

	public void stopConnection() {
		stopThread = true;
	}

	protected void initConnection() throws IOException {
		if (token == null) {
			token = service.getSessionToken();
		}
		authorized = 0;
		reconnect = false;
		selector = Selector.open();
		SocketChannel activeChannel = SocketChannel.open();
		activeChannel.configureBlocking(true);
		activeChannel.connect(new InetSocketAddress(token.hostName, Integer.parseInt(token.port)));
		activeChannel.configureBlocking(false);
		SelectionKey key = activeChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		if (this.activeChannel != null) {
			stopChannel();
		}
		this.activeChannel = activeChannel;
		key.attach(new Integer(++activeConnectionId));

	}
	
	public String format(String cmd, Map<String, Object> params) {
		JSONObject json;
		try {
			json = new JSONObject();
			Iterator<Entry<String, Object>> it = params.entrySet().iterator();
			while(it.hasNext()) {
				Entry<String, Object> e = it.next();
				json.put(e.getKey(), e.getValue());
			}
			return cmd + "|"+json.toString();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public void scheduleHeartbeat(long delay) {
		Message msg = serviceThread.obtainMessage();
		msg.what = HEARTBEAT_MSG;
		serviceThread.postDelayed(new Runnable() {
			@Override
			public void run() {
				checkAsyncSocket();
			}
		}, delay);
	}

	public boolean isConnected() {
		return activeChannel != null;
	}

	protected void checkAsyncSocket() {
		long delay = HEARTBEAT_DELAY;
		try {
			if (selector == null) {
				stopThread = true;
			} else {
				if(activeChannel == null || reconnect) {
					initConnection();
				} else {
					checkSelectedKeys();
				}
			}
		} catch (Exception e) {
			log.info("Exception selecting socket", e);
			e.printStackTrace();
			delay = HEARTBEAT_FAILED_DELAY;
			if (activeChannel != null && !activeChannel.isConnected()) {
				activeChannel = null;
			}
			if (failures++ > LIMIT_OF_FAILURES_RECONNECT) {
				reconnect = true;
			}
		}
		if (stopThread) {
			stopChannel();
			serviceThread.getLooper().quit();
		} else {
			scheduleHeartbeat(delay);
		}
	}

	private void stopChannel() {
		if (activeChannel != null) {
			try {
				activeChannel.close();
			} catch (IOException e) {
			}
		}
		activeChannel = null;
	}

	private void checkSelectedKeys() throws IOException {
		/* int s = */selector.select(SELECT_TIMEOUT);
		Set<SelectionKey> keys = selector.selectedKeys();
		if (keys == null) {
			return;
		}
		Iterator<SelectionKey> iterator = keys.iterator();
		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			final boolean isActive = new Integer(activeConnectionId).equals(key.attachment());
			// final boolean isActive = activeChannel == key.channel();
			if (isActive) {
				if (key.isWritable()) {
					writeCommands();
				}
				if (key.isReadable()) {
					readCommands();
				}
			} else {
				try {
					key.channel().close();
				} catch (Exception e) {
					log.info("Exception closing channel", e);
					e.printStackTrace();
				}
			}
			iterator.remove();
		}
	}

	private void readCommands() throws IOException {
		boolean hasSomethingToRead = true;
		while (hasSomethingToRead) {
			pendingReadCommand.clear();
			int read = activeChannel.read(pendingReadCommand);
			if (!pendingReadCommand.hasRemaining()) {
				hasSomethingToRead = true;
			} else {
				hasSomethingToRead = false;
			}
			if(read == -1) {
				reconnect = true;
			} else if (read > 0) {
				byte[] ar = pendingReadCommand.array();
				String res = new String(ar, 0, read);
				readCommand += res;
				int i;
				while ((i = readCommand.indexOf('\n')) != -1) {
					String cmd = readCommand.substring(0, i);
					readCommand = readCommand.substring(i + 1);
					queueOfMessages.add(cmd.replace("\\n", "\n"));
				}
			}
		}

		if (queueOfMessages.size() > 0) {
			processReadMessages();
		}

	}

	private void processReadMessages() {
		while(!queueOfMessages.isEmpty()){
			String cmd = queueOfMessages.poll();
			log.info("OSMO get:"+cmd);
			int k = cmd.indexOf('|');
			String header = cmd;
			String data = "";
			if(k >= 0) {
				header = cmd.substring(0, k);
				data = cmd.substring(k + 1);
			}
			JSONObject obj = null;
			if(data.startsWith("{")) {
				try {
					obj = new JSONObject(data);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			boolean error = false;
			if(obj != null && obj.has("error")) {
				error = true;
				try {
					service.showErrorMessage(obj.getString("error"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			if(header.equalsIgnoreCase("TOKEN")) {
				if(!error){
					authorized = 2;
					// TODO delete
					service.showErrorMessage("OSMo authorization successfull");
				}
				continue;
			}
			boolean processed = false;
			for (OsMoReactor o : listReactors) {
				if (o.acceptCommand(header, data, obj, this)) {
					processed = true;
					break;
				}
			}
			if (!processed) {
				log.warn("Command not processed '" + cmd + "'");
			}
		}
	}

	private void writeCommands() throws UnsupportedEncodingException, IOException {
		if (pendingSendCommand == null) {
			pendingSendCommand = getNewPendingSendCommand();
		}
		while (pendingSendCommand != null) {
			activeChannel.write(pendingSendCommand);
			if (!pendingSendCommand.hasRemaining()) {
				pendingSendCommand = getNewPendingSendCommand();
			} else {
				break;
			}
		}
	}
	
	

	private ByteBuffer getNewPendingSendCommand() throws UnsupportedEncodingException {
		if(authorized == 0) {
			String auth = "TOKEN|"+ token.token;
			log.info("OSMO send:" + auth);
			authorized = 1;
			return ByteBuffer.wrap(prepareCommand(auth).toString().getBytes("UTF-8"));
		}
		if(authorized == 1) {
			return null;
		}
		for (OsMoSender s : listSenders) {
			String l = s.nextSendCommand(this);
			if (l != null) {
				StringBuilder res = prepareCommand(l);
				log.info("OSMO send " + res);
				return ByteBuffer.wrap(res.toString().getBytes("UTF-8"));
			}
		}
		return null;
	}

	private StringBuilder prepareCommand(String l) {
		boolean addNL = true;
		StringBuilder res = new StringBuilder();
		int i = 0;
		while (true) {
			int ni = l.indexOf('\n');
			if (ni == l.length() - 1) {
				res.append(l.substring(i));
				addNL = false;
				break;
			} else if (ni == -1) {
				res.append(l.substring(i));
				break;
			} else {
				res.append(l.substring(i, ni));
				res.append("\\").append("n");
			}
			i = ni + 1;
		}
		if (addNL) {
			l += "\n";
		}
		return res;
	}
}
