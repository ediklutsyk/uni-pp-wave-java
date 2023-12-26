import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Main {

    public static void main(String[] args) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        // ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);

        int[] array = createArray(100_000000);
        log.info("Array created");
        Instant start = Instant.now();
        calculateParallel(array);
        // calculateByThreads(array);
        // calculateSeq(array);
        // calculateByChunks(array, 10);
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info("Result: {}, Time: {}", array[0], timeElapsed);
    }


    private static void calculateSeq(int[] array) {
        int length = array.length;
        while (length > 1) {
            log.debug("Current result: {}", array[0]);
            for (int i = 0; i < length / 2; i++) {
                array[i] = array[i] + array[length - 1 - i];
            }
            length = length / 2 + length % 2;
        }
    }

    private static void calculateParallel(int[] array) {
        try (ExecutorService executorService = Executors.newWorkStealingPool(100)) {
            int length = array.length;
            while (length > 1) {
                Instant waveStart = Instant.now();
                CountDownLatch countDownLatch = new CountDownLatch(length / 2);
                for (int i = 0; i < length / 2; i++) {
                    final int finalI = i;
                    final int finalLength = length;
                    executorService.submit(() -> {
                        array[finalI] = array[finalI] + array[finalLength - 1 - finalI];
                        countDownLatch.countDown();
                    });
                }
                Instant waveEnd = Instant.now();
                log.debug("Wave time: {}", Duration.between(waveStart, waveEnd).toMillis());
                try {
                    log.debug("Waiting for threads to finish");
                    countDownLatch.await();
                    log.debug("Threads finished");
                } catch (InterruptedException ignored) {
                    log.debug("Something went wrong");
                }
                length = length / 2 + length % 2;
            }
        }
    }

    private static void calculateByChunks(int[] array, int threadsAmount) {
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadsAmount)) {
            int length = array.length;
            while (length > 1) {
                if (length / 2 < threadsAmount) {
                    threadsAmount = length / 2;
                }
                int part = length / 2 / threadsAmount;
                int remainder = length / 2 % threadsAmount;
                CountDownLatch countDownLatch = new CountDownLatch(threadsAmount);
                for (int i = 0; i < threadsAmount; i++) {
                    int start = i * part;
                    int finish = start + part;
                    if (i == threadsAmount - 1) {
                        finish += remainder;
                    }
                    int finalFinish = finish;
                    int finalLength = length;
                    executorService.submit(() -> {
                        for (int j = start; j < finalFinish; j++) {
                            array[j] = array[j] + array[finalLength - 1 - j];
                        }
                        countDownLatch.countDown();
                    });
                }
                log.debug("Waiting for threads to finish");
                Instant waveStart = Instant.now();
                countDownLatch.await();
                Instant waveEnd = Instant.now();
                log.debug("Wave time: {}", Duration.between(waveStart, waveEnd).toMillis());
                log.debug("Threads finished");
                length = length / 2 + length % 2;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void calculateByThreads(int[] array) {
        WaveThreadPool myThreadPool = new WaveThreadPool(array);
        while (myThreadPool.getLength().get() > 1) {
            if (!myThreadPool.getIsWaiting().get()) {
                log.debug("Current result: {}", array[0]);
                for (int i = 0; i < myThreadPool.getLength().get() / 2; i++) {
                    myThreadPool.submit(i);
                }
                log.debug("Waiting for threads to finish");
                myThreadPool.getIsWaiting().set(true);
            }
        }
        myThreadPool.stopQueue();
    }


    public static int[] createArray(int length) {
        int[] array = new int[length];
        Arrays.fill(array, 1);
        return array;
    }


}
