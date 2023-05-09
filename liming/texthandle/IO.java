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

    public static IOScreen getScreen(IOData io, float quality, String code, float factor) {
        IOScreen ioscreen = new IOScreen(io, quality, code, factor);
        return ioscreen;
    }

    /**
     * 采用异步线程获取屏幕io流
     */
    public static class IOScreen {
        private Thread thread;
        private volatile boolean running = true;
        private State state;

        public IOScreen(IOData io, float quality, String code, float factor) {
            state = new State(io, factor);
            thread = new Thread(() -> {
                long StartTime = System.currentTimeMillis();
                while (running) {
                    Thread thread = new Thread(() -> {
                        try {
                            synchronized (state) {
                                if (!state.equals())
                                    return;
                                state.add();
                            }
                            Robot robot = new Robot();
                            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                            BufferedImage screenCapture;
                            ByteArrayOutputStream baos;
                            screenCapture = robot.createScreenCapture(screenRect);
                            BufferedImage compressedImage = compressImage(screenCapture, quality);
                            baos = new ByteArrayOutputStream();
                            ImageIO.write(compressedImage, code, baos);
                            byte[] bytes = baos.toByteArray();
                            io.getByte(bytes);
                            synchronized (state) {
                                state.remove();
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    });
                    thread.start();
                    try {
                        Thread.sleep(io.time - (System.currentTimeMillis() - StartTime) % io.time);

                    } catch (InterruptedException e) {
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

        private class State {
            private long num;
            private long max;

            public State(IOData io, float factor) {
                max = (long) Math.ceil((1 * 1000 / io.time) * factor);
                num = 0;
            }

            public long add() {
                return ++num;
            }

            public long remove() {
                return --num;
            }

            private boolean equals() {
                return num < max;
            }
        }
    }
}