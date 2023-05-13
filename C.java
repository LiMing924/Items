import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import liming.texthandle.GetDataAndPacket;
import liming.texthandle.HandleReceive;

public class C {
    HandleReceive receive;

    public static void main(String[] args) throws SocketException {
        new C();
    }

    public C() throws SocketException {
        receive = new HandleReceive(new GetDataAndPacket() {

            @Override
            public void sendDataToClient(Map<String, String> data, InetAddress address, int port,
                    DatagramSocket socket) {
                String d = data.get("picture");
                System.out.println("收到数据" + d.length());
            }

            @Override
            public void writeLog(Object message) {
            }

            @Override
            public void writeStrongLog(Object message) {
            }

            @Override
            public boolean isDataReceived(long timeout) {
                // TODO Auto-generated method stub
                return false;
            }

        }, 6666, 512);
        receive.start();
        Map<String, String> d = new HashMap<>();
        d.put("try", "try");
        while (true) {
            try {
                receive.send(d, "45.125.46.48", 11378);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
