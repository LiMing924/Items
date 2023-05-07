package liming.texthandle;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
		myThread = new MyThread();
		myThread.start();
	}

	public Map<String, String> get(JSONObject object) throws InterruptedException, ExecutionException {
		text("正在处理数据，map：", object);
		text(Datas.keySet(), "添加数据=", object);
		String key = object.optString(UDPData.keys.get(0));
		int i;
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
		final int x = i;
		return executors[x].submit(() -> {
			values[x] = key;
			UDPData data;
			int length = object.optInt(UDPData.keys.get(1));
			if (Datas.containsKey(key)) {
				text("已有记录：", key, "正在获取");
				data = Datas.remove(key);
				text(data);
			} else {
				text("首次接收：", key, "正在新建");
				data = new UDPData(interface1, key, length);
			}
			data.put(object);
			values[x] = null;
			if (data.value())
				return data.getData();
			else {
				Datas.put(key, data);
				return null;
			}
		}).get();
	}

	@SuppressWarnings("all")
	public void clear() {
		Datas.clear();
		expire = false;
		{
			long s = System.currentTimeMillis();
			while (!expire_end) {
				if (System.currentTimeMillis() >= s + IntervalTime * 2.5) {
					break;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!expire_end) {
				text_Strong("超时数据处理线程终止异常");
				myThread.stop();
			}
		}
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

	// 查询是否过期
	private MyThread myThread;
	private boolean expire = true;
	private boolean expire_end = false;
	private long IntervalTime = 1 * 1000;

	private class MyThread extends Thread {
		@Override
		public void run() {
			text("超时数据处理功能已开启");
			Random random = new Random();
			while (expire) {
				try {
					MThread mThread = new MThread();
					mThread.start();
					mThread.join();
					long time = random.nextInt((int) IntervalTime * 3 / 2) + IntervalTime / 2;
					Thread.sleep(time);
				} catch (InterruptedException e) {
					continue;
				}
			}
			expire_end = true;
			text_Strong("超时数据处理已关闭");
		}

		class MThread extends Thread {
			@Override
			public void run() {
				for (String key : Datas.keySet()) {
					UDPData data = Datas.get(key);
					if (data.getTime() + 100 * 1000 >= System.currentTimeMillis()) {
						text("key= " + key + " 超时未收取完成，正在释放该数据 " + data);
					}
					if (!expire)
						break;
				}
			}

		}
	}
}
