package liming.texthandle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
public class HandleReceive {
	private final GetDataAndPacket gdap; // 回调接口
	private final List<DatagramSocket> sockets; // 所有的DatagramSocket
	private ExecutorService sendPool; // 发送线程池
	private ExecutorService receivePool; // 接收线程池
	private int port = 24651; // 默认端口号
	private int dataSize = 512; // 默认接收区大小
	private static boolean debug = false; // 默认关闭调试模式
	private static int sendSpeed = 10; // 默认发送端处理速度
	private int handlesize;// bufferPool成员的个数
	private int handleDatasize_max = 500;
	private Unit handleDatasize_maxUnit = Unit.MB;
	private BlockingQueue<DatagramPacket> bufferPool;
	private boolean running = false;

	private Text text;

	public Text getText() {
		return text;
	}

	public HandleReceive(GetDataAndPacket gdap) {
		this.gdap = gdap;
		sockets = new ArrayList<>();
	}

	/**
	 * 构造函数，设置回调接口、端口号、接收区大小
	 * 
	 * @throws SocketException
	 */
	public HandleReceive(GetDataAndPacket gdap, int port, int dataSize) throws SocketException {
		this.gdap = gdap;
		this.port = port;
		this.dataSize = dataSize;
		this.sockets = new ArrayList<>();
		DatagramSocket socket = new DatagramSocket(port);
		this.sockets.add(socket);
	}

	/**
	 * 构造函数，设置回调接口、DatagramSocket
	 */
	public HandleReceive(GetDataAndPacket gdap, DatagramSocket... sockets) {
		this.gdap = gdap;
		this.sockets = new ArrayList<>();
		for (DatagramSocket socket : sockets)
			this.sockets.add(socket);

	}

	/**
	 * 构造函数，设置回调接口、端口号、接收区大小、端口号范围
	 * 
	 * @throws SocketException
	 */
	public HandleReceive(GetDataAndPacket gdap, int port, int dataSize, int minPort, int maxPort)
			throws SocketException {
		this.gdap = gdap;
		this.dataSize = dataSize;
		this.sockets = new ArrayList<>();
		try {
			DatagramSocket socket = new DatagramSocket(port);
			this.port = port;
			this.sockets.add(socket);
		} catch (SocketException e) {
			gdap.writeLog("端口" + port + "被占用");
			int i = 0;
			for (i = minPort; i <= maxPort; i++) {
				try {
					DatagramSocket socket = new DatagramSocket(i);
					this.port = i;
					this.sockets.add(socket);
					break;
				} catch (SocketException e1) {
					gdap.writeLog("端口" + i + "被占用");
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
		if (sockets.size() < 1) {
			setPort(socket.getLocalPort());
		}
		sockets.add(socket);
		try {
			if (socket.getReceiveBufferSize() > dataSize) {
				int size = dataSize;
				setDataSize(socket.getReceiveBufferSize());
				gdap.writeLog("datasize发生改变：" + size + " -> " + dataSize);
			}
		} catch (Exception e) {
		}
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

	public void setDataSizeMax(int size, Unit unit) {
		handleDatasize_max = size;
		handleDatasize_maxUnit = unit;
	}

	/**
	 * 设置接收区大小
	 */
	public void setDataSize(int dataSize) {
		if (text != null) {
			this.dataSize = text.setDataSize(dataSize);
		} else
			this.dataSize = dataSize;
		gdap.writeLog("DataSize发生改变: " + dataSize + " -> " + this.dataSize);
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
	public void start() {
		if (sockets.size() < 1) {
			gdap.writeStrongLog(FileRW.getError(new Exception("未指定socket,启动失败")));
			return;
		}
		if (running) {
			gdap.writeStrongLog(new Exception("已在运行中,启动失败"));
			return;
		}
		running = true;
		text = new Text(gdap, sockets.size());
		setDataSize(dataSize);
		gdap.writeLog("发送线程池设置值=" + sendSpeed + ",处理线程池中等待加入的sockets个数为=" + sockets.size());
		sendPool = Executors.newFixedThreadPool(sendSpeed); // 初始化发送线程池

		handlesize = (int) Math.ceil((double) handleDatasize_max * handleDatasize_maxUnit.getValue() / dataSize);
		bufferPool = new ArrayBlockingQueue<>(handlesize);
		for (int i = 0; i < handlesize; i++) {
			bufferPool.offer(new DatagramPacket(new byte[dataSize], dataSize));
		}
		receivePool = Executors.newFixedThreadPool(sockets.size() + 1); // 初始化接收线程池
		receivePool.execute(() -> {
			for (DatagramSocket socket : sockets) {
				receivePool.execute(() -> {
					while (running) {
						try {
							DatagramPacket packet = bufferPool.take();
							packet.setData(new byte[dataSize], 0, dataSize);
							socket.receive(packet);
							gdap.writeLog(
									socket.getLocalPort() + "接收数据" + packet.getAddress() + ":" + packet.getPort());
							receivePool.execute(() -> {
								try {
									ReceiveMap map = text.Handle(packet);
									if (map != null) {
										gdap.sendDataToClient(map, packet.getAddress(), packet.getPort(), socket);
									}
								} catch (Exception e) {
									gdap.writeStrongLog(socket.getLocalPort() + " 处理中断 " + FileRW.getError(e));
								}
							});
							bufferPool.offer(packet);
						} catch (IOException | InterruptedException e) {
							gdap.writeStrongLog("接收端关闭 " + e);
							break;
						}
					}
				});
				gdap.writeLog("端口" + socket.getLocalPort() + "的socket加入线程池中");
			}
		});
		gdap.writeStrongLog("数据处理端启动成功");
	}

	/**
	 * 关闭接收和发送线程池
	 */
	public void stop() {
		if (!running) {
			gdap.writeStrongLog(new Exception("未启动,停止失败"));
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
			sendPool.shutdownNow();
			receivePool.shutdownNow();
		}
		if (sendPool.isShutdown() && sendPool.isTerminated())
			gdap.writeLog("发送池结束");
		if (receivePool.isShutdown() && receivePool.isTerminated())
			gdap.writeLog("处理池结束");
		bufferPool.clear();
		text.clear();
		gdap.writeStrongLog("数据处理端结束成功");
	}

	/**
	 * 发送数据包，使用指定的DatagramSocket、IP和端口号
	 */
	public boolean send(ReceiveMap receiveMap, String ip, int port, DatagramSocket socket)
			throws InterruptedException, ExecutionException {
		return sendPool.submit(() -> {
			try {
				boolean state = text.Send(receiveMap, InetAddress.getByName(ip), port, socket);
				return state;
			} catch (NullPointerException e) {
				gdap.writeStrongLog("服务器未启动");
			} catch (IOException e) {
				gdap.writeLog(e);
			}
			return false;
		}).get();
	}

	public String getPools() {
		return "\n\treceivePool:" + receivePool + "\n\tsendPool" + sendPool;
	}

	/**
	 * 发送数据包，使用指定的IP和端口号和第一个Datagram端口号和第一个DatagramSocket
	 */
	public boolean send(ReceiveMap receiveMap, String ip, int port) throws InterruptedException, ExecutionException {
		if (sockets.size() > 0) {
			return send(receiveMap, ip, port, sockets.get(0));
		} else {
			try (DatagramSocket socket = new DatagramSocket(port)) {
				return send(receiveMap, ip, port, socket);
			} catch (Exception e) {
				gdap.writeLog(e);
				return false;
			}

			// return sendPool.submit(() -> {
			// try (DatagramSocket socket = new DatagramSocket(port)) {
			// return text.Send(receiveMap, InetAddress.getByName(ip), port, socket);
			// } catch (IOException e) {
			// gdap.writeLog(e);
			// return false;
			// }
			// }).get();
		}
	}

	public boolean send(ReceiveMap receiveMap, InetAddress address, int port, DatagramSocket socket)
			throws InterruptedException, ExecutionException {
		return sendPool.submit(() -> {
			try {
				boolean state = text.Send(receiveMap, address, port, socket);
				return state;
			} catch (IOException e) {
				gdap.writeLog(FileRW.getError(e));
				return false;
			}
		}).get();
	}

	/**
	 * 发送数据包，使用指定的DatagramSocket
	 */
	public boolean send(ReceiveMap receiveMap, DatagramPacket packet, DatagramSocket socket)
			throws InterruptedException, ExecutionException {
		return sendPool.submit(() -> {
			try {
				boolean state = text.Send(receiveMap, packet, socket);
				return state;
			} catch (IOException e) {
				gdap.writeLog(FileRW.getError(e));
				return false;
			}
		}).get();
	}

	/**
	 * 发送数据包，使用第一个DatagramSocket
	 */
	public boolean send(ReceiveMap receiveMap, DatagramPacket packet)
			throws UnsupportedEncodingException, InterruptedException, ExecutionException {
		if (sockets.size() > 0) {
			return send(receiveMap, packet, sockets.get(0));
		} else {
			return sendPool.submit(() -> {
				try (DatagramSocket socket = new DatagramSocket(port)) {
					return text.Send(receiveMap, packet, socket);
				} catch (IOException e) {
					gdap.writeLog(e);
					return false;
				}
			}).get();
		}
	}

	@Override
	public String toString() {
		return "HandleReceive [debug=" + debug + ", sockets=" + sockets + ", sendPool=" + sendPool + ", receivePool="
				+ receivePool + ", port=" + port + ", dataSize=" + dataSize + "]";
	}

	// 获取处理服务器所有状态
	public JSONObject getState() {
		JSONObject root = new JSONObject();
		root.put("Sockets", getStateSockets());
		root.put("pool", getStatereceivePool());
		root.put("info", getStateServerInfo());
		root.put("text", getStateText());
		return root;
	}

	// 获取处理服务器DatagramSocket的状态
	public JSONArray getStateSockets() {
		JSONArray root = new JSONArray();
		for (int i = 0; i < sockets.size(); i++) {
			JSONObject object = new JSONObject();
			DatagramSocket socket = sockets.get(i);
			object.put("localAddress", socket.getLocalAddress().toString());
			object.put("localPort", socket.getLocalPort());
			root.put(object);
		}
		return root;
	}

	// 获取处理服务器DatagramSocket线程处理池的状态
	public JSONObject getStatereceivePool() {
		JSONObject root = new JSONObject();
		root.put("send", getPool(sendPool));
		root.put("receive", getPool(receivePool));
		return root;
	}

	// 获取处理服务器基本属性
	public JSONObject getStateServerInfo() {
		JSONObject object = new JSONObject();
		object.put("port", port);
		object.put("size", dataSize);
		object.put("handlesize", handlesize);
		object.put("debug", debug);
		object.put("sendSpeed", sendSpeed);
		return object;
	}

	// 获取处理服务器Text状态信息
	public JSONObject getStateText() {
		if (text == null) {
			JSONObject root = new JSONObject();
			root.put("text", "服务器未启动，无法获取信息");
			return root;
		}
		return text.getState();
	}

	private JSONObject getPool(ExecutorService service) {
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
}
