import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
public class WaveRunnable implements Runnable {

    private final BlockingQueue<Integer> numberQueue;
    private final int[] array;
    private final AtomicInteger length;
    private AtomicBoolean isWaiting;
    private AtomicInteger status;

    public WaveRunnable(BlockingQueue<Integer> numberQueue, int[] array, AtomicInteger length, AtomicBoolean isWaiting, AtomicInteger status) {
        this.numberQueue = numberQueue;
        this.array = array;
        this.length = length;
        this.isWaiting = isWaiting;
        this.status = status;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Integer i = status.get() == 0 ? numberQueue.take() : numberQueue.poll();
                if (i != null) {
                    array[i] = array[i] + array[length.get() - 1 - i];
                    log.debug(Thread.currentThread().getName() + ": " + array[i]);
                } else {
                    log.debug("Waiting for tasks {}", status.get());
                    if (isWaiting.compareAndSet(true, false)) {
                        int currentLength = length.get();
                        length.set(currentLength / 2 + currentLength % 2);
                        log.debug("Updating length {} -> {}", currentLength, length.get());
                    } else {
                        log.debug("Waiting");
                        Thread.sleep(1);
                    }
                }
            } catch (InterruptedException e) {
                log.debug("Stopping {}", Thread.currentThread().getName());
                return;
            }
        }
    }
}