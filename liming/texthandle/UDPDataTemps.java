package liming.texthandle;

import java.util.HashMap;
import java.util.Map;
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
	private String[] values;// 正在处理的参数列表
	private Object lock;

	public UDPDataTemps(UDP_Interface interface1) {
		this(interface1, 5);

	}

	public UDPDataTemps(UDP_Interface interface1, int size) {
		this.interface1 = interface1;
		this.SIZE = size;
		executors = new ExecutorService[SIZE];
		values = new String[SIZE];
		for (int i = 0; i < SIZE; i++) {
			executors[i] = Executors.newFixedThreadPool(1);
		}
		lock = new Object();
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
		int i;
		synchronized (lock) {
			for (i = 0; i < SIZE; i++) {
				if (values[i] != null && values[i].equals(key)) {
					break;
				}
			}

			if (i == SIZE) {
				int[] handle = new int[SIZE];
				for (int j = 0; j < SIZE; j++) {
					ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executors[j];
					handle[j] = threadPoolExecutor.getQueue().size();
				}
				i = 0;
				for (int j = 1; j < handle.length; j++) {
					if (handle[j] < handle[i]) {
						i = j;
					}
				}
			}
		}
		final int x = i;
		return executors[x].submit(() -> {
			synchronized (lock) {
				values[x] = key;
			}
			UDPData data1;
			if (Datas.containsKey(key)) {
				text("已有记录：", key, "正在获取");
				data1 = new UDPData(Datas.remove(key), data);
				text(data1);
			} else {
				data1 = data;
			}
			synchronized (lock) {
				values[x] = null;
			}
			if (data1.value())
				return data.getData();
			else {
				Datas.put(key, data1);
				return null;
			}
		}).get();
	}

	public void clear() {
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
			String[] vs = this.values.clone();
			ExecutorService[] es = executors.clone();
			JSONArray v = new JSONArray();
			for (int i = 0; i < SIZE; i++) {
				ThreadPoolExecutor executor = (ThreadPoolExecutor) es[i];
				JSONObject o = new JSONObject();
				o.put("num", i);
				o.put("handle", vs[i] == null ? "null" : vs[i]);// 正在处理的key
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
