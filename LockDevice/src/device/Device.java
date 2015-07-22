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

public class Device {
	
	private static String deviceSerialNo;
	private static ByteBuffer buf;
	private static String command;
	private static long lastPong;
	private static boolean run;
	private static int attempts;
	private static Timer t;
	private static TimerTask pingPongTask;


	public static void main(String[] args) {
		buf = ByteBuffer.allocate(80);
		buf.clear();
		attempts = 1;
		
		deviceSerialNo = getMac();
		setUpPingPong();
		while(true){
			System.out.println("Attempt: " + attempts);
			setUpConnection();			
			attempts++;
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			//check for failed attempts and undertake action
		}
	}
	
	private static String getMac(){
		try {
			final NetworkInterface netInf = NetworkInterface.getNetworkInterfaces().nextElement();
			final byte[] mac = netInf.getHardwareAddress();
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
			}
			return sb.toString();
		} catch (SocketException ex) {
			System.out.println(ex.getMessage());
			return "ErrorInMac";
		}
	}
	
	private static void setUpPingPong(){
		pingPongTask = new TimerTask() {
			
			@Override
			public void run() {
				long current = System.currentTimeMillis();
				long delay = current-lastPong;
				if(delay < 70*1000){
					command = "Ping";
				} else {
					System.out.println("Connection lost, ms since last pong: " + delay);
					breakConnection();
				}
				
			}
		};
		//schedule pingpong
		t = new Timer();
		t.schedule(pingPongTask, 1000, 60 * 1000);	
	}
	
	private static void breakConnection() {
		run = false;
	}

	private static void setUpConnection(){
		try (SocketChannel s = SocketChannel.open();) {
			//set up socket
			s.configureBlocking(false);
			s.connect(new InetSocketAddress("soerendonk.iwa.nu", 44444));
			
			//connect to socket
			while (!s.finishConnect()) {
				//System.out.println("Connecting");
			}
			
			//reset connection attempts
			System.out.println("Connected");
			attempts = 0;
			
			//set up initial run
			lastPong = System.currentTimeMillis();
			command = "Hello";
			run = true;
						
			//keep running until breakConnection is called
			while (run) {
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
						lastPong = System.currentTimeMillis();
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
