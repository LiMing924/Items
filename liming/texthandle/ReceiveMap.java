package liming.texthandle;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.json.JSONObject;

public final class ReceiveMap {
    private final Map<String, Object> map;
    private final Set<String> stringKey;
    private final Set<String> byteKey;
    private final Set<String> jsonObjectkey;

    private InetAddress inetAddress;
    private int port;

    private UDPDataInfo info;

    public ReceiveMap() {
        map = new HashMap<>();
        stringKey = new HashSet<>();
        byteKey = new HashSet<>();
        jsonObjectkey = new HashSet<>();
    }

    public ReceiveMap(InetAddress inetAddress, int port) {
        this();
        setIP(inetAddress, port);
    }

    public ReceiveMap(String ip, int port) throws UnknownHostException {
        this();
        setIP(ip, port);
    }

    public void setIP(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }

    public void setIP(String ip, int port) throws UnknownHostException {
        this.inetAddress = InetAddress.getByName(ip);
        this.port = port;
    }

    public boolean isIP() {
        return inetAddress != null && port != 0;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public int getPort() {
        return port;
    }

    public String put(String key, String value) {
        if (contains(key) && !containsString(key))
            remove(key);
        stringKey.add(key);
        return (String) map.put(key, value);
    }

    public byte[] put(String key, byte[] value) {
        if (contains(key) && !containsByte(key))
            remove(key);
        byteKey.add(key);
        return (byte[]) map.put(key, value);
    }

    public JSONObject put(String key, JSONObject value) {
        if (contains(key) && !containsJsonObject(key))
            remove(key);
        jsonObjectkey.add(key);
        return (JSONObject) map.put(key, value);
    }

    public byte[] putFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + " 文件未找到");
        }
        return put(file.getName(), FileRW.readFileByte(file));
    }

    private void remove(String key) {
        Object object = map.remove(key);
        if (object instanceof String) {
            stringKey.remove(key);
        } else if (object instanceof byte[]) {
            byteKey.remove(key);
        } else if (object instanceof JSONObject) {
            jsonObjectkey.remove(key);
        }
    }

    public Object get(String key) {
        return map.get(key);
    }

    public String getString(String key) {
        return (String) map.get(key);
    }

    public byte[] getByte(String key) {
        return (byte[]) map.get(key);
    }

    public JSONObject getJsonObject(String key) {
        return (JSONObject) map.get(key);
    }

    public Map<String, String> getString() {
        Map<String, String> strings = new HashMap<>();
        for (String key : stringKey) {
            strings.put(key, getString(key));
        }
        return strings;
    }

    public Map<String, byte[]> getByte() {
        Map<String, byte[]> bytes = new HashMap<>();
        for (String key : byteKey) {
            bytes.put(key, getByte(key));
        }
        return bytes;
    }

    public Map<String, JSONObject> getJsonObject() {
        Map<String, JSONObject> jsonObjects = new HashMap<>();
        for (String key : jsonObjectkey) {
            jsonObjects.put(key, getJsonObject(key));
        }
        return jsonObjects;
    }

    public Object opt(String key) {
        return opt(key, null);
    }

    public String optString(String key) {
        return optString(key, null);
    }

    public byte[] optByte(String key) {
        return optByte(key, null);
    }

    public JSONObject optJsonObject(String key) {
        return optJsonObject(key, null);
    }

    public Object opt(String key, Object value) {
        if (map.containsKey(key))
            return map.get(key);
        return value;
    }

    public String optString(String key, String value) {
        if (stringKey.contains(key))
            return getString(key);
        return value;
    }

    public byte[] optByte(String key, byte[] value) {
        if (byteKey.contains(key))
            return getByte(key);
        return value;
    }

    public JSONObject optJsonObject(String key, JSONObject value) {
        if (jsonObjectkey.contains(key))
            return getJsonObject(key);
        return value;
    }

    public Set<String> getStringKey() {
        return stringKey;
    }

    public Set<String> getByteKey() {
        return byteKey;
    }

    public Set<String> getJsonObjectKey() {
        return jsonObjectkey;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    public boolean containsString(String key) {
        return stringKey.contains(key);
    }

    public boolean containsByte(String key) {
        return byteKey.contains(key);
    }

    public boolean containsJsonObject(String key) {
        return jsonObjectkey.contains(key);
    }

    protected void setInfo(UDPDataInfo info) {
        this.info = info;
    }

    protected UDPDataInfo getInfo() {
        return info;
    }

    protected FileRW getEnCode() {
        return info.getEnCode();
    }

    protected int getDataSize() {
        return info.getDataSize();
    }

    public long getSendTime() {
        if (info != null) {
            return info.getSendTime();
        } else
            return 0;
    }

    public long getReceiveTime() {
        if (info != null) {
            return info.getStartTime();
        } else
            return 0;
    }

    public long getLastTime() {
        if (info != null) {
            return info.getLastTime();
        } else
            return 0;
    }

    public long getEndTime() {
        if (info != null) {
            return info.getEndTime();
        } else
            return 0;
    }

    public String getDataInfo() {
        if (info != null)
            return info.getDataInfo();
        else
            return DataProcessing.getVesion();
    }

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
        return "ReceiveMap [map=" + map.keySet() + ", stringKey=" + stringKey + ", byteKey=" + byteKey
                + ", jsonObjectkey=" + jsonObjectkey + "]";
    }

}
