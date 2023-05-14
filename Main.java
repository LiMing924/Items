import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) throws AWTException, IOException {
        ScreenCapture screenCapture = new ScreenCapture(1f, "jpg", 0.1f, 30, 2f);
        screenCapture.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int frame = 30;
        long time = 1000;
        int size = (int) Math.ceil(frame * (time / 2000));
        int fps = (int) Math.ceil(frame * 0.6);
        List<BufferedImage> datas = null;
        while (datas == null || datas.size() < size) {
            if (screenCapture.getFps() < fps)
                continue;
            datas = screenCapture.get(System.currentTimeMillis(), time);
        }
        screenCapture.stop();

        BufferedImage bufferImage = null;

        for (int i = 0; i < datas.size(); i++) {
            BufferedImage data = datas.get(i);
            if (data == null)
                continue;
            BufferedImage image = datas.get(i);
            // 创建双缓冲区
            if (bufferImage == null) {
                bufferImage = new BufferedImage(image.getWidth(), image.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
            }

            // 在缓冲区中绘制图像
            Graphics2D g2d = bufferImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            // 将图像保存在本地
            File outputfile = new File("./img/image" + i + ".jpg");
            ImageIO.write(bufferImage, "jpg", outputfile);
        }
        System.out.println(datas.size());
    }

}
