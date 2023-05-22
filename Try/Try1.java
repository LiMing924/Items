package Try;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;

import liming.texthandle.GetDataAndPacket;
import liming.texthandle.HandleReceive;
import liming.texthandle.ReceiveMap;

public class Try1 {
    public static void main(String[] args) throws SocketException {
        new Try1();
    }

    HandleReceive receive;

    public Try1() throws SocketException {
        HandleReceive.setDebug(true);
        receive = new HandleReceive(new GetDataAndPacket() {

            @Override
            public void sendDataToClient(ReceiveMap data, InetAddress address, int port,
                    DatagramSocket socket) {
                System.out.println(data);
                System.out.println(data.getDataInfo());
            }

            @Override
            public void writeLog(Object message) {
                if (receive.getDebug()) {
                    System.out.println("writeLog: " + message);
                }
            }

            @Override
            public void writeStrongLog(Object message) {
                System.out.println("writeStrongLog: " + message);
            }

            @Override
            public boolean isDataReceived(long timeout) {
                return false;
            }

        }, 6465, 1024);
        // receive.setDataSizeMax(100, Unit.GB);
        receive.start();
        System.out.println("发送长度为：" + receive.getDataSize());
        String str = "0123456789";
        while (str.length() < receive.getDataSize() * 1) {
            str += str;
        }
        // System.out.println("发送长度为：" + receive.getDataSize());
        ReceiveMap map = new ReceiveMap();
        map.put("str1", str);
        map.put("str2", str);
        map.put("str3", str);
        System.out.println(map.getDataInfo());
        try {
            receive.send(map, "127.0.0.1", 6465);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
