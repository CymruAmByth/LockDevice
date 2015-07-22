package device;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ServerConnection implements Runnable {

	private String deviceSerialNo;
	private final ByteBuffer buf;
	private String command;

	public ServerConnection() {

		buf = ByteBuffer.allocate(80);
		buf.clear();
		// Set up pingpong
		Timer t = new Timer();
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				command = "Ping";
			}
		}, 60 * 1000, 60 * 1000);
		try {
			final NetworkInterface netInf = NetworkInterface.getNetworkInterfaces().nextElement();
			final byte[] mac = netInf.getHardwareAddress();
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
			}
			deviceSerialNo = sb.toString();
		} catch (SocketException ex) {
			System.out.println(ex.getMessage());
			deviceSerialNo = "ErrorInMac";
		}
	}

	@Override
	public void run() {
		try (SocketChannel s = SocketChannel.open();) {
			s.configureBlocking(false);
			s.connect(new InetSocketAddress("soerendonk.iwa.nu", 44444));
			while (!s.finishConnect()) {
				System.out.println("Connecting");
			}
			System.out.println("Connected");
			command = "Hello";
			while (true) {
				// sending data is a command is there
				if (command != null) {
					buf.clear();
					buf.put(command.getBytes());
					buf.flip();
					while (buf.hasRemaining()) {
						s.write(buf);
					}
					command = null;
				}
				// attempting to receive data
				buf.clear();
				if (s.read(buf) > 0) {
					buf.flip();
					String data = new String(buf.array(), buf.position(), buf.limit());
					data = data.trim();
					System.out.println("Received: " + data + " @ " + new Date().toString());
					switch (data) {
					case "Hello there":
						command = "DEV:" + deviceSerialNo;
						break;
					case "Pong":
						break;
					default:
						System.out.println(data);
						break;
					}
				}
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}
}
