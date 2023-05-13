import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import liming.key.encryption.RSA_Encryption;
import liming.texthandle.GetDataAndPacket;
import liming.texthandle.HandleReceive;
import liming.texthandle.IO;

public class S {
    public String picture;
    IO.IOScreen ioscreen;
    long rn = 0;

    public static void main(String[] args) {
        try {
            new S();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public S() throws SocketException {

        IO.IOData iod = new IO.IOData() {
            private long lastTime = System.currentTimeMillis();
            private int frameCount = 0;

            private void updateFPS() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTime >= 1000) {
                    int fps = (int) (frameCount * 1000 / (currentTime - lastTime));
                    System.out.print("\r" + fps + " fps " + rn);
                    lastTime = currentTime;
                    frameCount = 0;
                }
            }

            @Override
            public void getByte(long time, byte[] data) {
                picture = RSA_Encryption.signatureToString(data);
                frameCount++;
                updateFPS();
            }
        };
        iod.setTime(1 * 1000, 30);
        ioscreen = IO.getScreen(iod, 0.5f, "jpg", 0.2f);
        ioscreen.start();

        receive = new HandleReceive(new GetDataAndPacket() {

            @Override
            public void sendDataToClient(Map<String, String> data, InetAddress address, int port,
                    DatagramSocket socket) {
                rn++;
                data = new HashMap<>();
                String p = picture;
                data.put("picture", p);
                try {
                    receive.send(data, address, port, socket);
                } catch (InterruptedException | ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void writeLog(Object message) {
                // System.out.println(message);
            }

            @Override
            public void writeStrongLog(Object message) {
                System.out.println(message);
            }

            @Override
            public boolean isDataReceived(long timeout) {
                return false;
            }

        }, 6465, 512);
        receive.start();
    }

    HandleReceive receive;
}
