package com.mmall.concurrency.example.ThreadSafty.count;

import com.mmall.concurrency.annotations.NotThreadSafe;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@NotThreadSafe
public class CountExample4 {

    //请求总数
    public static int clientTotal = 5000;

    //同时并发吃行的线程数
    public static int threadTotal = 200;

    //
    public static volatile int count = 0;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        final Semaphore semaphore = new Semaphore(threadTotal);
        final CountDownLatch countDownLatch = new CountDownLatch(clientTotal);
        for (int i = 0; i < clientTotal; i++) {
            executorService.execute(() -> {
                try {
                    //判断进程是否允许被执行
                    semaphore.acquire();
                    add();
                    semaphore.release();
                } catch (Exception e) {
                    log.error("exception", e);
                }
                //闭锁统计
                countDownLatch.countDown();

            });
        }
        countDownLatch.await();
        log.info("count:{}", count);
        executorService.shutdown();
    }

    private static void add() {
        count++;
        // 1, 取出主内存的count值
        // 2, +1
        // 3, 写会主存
    }

}