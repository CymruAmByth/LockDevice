package device;

public class Device {

	public static void main(String[] args) {
        ServerConnection conn = new ServerConnection();
        Thread t = new Thread(conn);
        t.start();
	}

}
