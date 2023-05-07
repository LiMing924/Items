package liming.texthandle;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public interface GetDataAndPacket {
	void sendDataToClient(Map<String, String> data, InetAddress address, int port, DatagramSocket socket);// 将收到的

	void writeLog(Object message);

	void writeStrongLog(Object message);

	boolean isDataReceived(long timeout);
}
