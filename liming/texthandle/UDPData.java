package liming.texthandle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import liming.texthandle.UDPLongDataTemps.Data;

class UDPData {
    private static int COUNT = 0;
    protected static final List<String> keys = Arrays.asList("DATAID", "DATALENGTH", "DATANUM");
    protected static final List<String> longkeys = Arrays.asList("LONGKEY", "LONGLENGTH", "LONGNUM", "LONGDATA");
    private long startTime;// 创建时间
    private long lastTime;// 最近接受的时间
    // private List<UDPData> datas;// 用于缓冲的数据
    private Map<String, String> data;// 数据列表
    private String ID; // 总数据id
    private boolean[] values; // 包状态标识符

    private int length; // 当前包长度

    private UDP_Interface interface1;// 消息接口
    private UDPLongDataTemps temps;// 长数据缓冲列表
    private boolean equals = false;// 有效标识符

    public UDPData(UDP_Interface interface1, JSONObject object) {
        String key = object.optString(keys.get(0));
        int length = object.optInt(keys.get(1), 0);
        if (key == null || length == 0)
            return;
        try {
            init(interface1, key, length);
            put(object);
            equals = true;
        } catch (Exception e) {
            System.out.println("数据获取失败");
        }
    }

    private List<UDPData> dataTemps = new ArrayList<>();

    public synchronized boolean add(UDPData data) {
        if (equals(data)) {
            lastTime = data.lastTime;
            dataTemps.add(data);
            for (int i = 0; i < length; i++)
                if (data.values[i])
                    values[i] = true;
            return true;
        } else {
            System.out.print("数据段不匹配 ID:" + ID.equals(data.ID) + " ,length:" + (length == data.length));
        }
        return false;
    }

    private UDPData(UDP_Interface interface1, String ID, int length) {
        init(interface1, ID, length);
    }

    private UDPData(UDPData data1, UDPData data2) throws Exception {
        if (data1 == null || data2 == null) {
            if (data1 == null ^ data2 == null) {
                if (data1 == null) {
                    interface1 = data2.interface1;
                    startTime = data2.startTime;
                    lastTime = data2.lastTime;
                    ID = data2.ID;
                    length = data2.length;
                    values = data2.values;
                    temps = data2.temps;
                    equals = data2.equals;
                } else {
                    interface1 = data1.interface1;
                    startTime = data1.startTime;
                    lastTime = data1.lastTime;
                    ID = data1.ID;
                    length = data1.length;
                    values = data1.values;
                    temps = data1.temps;
                    equals = data1.equals;
                }
            } else
                throw new Exception("传入参数为空");
        } else {
            interface1 = data1.interface1;
            text("开始合并参数", data1, data1.data.keySet(), data2, data2.data.keySet());
            if (!data1.equals(data2)) {
                text("合并的参数不匹配，d1:[", data1.ID, ":", data1.length, "]", "d2:[", data2.ID, ":", data2.length, "]");
                return;
            }
            startTime = 0;
            data = new HashMap<>();
            if (data1.equals || data2.equals) {
                if (data1.equals) {
                    text("数据1有效");
                    data.putAll(data1.data);
                    startTime = data1.startTime;
                    temps = data1.temps;
                    ID = data1.ID;
                }
                if (data2.equals) {
                    text("数据2有效");
                    data.putAll(data2.data);
                    if (startTime == 0) {
                        startTime = data2.startTime;
                    } else
                        startTime = Math.min(data1.startTime, data2.startTime);

                    if (temps == null)
                        temps = data2.temps;
                    else {
                        List<Data> datas = temps.add(data2.temps);
                        for (Data data : datas) {
                            this.data.put(data.getKey(), data.getValue());
                        }
                    }
                    ID = data2.ID;
                }
            } else {
                text("合并数据无效");
                return;
            }
        }
        lastTime = System.currentTimeMillis();
        length = data1.length;
        equals = true;
        text("合并完成", this, data.keySet());
    }

    private void init(UDP_Interface interface1, String id, int length) {
        this.length = length;
        lastTime = startTime = System.currentTimeMillis();
        this.interface1 = interface1;
        this.ID = id;
        values = new boolean[length];
        data = new HashMap<>();
        temps = new UDPLongDataTemps(interface1);
        dataTemps = new ArrayList<>();
        equals = true;
    }

    /**
     * 判断是否是同一个包
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof UDPData) {
            UDPData o1 = (UDPData) object;
            return length == o1.length && ID.equals(o1.ID);
        } else {
            return false;
        }
    }

    /**
     * 判断是否超时
     * 
     * @param startTimeout 总超时时间
     * @param timeout      离最近的一次消息超时时间
     * @return
     */
    public boolean isTimeOut(long startTimeout, long timeout) {
        long nowtime = System.currentTimeMillis();
        return this.startTime + startTimeout < nowtime || this.lastTime + timeout < nowtime;
    }

    private void put(JSONObject object) throws InterruptedException, ExecutionException {
        text("UDPData添加数据=", object);
        object.remove(keys.get(0));
        object.remove(keys.get(1));
        int num = (int) object.remove(keys.get(2));
        values[num] = true;
        if (isLong(object.keySet())) {// 长数据处理
            text("长数据", object);
            // 从长数据处理对象缓存池中传入包中参数，得到处理结果;
            Data longdata = temps.put(ID, object.optInt(longkeys.get(1)),
                    object.optInt(longkeys.get(2)),
                    object.optString(longkeys.get(0)), object.optString(longkeys.get(3)));
            if (longdata == null) {
                text("数据缺失", object.optString(longkeys.get(0)));
                return;
            } else {
                text("数据完整", object.optString(longkeys.get(0)));
                data.put(longdata.getKey(), longdata.getValue());
            }
        } else {// 短数据处理
            Set<String> keys = object.keySet();
            for (String key : keys) {
                String k;
                if (key.endsWith("/liming/")) {
                    k = key.substring(0, key.length() - "/liming/".length());
                } else
                    k = key;
                data.put(k, object.optString(key));
            }
            text("短数据添加", keys);
        }
        text(values);
    }

    /**
     * 判断是否是长数据
     * 
     * @param set
     * @return
     */
    private static boolean isLong(Set<String> set) {
        if (set.size() != 4)
            return false;
        for (String key : set) {
            if (!longkeys.contains(key))
                return false;
        }
        return true;
    }

    public boolean value() {
        for (boolean b : values) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public boolean[] values() {
        return values;
    }

    public Map<String, String> getData() {
        dataTemps.add(this);
        MergeAllRunnable mergeAllRunnable = new MergeAllRunnable();
        Thread thread = new Thread(mergeAllRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return mergeAllRunnable.get();
    }

    public String getID() {
        return ID;
    }

    public void clear() {
        data.clear();
        temps.clear();
    }

    @Override
    public String toString() {
        return "UDPData [data=" + data + ", ID=" + ID + ", values=" + Arrays.toString(values) + "]";
    }

    private void text(Object... objects) {
        String str = "\tUDPData:{";
        for (Object object : objects) {
            str += " " + object.toString();
        }
        interface1.udp_log(str + "}");
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastTime() {
        return lastTime;
    }

    /**
     * 获取超时信息，如最开始收到的时间，最后一个包收到的时间
     */
    public String getGiatus() {
        if (value()) {
            return "完整数据包";
        }
        StringBuffer giatus = new StringBuffer();
        int num = 0;
        for (int i = 0; i < length; i++) {
            if (!values[i]) {
                giatus.append("," + i);
                num++;
            }
        }
        return "共 " + length + " 个包,收到 " + (length - num) + " 个包，缺失 " + num + " 个,{"
                + (giatus.toString().length() > 1 ? giatus.toString().substring(1) : "")
                + "}";
    }

    class MergeAllRunnable implements Runnable {
        int count, size;
        ExecutorService executor;

        public MergeAllRunnable() {
            count = 0;
            size = 4;
            executor = Executors.newFixedThreadPool(size);
        }

        private Object key = new Object();
        private Object Count = new Object();
        private UDPData data;

        @Override
        public void run() {
            System.out.println("开始合并 待合并包数 " + dataTemps.size());
            while (dataTemps.size() > 1 || count > 0) {
                if (dataTemps.size() > 1) {
                    UDPData data1 = dataTemps.remove(0);
                    UDPData data2 = dataTemps.remove(0);
                    synchronized (Count) {
                        count++;
                    }
                    executor.submit(() -> {
                        COUNT++;
                        System.out.println("合成次数：" + COUNT);
                        MergeTwoRunnable runnable = new MergeTwoRunnable(data1, data2);
                        Thread thread = new Thread(runnable);
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            System.out.println(FileRW.getError(e));
                        }
                        dataTemps.add(runnable.get());
                        synchronized (Count) {
                            count--;
                        }
                        synchronized (key) {
                            key.notify();
                        }
                    });
                }
                if (dataTemps.size() < 2 || count == 4) {
                    synchronized (key) {
                        try {
                            key.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
            executor.shutdownNow();
            // System.out.println("剩余待处理对象 " + dataTemps.size());
            data = dataTemps.remove(0);
            // System.out.println(data);
            System.out.println("合并结束");
        }

        public Map<String, String> get() {
            return data.data;
        }
    }

    /**
     * 将两个UDP对象合成为一个UDP对象的方法
     */
    class MergeTwoRunnable implements Runnable {
        UDPData data1;
        UDPData data2;
        UDPData data;
        Object key;

        public MergeTwoRunnable(UDPData data1, UDPData data2) {
            this.data1 = data1;
            this.data2 = data2;
            key = new Object();
        }

        @Override
        public void run() {
            synchronized (key) {
                // System.out.println("开始合成数据包：" + data);
                try {
                    data = new UDPData(data1, data2);
                } catch (Exception e) {
                    e.printStackTrace();
                    data = new UDPData(interface1, ID, length);
                }

                // System.out.println("结束合成数据包：" + data);
            }
        }

        public UDPData get() {
            synchronized (key) {
                // System.out.println("获取数据：" + data);
                return data;
            }
        }
    }

}
