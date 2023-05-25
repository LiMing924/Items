package liming.texthandle;

import java.net.DatagramPacket;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

public class DtatPoolTemps {
    private Map<String, UDPDataInfo> Datas = new ConcurrentHashMap<>();
    private int SIZE = 5;// 默认线程池个数
    private ExecutorService[] executors;
    private UDP_Interface interface1;
    private Map<String, Integer> valuesRun;// 关键字所在的处理线程池序号
    private int[] valuesWait; // 线程池待处理的关键字
    private boolean running;
    private Thread timeOutThread;

    private long timeOutAll = 100 * 1000, timeout = 5 * 1000;

    public DtatPoolTemps(UDP_Interface interface1, int size) {
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

    /**
     * 添加数据
     * 
     * @param object
     * @return
     * @throws Exception
     */
    public ReceiveMap get(DatagramPacket packet) throws Exception {
        UDPDataInfo info = null;
        try {
            info = new UDPDataInfo(packet);
        } catch (Exception e) {
            text_Strong(e.toString());
            return null;
        }
        text("正在处理数据，info：", info);
        if (info.isValid()) {// 判断单个数据是否接收完,若接收完就直接返回结果
            // System.out.println("数据接收完毕，返回处理");
            return info.getReceiveMap();
        }
        String key = info.getID();

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
            add(i);
        }
        /* 计算结束 */

        final int x = i;
        final UDPDataInfo udpDataInfo = info;
        return executors[x].submit(() -> {
            try {
                UDPDataInfo d = putData(key, udpDataInfo);
                if (d.isValid()) {
                    valuesRun.remove(key);
                    return d.getReceiveMap();
                } else
                    return null;
            } finally {
                minus(x);
            }
        }).get();
    }

    private Object WaitKey = new Object();

    private void minus(int i) {
        synchronized (WaitKey) {
            valuesWait[i]--;
        }
    }

    private void add(int i) {
        synchronized (WaitKey) {
            valuesWait[i]++;
        }
    }

    private synchronized UDPDataInfo putData(String key, UDPDataInfo info) {
        UDPDataInfo i = null;
        if (Datas.containsKey(key)) {
            i = Datas.remove(key);
            i.put(info);
        } else {
            i = info;
        }
        if (!i.isValid()) {
            Datas.put(key, i);
        }
        return i;
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
        String str = "处理缓冲池:[";
        for (Object object : objects) {
            str += " " + object.toString();
        }
        interface1.udp_log(str + "]");
    }

    private void text_Strong(Object... objects) {
        String str = "处理缓冲池:[";
        for (Object object : objects) {
            str += " " + object.toString();
        }
        interface1.udp_slog(str + "]");
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

    private class TimeOutRunnble implements Runnable {
        @Override
        public void run() {
            while (running) {
                synchronized (Datas) {
                    for (String key : Datas.keySet()) {
                        if (Datas.get(key).isTimeOut(timeOutAll, timeout)) {
                            UDPDataInfo data = Datas.remove(key);
                            interface1.udp_slog("接收超时 " + data.getID() + " " + new Date(data.getStartTime()) + " "
                                    + new Date(data.getLastTime()) + " " + new Date() + " " + data.getGiatus());
                        }
                    }
                    try {
                        Datas.wait(timeout / 2);
                    } catch (Exception e) {
                    }
                }
            }
            interface1.udp_log("超时线程结束");
        }
    }
}
