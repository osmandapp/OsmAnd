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
import java.util.Set;

import net.osmand.PlatformUtil;
import net.osmand.plus.osmo.OsMoService.OsMoReactor;
import net.osmand.plus.osmo.OsMoService.OsMoSender;

import org.apache.commons.logging.Log;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class OsMoThread {
	private static String TRACKER_SERVER = "srv.osmo.mobi";
	private static int TRACKER_PORT = 4242;

	protected final static Log log = PlatformUtil.getLog(OsMoThread.class);
	private static final long HEARTBEAT_DELAY = 100;
	private static final long HEARTBEAT_FAILED_DELAY = 10000;
	private static final long LIMIT_OF_FAILURES_RECONNECT = 10;
	private static final long CONNECTION_DELAY = 25000;
	private static final long SELECT_TIMEOUT = 500;
	private static int HEARTBEAT_MSG = 3;
	private Handler serviceThread;

	private int failures = 0;
	private int activeConnectionId = 0;
	// -1 means connected, 0 needs to reconnect, > 0 when connection initiated
	private long connectionStarted = 0;
	private boolean stopThread;
	private Selector selector;

	private List<OsMoSender> listSenders;
	private List<OsMoReactor> listReactors;

	private SocketChannel activeChannel;
	private ByteBuffer pendingSendCommand;
	private String readCommand = "";
	private ByteBuffer pendingReadCommand = ByteBuffer.allocate(2048);
	private LinkedList<String> queueOfMessages = new LinkedList<String>();

	public OsMoThread(List<OsMoSender> listSenders, List<OsMoReactor> listReactors) {
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
		try {
			selector = Selector.open();
			connectionStarted = System.currentTimeMillis();
			activeChannel = SocketChannel.open();
			activeChannel.configureBlocking(false);
			SelectionKey key = activeChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			key.attach(new Integer(++activeConnectionId));
			activeChannel.connect(new InetSocketAddress(TRACKER_SERVER, TRACKER_PORT));
		} catch (IOException e) {
			throw e;
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
		return connectionStarted == -1;
	}

	protected void checkAsyncSocket() {
		long delay = HEARTBEAT_DELAY;
		try {
			if (selector == null) {
				stopThread = true;
			} else {
				if (activeChannel != null && connectionStarted != -1 && !activeChannel.isConnectionPending()) {
					// connection ready
					connectionStarted = -1;
				}
				if ((connectionStarted != -1 && System.currentTimeMillis() - connectionStarted > CONNECTION_DELAY)
						|| activeChannel == null) {
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
				stopChannel();
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
			if (pendingReadCommand.hasRemaining()) {
				hasSomethingToRead = true;
			} else {
				hasSomethingToRead = false;
			}
			if (read > 0) {
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
			while(!queueOfMessages.isEmpty()){
				String cmd = queueOfMessages.poll();
				boolean processed = false;
				for (OsMoReactor o : listReactors) {
					if (o.acceptCommand(cmd)) {
						processed = true;
						break;
					}
				}
				if (!processed) {
					log.warn("Command not processed '" + cmd + "'");
				}
			}
		}

	}

	private void writeCommands() throws UnsupportedEncodingException, IOException {
		if (pendingSendCommand == null) {
			getNewPendingSendCommand();
		}
		while (pendingSendCommand != null) {
			activeChannel.write(pendingSendCommand);
			if (!pendingSendCommand.hasRemaining()) {
				pendingSendCommand = null;
				getNewPendingSendCommand();
			}
		}
	}

	private void getNewPendingSendCommand() throws UnsupportedEncodingException {
		for (OsMoSender s : listSenders) {
			String l = s.nextSendCommand();
			if (l != null) {
				StringBuilder res = prepareCommand(l);
				pendingSendCommand = ByteBuffer.wrap(res.toString().getBytes("UTF-8"));
				break;
			}
		}
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