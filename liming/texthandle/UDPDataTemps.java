package liming.texthandle;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

class UDPDataTemps {
	private Map<String, UDPData> Datas = new HashMap<>();
	private int SIZE = 5;// 默认线程池个数
	private ExecutorService[] executors;
	private UDP_Interface interface1;
	private Map<String, Integer> valuesRun;// 关键字所在的处理线程池序号
	private int[] valuesWait; // 线程池待处理的关键字
	private boolean running;
	private Thread timeOutThread;

	private long timeOutAll = 100 * 1000, timeout = 5 * 1000;

	public UDPDataTemps(UDP_Interface interface1) {
		this(interface1, 5);

	}

	public UDPDataTemps(UDP_Interface interface1, int size) {
		this.interface1 = interface1;
		this.SIZE = size;
		executors = new ExecutorService[SIZE];
		valuesRun = new ConcurrentHashMap<>();
		valuesWait = new int[SIZE];
		for (int i = 0; i < SIZE; i++) {
			executors[i] = Executors.newFixedThreadPool(1);
			valuesWait[i] = 0;
		}
		running = true;
		timeOutThread = new Thread(new TimeOutRunnble());
		timeOutThread.start();
	}

	private class TimeOutRunnble implements Runnable {
		@Override
		public void run() {
			while (running) {
				synchronized (Datas) {
					for (String key : Datas.keySet()) {
						if (Datas.get(key).isTimeOut(timeOutAll, timeout)) {
							UDPData data = Datas.remove(key);
							interface1.udp_slog("接收超时 " + data.getID() + " " + new Date(data.getStartTime()) + " "
									+ new Date(data.getLastTime()) + " " + data.getGiatus());
						}
					}
					try {
						Datas.wait(timeout / 2);
					} catch (Exception e) {
						interface1.udp_log(FileRW.getError(e));
					}
				}
			}
		}
	}

	/**
	 * 添加数据
	 * 
	 * @param object
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public Map<String, String> get(JSONObject object) throws InterruptedException, ExecutionException {
		text("正在处理数据，map：", object);
		text(Datas.keySet(), "添加数据=", object);
		UDPData data = new UDPData(interface1, object);
		if (data.value()) {// 判断单个数据是否接收完,若接收完就直接返回结果
			return data.getData();
		}
		String key = data.getID();

		int i = -1;

		/* 获取当前key在缓冲列表位置，若存在则加入，若不存在则计算最小的那个缓冲位置 */
		synchronized (valuesRun) {
			if (valuesRun.containsKey(key)) {
				i = valuesRun.get(key).intValue();
			} else {
				i = 0;
				for (int n = 0; n < SIZE; n++) {
					if (valuesWait[i] > valuesWait[n])
						i = n;
				}
			}
			valuesWait[i]++;
		}
		/* 计算结束 */

		final int x = i;
		return executors[x].submit(() -> {
			try {
				UDPData d = putData(key, data);
				if (d != null && d.value()) {
					valuesRun.remove(key);
					return d.getData();
				} else
					return null;
			} finally {
				valuesWait[x]--;
			}
		}).get();
	}

	private UDPData putData(String key, UDPData new_data) {
		long start = System.currentTimeMillis();
		synchronized (Datas) {
			if (Datas.containsKey(key)) {
				UDPData data = Datas.remove(key);
				data.add(new_data);
				if (data.value()) {
					return data;
				} else
					Datas.put(key, data);
			} else {
				System.out.println("未有记录");
				Datas.put(key, new_data);
			}
		}
		long end = System.currentTimeMillis();
		System.out.print("\r" + (end - start) + "\t");
		return null;
	}

	public void clear() {
		running = false;
		timeOutThread.interrupt();
		Datas.clear();
		int i = 1;
		for (ExecutorService executor : executors) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
			}
			if (executor.isShutdown() && executor.isTerminated()) {
				text(i++ + "已关闭");
			} else {
				text(i++ + "未关闭");
			}
		}
		text_Strong("数据处理结束");
	}

	private void text(Object... objects) {
		String str = "UDPTemps:{";
		for (Object object : objects) {
			str += " " + object.toString();
		}
		interface1.udp_log(str + "}");
	}

	private void text_Strong(Object... objects) {
		String str = "UDPTemps:{";
		for (Object object : objects) {
			str += " " + object.toString();
		}
		interface1.udp_slog(str + "}");
	}

	public JSONObject getState() {
		JSONObject root = new JSONObject();
		root.put("pool", getStatePool());
		root.put("info", getBaseInfo());
		return root;
	}

	public JSONObject getBaseInfo() {
		JSONObject root = new JSONObject();
		root.put("size", SIZE);
		return root;
	}

	private JSONObject getStatePool() {
		JSONObject root = new JSONObject();
		{
			ExecutorService[] es = executors.clone();
			JSONArray v = new JSONArray();
			for (int i = 0; i < SIZE; i++) {
				ThreadPoolExecutor executor = (ThreadPoolExecutor) es[i];
				JSONObject o = new JSONObject();
				o.put("num", i);
				// o.put("handle", vs[i] == null ? "null" : vs[i]);// 正在处理的key
				o.put("wait", executor.getQueue().size());// 等待个数
				o.put("complete", executor.getCompletedTaskCount());// 已完成个数
				o.put("running ", executor.getActiveCount());// 正在运行个数
				v.put(o);
			}
			root.put("values", v);
		}
		return root;
	}
}