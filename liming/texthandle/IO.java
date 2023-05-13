package liming.texthandle;

import java.awt.Rectangle;
// import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
// import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class IO {
    public static abstract class IOData {
        public abstract void getByte(long time, byte[] data);

        private long time = 1 * 1000 / 30;

        public long setTime(long AllTime, float frameRate) {
            return time = (long) Math.ceil(AllTime / frameRate);

        }
    }

    public static IOScreen getScreen(IOData io, float quality, String code, float factor) {
        return IOScreen.getScreen(io, quality, code, factor);
    }

    /**
     * 采用异步线程获取屏幕io流
     */
    public static class IOScreen {
        private ExecutorService executorService;
        private Thread thread;
        private volatile boolean running = true;
        private State state;
        private IOData io;
        private String code;
        private float quality;
        private long time;

        private IOScreen(IOData io, float quality, String code, float factor) {
            state = new State(io, factor);
            this.io = io;
            this.code = code;
            this.quality = quality;
            executorService = Executors.newFixedThreadPool((int) state.max * 2);
            time = 600;
        }

        public static IOScreen getScreen(IOData io, float quality, String code, float factor) {
            return new IOScreen(io, quality, code, factor);
        }

        public void start() {
            running = true;
            if (thread != null && !thread.isAlive()) {
                thread.interrupt();
            }

            if (addThread != null && !addThread.isAlive()) {
                addThread.interrupt();
            }

            datas = new ArrayList<>();

            addThread = new Thread(() -> {
                synchronized (datas) {
                    while (running) {
                        // System.out.println("轮回处理截图数据");
                        while (datas.size() > 0) {
                            Data data = datas.remove(0);
                            if (data.time < System.currentTimeMillis() - time * 2)
                                continue;
                            new Thread(() -> {
                                // System.out.println("截图数据回传");
                                io.getByte(data.time, data.bytes);
                            }).start();
                        }
                        // System.out.println("截图数据暂时处理完毕,进入等待");
                        try {
                            datas.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // System.out.println("等待结束");
                    }
                }
            });

            thread = new Thread(() -> {
                long StartTime = System.currentTimeMillis();
                while (running) {
                    Future<?> future = executorService.submit(new MRunnable());
                    executorService.execute(() -> {
                        try {
                            future.get(time, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            // TODO: handle exception
                            future.cancel(true);
                        }
                    });
                    try {
                        Thread.sleep(io.time - (System.currentTimeMillis() - StartTime) % io.time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            addThread.start();
            System.out.println("已启动1");
        }

        public void stop() {
            running = false;
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executorService.shutdownNow();
        }

        private List<Data> datas;

        private Thread addThread;

        private void add(Data data) {
            synchronized (datas) {
                // System.out.println("添加截图数据");
                datas.add(data);
                datas.notify();
            }
        }

        private class Data {
            long time;
            byte[] bytes;

            private Data(long time, byte[] bytes) {
                this.time = time;
                this.bytes = bytes;
            }
        }

        private class State {
            private long num;
            private long max;

            private State(IOData io, float factor) {
                max = (long) Math.ceil((1 * 1000 / io.time) * factor);
                num = 0;
            }

            private long add() {
                return ++num;
            }

            private long remove() {
                return --num;
            }

            private boolean equals() {
                return num < max;
            }
        }

        private class MRunnable implements Runnable {

            @Override
            public void run() {
                try {
                    synchronized (state) {
                        if (!state.equals())
                            return;
                        state.add();
                    }
                    long time = System.currentTimeMillis();
                    Robot robot = new Robot();
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    byte[] bytes = compressImage(screenCapture, quality);
                    add(new Data(time, bytes));
                } catch (Exception e) {
                } finally {
                    synchronized (state) {
                        state.remove();
                    }
                }

            }

            private byte[] compressImage(BufferedImage image, float quality) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(code);
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
                ios.close();
                writer.dispose();
                return baos.toByteArray();
            }

        }
    }
}