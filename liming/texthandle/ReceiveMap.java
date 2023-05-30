package liming.texthandle;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ReceiveMap {
    /**
     * 携带的数据集
     */
    private final Map<String, Object> map;
    /**
     * byte[] 类型的key
     */
    private final Set<String> byteKey;

    private InetAddress inetAddress;
    private int port;

    /**
     * 发送端信息，仅在由本协议通过网络传递后，作为接收端才能设置信息
     */
    private UDPDataInfo info;

    public ReceiveMap() {
        map = new HashMap<>();
        byteKey = new HashSet<>();
    }

    public ReceiveMap(InetAddress inetAddress, int port) {
        this();
        setIP(inetAddress, port);
    }

    public ReceiveMap(String ip, int port) throws UnknownHostException {
        this();
        setIP(ip, port);
    }
    // ========= put putAll ============

    // byte,char,short,int,long,float,double,boolean
    // String,byte[]
    public synchronized ReceiveMap put(String key, Byte value) {
        map.put(key, (byte) value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Character value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Short value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Integer value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Long value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Float value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Double value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Boolean value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, String value) {
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, byte[] value) {
        byteKey.add(key);
        map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap put(String key, Object value) {
        if (value instanceof byte[]) {
            put(key, (byte[]) value);
        } else
            map.put(key, value);
        return this;
    }

    public synchronized ReceiveMap putAll(JSONObject values) {
        for (String key : values.keySet()) {
            put(key, values.get(key));
        }
        return this;
    }

    public synchronized ReceiveMap putAll(Map<?, ?> values) {
        for (Object key : values.keySet()) {
            put(key.toString(), values.get(key));
        }
        return this;
    }

    public synchronized ReceiveMap putAll(ReceiveMap receiveMap) {

        return this;
    }

    public ReceiveMap putFile(String key, File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + " 文件未找到");
        }
        return put(key, FileRW.readFileByte(file));
    }
    // ============= receive 方法区 ======================

    public synchronized Object remove(String key) {
        byteKey.remove(key);
        return map.remove(key);
    }

    // ============== get pot 方法区 ======================

    public Object get(String key) {
        if (map.containsKey(key))
            return map.get(key);
        throw new RuntimeException("key= " + key + " 未在在Receive中不存在");
    }

    public Object opt(String key) {
        return opt(key, null);
    }

    public Object opt(String key, Object value) {
        Object object = map.get(key);
        return object == null ? value : object;
    }

    public byte getByte(String key) {
        return (byte) get(key);
    }

    public byte optByte(String key) {
        return optByte(key, (byte) 0);
    }

    public byte optByte(String key, Byte value) {
        Object object = opt(key);
        return object instanceof Byte ? (byte) object : value;
    }

    public char getChar(String key) {
        return (char) get(key);
    }

    public char optChar(String key) {
        return optChar(key, (char) 0);
    }

    public char optChar(String key, Character value) {
        Object object = opt(key);
        return object instanceof Character ? (char) object : value;
    }

    public short getShort(String key) {
        return (short) get(key);
    }

    public short optShort(String key) {
        return optShort(key, (short) 0);
    }

    public short optShort(String key, Short value) {
        Object object = opt(key);
        return object instanceof Short ? (short) object : value;
    }

    public int getInt(String key) {
        return (int) get(key);
    }

    public int optInt(String key) {
        return optInt(key, 0);
    }

    public int optInt(String key, Integer value) {
        Object object = opt(key);
        return object instanceof Integer ? (int) object : value;
    }

    public long getLong(String key) {
        return (long) get(key);
    }

    public long optLong(String key) {
        return optLong(key, 0l);
    }

    public long optLong(String key, Long value) {
        Object object = opt(key);
        return object instanceof Long ? (long) object : value;
    }

    public float getFloat(String key) {
        return (float) get(key);
    }

    public float optFloat(String key) {
        return optFloat(key, 0f);
    }

    public float optFloat(String key, Float value) {
        Object object = opt(key);
        return object instanceof Float ? (float) object : value;
    }

    public double getDoyble(String key) {
        return (double) get(key);
    }

    public double optDouble(String key) {
        return optDouble(key, 0d);
    }

    public double optDouble(String key, Double value) {
        Object object = opt(key);
        return object instanceof Double ? (double) object : value;
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    public boolean optBoolean(String key) {
        return optBoolean(key, Boolean.valueOf(false));
    }

    public boolean optBoolean(String key, Boolean value) {
        Object object = opt(key);
        return object instanceof Boolean ? (boolean) object : value;
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public String optString(String key) {
        return optString(key, null);
    }

    public String optString(String key, String value) {
        Object object = opt(key);
        return object == null ? value : object.toString();
    }

    public byte[] getBytes(String key) {
        return (byte[]) get(key);
    }

    public byte[] optBytes(String key) {
        return optBytes(key, null);
    }

    public byte[] optBytes(String key, byte[] value) {
        Object object = opt(key);
        return object instanceof byte[] ? (byte[]) object : value;
    }

    public JSONObject getJsonObject(String key) {
        return (JSONObject) get(key);
    }

    public JSONObject optJsonObject(String key) {
        return optJsonObject(key, null);
    }

    public JSONObject optJsonObject(String key, JSONObject value) {
        Object object = opt(key);
        return object instanceof JSONObject ? (JSONObject) object : value;
    }

    public JSONArray getJsonArray(String key) {
        return (JSONArray) get(key);
    }

    public JSONArray optJsonArray(String key) {
        return optJsonArray(key, null);
    }

    public JSONArray optJsonArray(String key, JSONArray value) {
        Object object = opt(key);
        return object instanceof JSONArray ? (JSONArray) object : value;
    }

    // ================ 额外区方法 ==================

    /**
     * 获取所有存放的值
     */
    public Map<String, Object> getData() {
        return map;
    }

    /**
     * 获取所有存放的非byte[]类型数据
     */
    public Map<String, Object> getNoBytesData() {
        Map<String, Object> data = new HashMap<>(map);
        for (String key : byteKey) {
            data.remove(key);
        }
        return data;
    }

    /**
     * 获取所有存放的非byte[]类型数据key
     */
    public Set<String> getNoByteKey() {
        Set<String> set = new HashSet<>(map.keySet());
        set.removeAll(byteKey);
        return set;
    }

    /**
     * 获取所有存放的byte[]类型数据
     */
    public Map<String, byte[]> getByte() {
        Map<String, byte[]> bytes = new HashMap<>();
        for (String key : byteKey) {
            bytes.put(key, optBytes(key, null));
        }
        return bytes;
    }

    /**
     * 获取所有存放的byte[]类型数据key
     */
    public Set<String> getByteKey() {
        return byteKey;
    }

    /**
     * 判断键是否存在
     */
    public boolean contains(String key) {
        return map.containsKey(key);
    }

    /**
     * 判断键是否为byte[]的数据key
     */
    public boolean containsByte(String key) {
        return byteKey.contains(key);
    }

    /**
     * 由接收端接收逻辑设置
     */
    protected void setInfo(UDPDataInfo info) {
        this.info = info;
    }

    /**
     * 由发送端发送逻辑获取
     */
    protected UDPDataInfo getInfo() {
        return info;
    }

    /**
     * 获取发送端的编码格式
     */
    protected FileRW getEnCode() {
        return info.getEnCode();
    }

    /**
     * 获取发送端的缓冲区大小
     */
    protected int getDataSize() {
        return info.getDataSize();
    }

    /**
     * 获取发送的时间
     */
    public long getSendTime() {
        if (info != null) {
            return info.getSendTime();
        } else
            return 0;
    }

    /**
     * 获取首次接收到的时间
     */
    public long getReceiveTime() {
        if (info != null) {
            return info.getStartTime();
        } else
            return 0;
    }

    /**
     * 获取最后一次收到的时间
     */

    public long getLastTime() {
        if (info != null) {
            return info.getLastTime();
        } else
            return 0;
    }

    /**
     * 获取收到后合并数据的时间
     */
    public long getEndTime() {
        if (info != null) {
            return info.getEndTime();
        } else
            return 0;
    }

    /**
     * 获取发送方的数据信息
     */
    public String getDataInfo() {
        if (info != null)
            return info.getDataInfo();
        else
            return DataProcessing.getVesion();
    }

    /**
     * 设置接收对象的套接字和端口号
     */
    public void setIP(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }

    /**
     * 设置接收对象的套接字和端口号
     */

    public void setIP(String ip, int port) throws UnknownHostException {
        this.inetAddress = InetAddress.getByName(ip);
        this.port = port;
    }

    /**
     * 判断当前是否设置了套接字和端口号
     */
    public boolean isIP() {
        return inetAddress != null && port != 0;
    }

    /**
     * 返回设置的套接字
     */
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    /**
     * 返回设置的端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 清空设置的发送端信息
     */
    public void clearInfo() {
        info = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReceiveMap))
            return false;
        ReceiveMap receiveMap = (ReceiveMap) obj;
        return getDataInfo().equals(receiveMap.getDataInfo()) && map.equals(receiveMap.map);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getDataInfo().hashCode();
        result = 31 * result + map.values().stream().mapToInt(Object::hashCode).sum();
        return result;
    }

    @Override
    public String toString() {
        return "ReceiveMap [map=" + map.keySet() + ", byteKey=" + byteKey + "]";
    }

}
