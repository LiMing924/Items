package Try;

import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;

import liming.texthandle.FileRW;
import liming.texthandle.HandleReceive;
import liming.texthandle.ReceiveMap;

public class Try4 {

    static FileRW ENCODED = FileRW.GBK;
    static int DATASIZE = 1024;

    public static void main(String[] args) throws Exception {
        ReceiveMap map = new ReceiveMap();
        map.put("text", new JSONObject().toString());
        map.put("json1", new JSONObject());
        map.put("json2", new JSONObject());
        map.put("ks1", "v1");
        map.put("ks2", "v2");
        map.put("ks3", "v3");
        byte[] fb = FileRW.readFileByte(new File("F:\\Desktop\\Jar文件\\ClassTable.jar"));
        map.put("kd1", "v1".getBytes(ENCODED.getValue()));
        map.put("kd2", "v2".getBytes(ENCODED.getValue()));
        map.put("kd3", "v3".getBytes(ENCODED.getValue()));
        System.out.println("文件长度：" + fb.length);
        map.put("file1", fb);
        map.put("file2", fb);
        map.put("file3", fb);
        // HandleReceive.setDebug(true);
        HandleReceive receive = new HandleReceive() {
            @Override
            public void sendDataToClient(ReceiveMap data, InetAddress address, int port, DatagramSocket socket)
                    throws ExecutionException, InterruptedException {
                System.out.println(data.getSendTime());
                System.out.println(data.getReceiveTime());
                System.out.println(data.getLastTime());
                System.out.println(data.getEndTime());
                System.out.println("网络延时：" + (data.getReceiveTime() - data.getSendTime()));
                System.out.println("首尾延时：" + (data.getLastTime() - data.getReceiveTime()));
                System.out.println("合并延时：" + (data.getEndTime() - data.getLastTime()));
                System.out.println("共计延时：" + (data.getEndTime() - data.getSendTime()));
                System.out.println(data);
                System.out.println(data.getDataInfo());
                stop();
            }

            @Override
            public boolean isDataReceived(long timeout) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'isDataReceived'");
            }
        };
        // receive.setDataSize(10240);
        receive.start();
        map.setIP("127.0.0.1", receive.getPort());
        receive.send(map);
    }
}
