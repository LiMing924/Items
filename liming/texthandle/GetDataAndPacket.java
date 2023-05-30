package liming.texthandle;

import java.net.DatagramSocket;

/**
 * 该接口定义了获取数据和处理数据包的方法
 */
public interface GetDataAndPacket {
	/**
	 * 处理收到的数据，并发送给客户端
	 * 
	 * @param data   数据
	 * @param socket 数据包套接字
	 */
	void sendDataToClient(ReceiveMap data, DatagramSocket socket) throws Exception;

	/**
	 * 写入日志信息
	 * 
	 * @param message 日志信息
	 */
	void writeLog(Object message);

	/**
	 * 写入强调的日志信息
	 * 
	 * @param message 强调的日志信息
	 */
	void writeStrongLog(Object message);

	/**
	 * 检查是否已经接收到数据
	 * 
	 * @param timeout 超时时间
	 * @return 如果收到数据则返回true，否则返回false
	 */
	boolean isDataReceived(long timeout);

}
