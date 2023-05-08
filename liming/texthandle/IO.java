package liming.texthandle;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

public class IO {
    public static abstract class IOData {
        public abstract void getByte(byte[] data);

        long time = 1 * 1000 / 30;

        public long setTime(long AllTime, float frameRate) {
            return time = (long) Math.ceil(AllTime / frameRate);
        }
    }

    public static IOThread getScreen(IOData io, float quality, String code) {
        IOThread iothread = new IOThread(io, quality, code);
        return iothread;
    }

    public static class IOThread {
        private Thread thread;
        private volatile boolean running = true;

        public IOThread(IOData io, float quality, String code) {
            thread = new Thread(() -> {
                while (running) {
                    long StartTime = System.currentTimeMillis();
                    new Thread(() -> {
                        try {
                            Robot robot = new Robot();
                            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                            BufferedImage screenCapture;
                            ByteArrayOutputStream baos;
                            long S = System.currentTimeMillis();
                            screenCapture = robot.createScreenCapture(screenRect);
                            BufferedImage compressedImage = compressImage(screenCapture, quality);
                            baos = new ByteArrayOutputStream();
                            ImageIO.write(compressedImage, code, baos);
                            byte[] bytes = baos.toByteArray();
                            // System.out.println(bytes.length);
                            io.getByte(bytes);
                            long s = System.currentTimeMillis();
                            long e = System.currentTimeMillis();
                            System.out.println("S: " + S + "\t,s: " + s + "\t,e: " + e + "\tr: " + (s - S) + "\t,t: "
                                    + (e - s) + "\t,a: " + (e - S) + "\t,size: " + bytes.length);
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }).start();
                    try {
                        Thread.sleep(io.time - (System.currentTimeMillis() - StartTime) % io.time);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }

        public void start() {
            running = true;
            thread.start();

        }

        public void stop() {
            running = false;
        }

        private BufferedImage compressImage(BufferedImage image, float quality) {
            int width = (int) (image.getWidth() * quality);
            int height = (int) (image.getHeight() * quality);
            BufferedImage compressedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = compressedImage.createGraphics();
            g2d.drawImage(image, 0, 0, width, height, null);
            g2d.dispose();
            return compressedImage;
        }
    }
}