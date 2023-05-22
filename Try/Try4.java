package Try;

import java.io.File;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.List;

import liming.texthandle.FileRW;
import liming.texthandle.ReceiveMap;

public class Try4 {

    static FileRW ENCODED = FileRW.GBK;
    static int DATASIZE = 1024;

    public static void main(String[] args) throws Exception {
        // ReceiveMap map = new ReceiveMap();

        // map.put("ks1", "v1");
        // map.put("ks2", "v2");
        // map.put("ks3", "v3");
        // byte[] fb = FileRW.readFileByte(new
        // File("F:\\Desktop\\Jar文件\\ClassTable.jar"));
        // System.out.println("文件长度：" + fb.length);
        // map.put("file1", fb);
        // map.put("file2", fb);
        // map.put("file3", fb);
        // // map.put("kd1", "v1".getBytes(ENCODED.getValue()));
        // // map.put("kd2", "v2".getBytes(ENCODED.getValue()));
        // // map.put("kd3", "v3".getBytes(ENCODED.getValue()));
        // long startTime = System.currentTimeMillis();
        // List<DatagramPacket> list = DataProcessing.getPackets(ENCODED, DATASIZE,
        // map);
        // long handleTime = System.currentTimeMillis();
        // UDPDataInfo info = new UDPDataInfo(list.remove(0));
        // while (list.size() > 0) {
        // info.put(new UDPDataInfo(list.remove(0)));
        // if (info.isValid())
        // break;
        // }
        // long getTime = System.currentTimeMillis();

        // ReceiveMap map2 = info.getReceiveMap();
        // long endTime = System.currentTimeMillis();

        // System.out.println("总用时：" + (endTime - startTime) + ",打包用时：" + (handleTime -
        // startTime) + ",加载用时："
        // + (getTime - handleTime) + ",合并用时：" + (endTime - getTime));
        // System.out.println(map2);
        // System.out.println(info);
        // FileRW.writeFileByte(new File("./1.jar"), map2.optByte("file1"), false);
        // FileRW.writeFileByte(new File("./2.jar"), map2.optByte("file2"), false);
        // FileRW.writeFileByte(new File("./3.jar"), map2.optByte("file3"), false);
        // System.out.println(info);
        // for (String key : map2.getStringKey()) {
        // System.out.println(key + ":" + map2.optString(key));
        // }
        // for (String key : map2.getByteKey()) {
        // System.out.println(key + ":" + Arrays.toString(map2.optByte(key)));
        // }
    }
}
