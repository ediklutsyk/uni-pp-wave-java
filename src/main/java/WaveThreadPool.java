import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
public class WaveThreadPool {

    private static int THREADS_AMOUNT = 10;
    private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
    private Thread[] threads = new Thread[THREADS_AMOUNT];
    private AtomicInteger length;
    private AtomicBoolean isWaiting = new AtomicBoolean(false);
    private AtomicInteger status = new AtomicInteger(0);

    public WaveThreadPool(int[] array) {
        this.length = new AtomicInteger(array.length);
        for (int i = 0; i < THREADS_AMOUNT; i++) {
            Thread thread = new Thread(new WaveRunnable(queue, array, length, isWaiting, status));
            threads[i] = thread;
            thread.start();
        }
    }

    public void submit(Integer index) {
        getQueue().add(index);
        if (getStatus().get() == 0) {
            getStatus().set(1);
        }
    }

    public void stopQueue() {
        for (Thread thread : getThreads()) {
            thread.interrupt();
        }
    }

}