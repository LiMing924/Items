package liming.texthandle;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReceiveMap {
    private Map<String, Object> map;
    private Set<String> stringKey;
    private Set<String> byteKey;
    private UDPDataInfo info;

    public ReceiveMap() {
        map = new HashMap<>();
        stringKey = new HashSet<>();
        byteKey = new HashSet<>();
    }

    public String put(String key, String value) {
        stringKey.add(key);
        return (String) map.put(key, value);
    }

    public byte[] put(String key, byte[] value) {
        byteKey.add(key);
        return (byte[]) map.put(key, value);
    }

    public byte[] putFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + " 文件未找到");
        }
        return put(file.getName(), FileRW.readFileByte(file));
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

    public Object opt(String key) {
        if (map.containsKey(key))
            return map.get(key);
        return null;
    }

    public String optString(String key) {
        if (stringKey.contains(key))
            return (String) map.get(key);
        return null;
    }

    public byte[] optByte(String key) {
        if (byteKey.contains(key))
            return (byte[]) map.get(key);
        return null;
    }

    public Object opt(String key, Object value) {
        if (map.containsKey(key))
            return map.get(key);
        return value;
    }

    public String optString(String key, String value) {
        if (stringKey.contains(key))
            return (String) map.get(key);
        return value;
    }

    public byte[] optByte(String key, byte[] value) {
        if (byteKey.contains(key))
            return (byte[]) map.get(key);
        return value;
    }

    public Set<String> getStringKey() {
        return stringKey;
    }

    public Set<String> getByteKey() {
        return byteKey;
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

    protected void setInfo(UDPDataInfo info) {
        this.info = info;
    };

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

    public String getDataInfo() {
        if (info != null)
            return info.getDataInfo();
        else
            return DataProcessing.getVesion();
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
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
        return "ReceiveMap [map=" + map.keySet() + ", stringKey=" + stringKey + ", byteKey=" + byteKey + "]";
    }

}
