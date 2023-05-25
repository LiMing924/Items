package liming.texthandle;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 用于处理UDP数据包的接收和发送
 */
public abstract class HandleReceive implements GetDataAndPacket {
	private final List<DatagramSocket> sockets; // 所有的DatagramSocket
	private ExecutorService sendPool; // 发送线程池
	private ExecutorService receivePool; // 接收线程池
	private int port = 64651; // 默认端口号
	private int dataSize = 512; // 默认接收区大小
	private static boolean debug = false; // 默认关闭调试模式
	private static int sendSpeed = 10; // 默认发送端处理速度
	private int handlesize;// bufferPool成员的个数
	private int handleDatasize_max = 500;
	private Unit handleDatasize_maxUnit = Unit.MB;
	private BlockingQueue<DatagramPacket> bufferPool;
	private boolean running = false;
	// private boolean init=false;

	private Text text;

	public Text getText() {
		return text;
	}

	public HandleReceive() {
		sockets = new ArrayList<>();
	}

	/**
	 * 构造函数，设置回调接口、端口号、接收区大小
	 *
	 * @throws SocketException
	 */
	public HandleReceive(int port, int dataSize) throws SocketException {
		this.port = port;
		this.dataSize = dataSize;
		this.sockets = new ArrayList<>();
		DatagramSocket socket = new DatagramSocket(port);
		this.sockets.add(socket);
	}

	/**
	 * 构造函数，设置回调接口、DatagramSocket
	 */
	public HandleReceive(DatagramSocket... sockets) {
		this.sockets = new ArrayList<>();
		for (DatagramSocket socket : sockets)
			this.sockets.add(socket);

	}

	/**
	 * 构造函数，设置回调接口、端口号、接收区大小、端口号范围
	 *
	 * @throws SocketException
	 */
	public HandleReceive(int port, int dataSize, int minPort, int maxPort)
			throws SocketException {
		this.dataSize = dataSize;
		this.sockets = new ArrayList<>();
		try {
			DatagramSocket socket = new DatagramSocket(port);
			this.port = port;
			this.sockets.add(socket);
		} catch (SocketException e) {
			writeLog("端口" + port + "被占用");
			int i = 0;
			for (i = minPort; i <= maxPort; i++) {
				try {
					DatagramSocket socket = new DatagramSocket(i);
					this.port = i;
					this.sockets.add(socket);
					break;
				} catch (SocketException e1) {
					writeLog("端口" + i + "被占用");
				}
			}
			if (i > maxPort)
				throw new SocketException("在设置范围port=" + port + ",[" + minPort + "," + maxPort + "]无端口可用");
		}
	}

	/**
	 * 添加端口号
	 *
	 * @param socket
	 */

	public void addSocket(DatagramSocket socket) {
		if (sockets.size() == 0 && port == 64651) {
			setPort(socket.getLocalPort());
		}
		sockets.add(socket);
	}

	/**
	 * 获取DatagramSocket列表
	 */
	public List<DatagramSocket> getSockets() {
		return sockets;
	}

	/**
	 * 获取 debug 状态
	 */
	public boolean getDebug() {
		return debug;
	}

	public int getDataSize() {
		return dataSize - 162;
	}

	/**
	 * 设置端口号
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public void setDataSizeMax(int size, Unit unit) {
		handleDatasize_max = size;
		handleDatasize_maxUnit = unit;
	}

	/**
	 * 设置接收区大小
	 */
	public void setDataSize(int dataSize) {
		int temp = this.dataSize;
		if (text != null) {
			this.dataSize = text.setDataSize(dataSize);
		} else
			this.dataSize = dataSize;
		writeLog("DataSize发生改变: " + temp + " -> " + this.dataSize);
	}

	/**
	 * 设置发送端处理速度
	 */
	public static void setSendSpeed(int sendSpeed) {
		HandleReceive.sendSpeed = sendSpeed;
	}

	/**
	 * 打开调试模式
	 */
	public static void setDebug(boolean debug) {
		HandleReceive.debug = debug;
		Text.DEBUG = debug;
	}

	/**
	 * 启动接收和发送线程池
	 *
	 * @throws Exception
	 */
	public synchronized final void start() {
		if (sockets.size() < 1) {
			// if (port != 0) {
			try {
				addSocket(new DatagramSocket(port));
			} catch (Exception e) {
				writeStrongLog(FileRW.getError(e));
				return;
			}
			// } else {
			// writeStrongLog(FileRW.getError(new Exception("未指定socket,启动失败")));
			// return;
			// }
		}
		if (running) {
			writeStrongLog(new Exception("已在运行中,启动失败"));
			return;
		}
		running = true;
		text = new Text(this, sockets.size());
		setDataSize(dataSize);
		writeLog("发送线程池设置值=" + sendSpeed + ",处理线程池中等待加入的sockets个数为=" + sockets.size());
		sendPool = Executors.newFixedThreadPool(sendSpeed); // 初始化发送线程池

		handlesize = (int) Math.ceil((double) handleDatasize_max * handleDatasize_maxUnit.getValue() / dataSize);
		bufferPool = new ArrayBlockingQueue<>(handlesize);
		handlesize = Math.min(handlesize, 20000);
		for (int i = 0; i < handlesize; i++) {
			bufferPool.offer(new DatagramPacket(new byte[dataSize], dataSize));
		}
		receivePool = Executors.newFixedThreadPool(sockets.size() * 2); // 初始化接收线程池
		receivePool.execute(() -> {
			for (DatagramSocket socket : sockets) {
				receivePool.execute(() -> {
					while (running) {
						try {
							DatagramPacket packet = bufferPool.take();
							packet.setData(new byte[dataSize], 0, dataSize);
							socket.receive(packet);
							writeLog(
									socket.getLocalPort() + "接收数据" + packet.getAddress() + ":" + packet.getPort());
							receivePool.execute(() -> {
								try {
									ReceiveMap map = text.Handle(packet);
									if (map != null) {
										map.setIP(packet.getAddress(), packet.getPort());
										sendDataToClient(map, packet.getAddress(), packet.getPort(), socket);
									}
								} catch (Exception e) {
									writeStrongLog(socket.getLocalPort() + " 处理中断 " + FileRW.getError(e));
								}
							});
							bufferPool.offer(packet);
						} catch (IOException | InterruptedException e) {
							writeStrongLog("接收端关闭 " + e.getMessage());
							break;
						}
					}
				});
				writeLog("端口" + socket.getLocalPort() + "的socket加入线程池中");
			}
		});
		writeStrongLog("数据处理端启动成功");
	}

	/**
	 * 关闭接收和发送线程池
	 */
	public synchronized final void stop() {
		if (!running) {
			writeStrongLog(new Exception("未启动,停止失败"));
		}
		running = false;
		for (DatagramSocket socket : sockets) {
			socket.close();
		}
		sendPool.shutdown();
		receivePool.shutdown();
		try {
			sendPool.awaitTermination(1, TimeUnit.SECONDS);
			receivePool.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		sendPool.shutdownNow();
		receivePool.shutdownNow();
		if (sendPool.isShutdown() && sendPool.isTerminated())
			writeLog("发送池结束");
		if (receivePool.isShutdown() && receivePool.isTerminated())
			writeLog("处理池结束");
		bufferPool.clear();
		text.clear();
		writeStrongLog("数据处理端结束成功");
	}

	public final String getPools() {
		return "\n\treceivePool:" + receivePool + "\n\tsendPool" + sendPool;
	}

	public final boolean send(ReceiveMap map) throws ExecutionException, InterruptedException {
		if (sockets.size() == 0)
			throw new InterruptedException("没有可用的端口发送数据");
		return send(map, sockets.get(0));
	}

	public final boolean send(ReceiveMap map, DatagramSocket socket) throws ExecutionException, InterruptedException {
		if (map.isIP()) {
			return send(map, map.getInetAddress(), map.getPort(), socket);
		} else {
			throw new InterruptedException("发送包未目标设置ip");
		}
	}

	/**
	 * 发送数据包，使用指定的IP和端口号和第一个Datagram端口号和第一个DatagramSocket
	 */
	public final boolean send(ReceiveMap receiveMap, String ip, int port)
			throws InterruptedException, ExecutionException, UnknownHostException {
		if (sockets.size() == 0)
			throw new InterruptedException("没有可用的端口发送数据");
		return send(receiveMap, ip, port, sockets.get(0));

	}

	/**
	 * 发送数据包，使用指定的DatagramSocket、IP和端口号
	 */
	public final boolean send(ReceiveMap receiveMap, String ip, int port, DatagramSocket socket)
			throws InterruptedException, ExecutionException, UnknownHostException {
		return send(receiveMap, InetAddress.getByName(ip), port, socket);
	}

	public final boolean send(ReceiveMap receiveMap, InetAddress address, int port)
			throws InterruptedException, ExecutionException {
		if (sockets.size() == 0)
			throw new InterruptedException("没有可用的端口发送数据");
		else
			return send(receiveMap, address, port, sockets.get(0));
	}

	public final boolean send(ReceiveMap receiveMap, InetAddress address, int port, DatagramSocket socket)
			throws InterruptedException, ExecutionException {
		return sendPool.submit(() -> {
			try {
				boolean state = text.Send(receiveMap, address, port, socket);
				return state;
			} catch (IOException e) {
				writeLog(FileRW.getError(e));
				return false;
			}
		}).get();
	}

	@Override
	public String toString() {
		return "HandleReceive [debug=" + debug + ", sockets=" + sockets + ", sendPool=" + sendPool + ", receivePool="
				+ receivePool + ", port=" + port + ", dataSize=" + dataSize + "]";
	}

	// 获取处理服务器所有状态
	public final JSONObject getState() {
		JSONObject root = new JSONObject();
		root.put("Sockets", getStateSockets());
		root.put("pool", getStatereceivePool());
		root.put("info", getStateServerInfo());
		root.put("text", getStateText());
		return root;
	}

	// 获取处理服务器DatagramSocket的状态
	public final JSONArray getStateSockets() {
		JSONArray root = new JSONArray();
		for (DatagramSocket datagramSocket : sockets) {
			JSONObject object = new JSONObject();
			object.put("localAddress", datagramSocket.getLocalAddress().toString());
			object.put("localPort", datagramSocket.getLocalPort());
			root.put(object);
		}
		return root;
	}

	// 获取处理服务器DatagramSocket线程处理池的状态
	public final JSONObject getStatereceivePool() {
		JSONObject root = new JSONObject();
		root.put("send", getPool(sendPool));
		root.put("receive", getPool(receivePool));
		return root;
	}

	// 获取处理服务器基本属性
	public final JSONObject getStateServerInfo() {
		JSONObject object = new JSONObject();
		object.put("port", port);
		object.put("size", dataSize);
		object.put("handlesize", handlesize);
		object.put("debug", debug);
		object.put("sendSpeed", sendSpeed);
		return object;
	}

	// 获取处理服务器Text状态信息
	public final JSONObject getStateText() {
		if (text == null) {
			JSONObject root = new JSONObject();
			root.put("text", "服务器未启动，无法获取信息");
			return root;
		}
		return text.getState();
	}

	private final JSONObject getPool(ExecutorService service) {
		JSONObject root = new JSONObject();
		ThreadPoolExecutor executor = (ThreadPoolExecutor) service;
		root.put("poolSize", executor.getPoolSize());
		root.put("activeCount", executor.getActiveCount());
		root.put("completedTaskCount", executor.getCompletedTaskCount());
		root.put("taskCount", executor.getTaskCount());
		root.put("isShutdown", executor.isShutdown());
		root.put("isTerminated", executor.isTerminated());
		return root;
	}

	@Override
	public abstract void sendDataToClient(ReceiveMap data, InetAddress address, int port, DatagramSocket socket)
			throws InterruptedException, ExecutionException, UnknownHostException;

	// @Override
	// public abstract void sendDataToClient(ReceiveMap data, InetAddress address,
	// int port, DatagramSocket socket);

	@Override
	public void writeLog(Object message) {
		if (debug)
			System.out.println("writeLog: " + message);
	}

	@Override
	public void writeStrongLog(Object message) {
		System.out.println("writeStrongLog: " + message);

	}

	@Override
	public abstract boolean isDataReceived(long timeout);
}
