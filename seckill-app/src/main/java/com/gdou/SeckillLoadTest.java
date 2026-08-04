package com.gdou;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SeckillLoadTest {

    static final String URL = "http://localhost:8080/hhw/seckill/onseckill/1000";
    static final int TOTAL = 300;        // total requests
    static final int THREADS = 300;     // concurrent threads (每人一次)

    static AtomicInteger success = new AtomicInteger(0);
    static AtomicInteger fail = new AtomicInteger(0);
    static AtomicInteger timeoutCount = new AtomicInteger(0);
    static AtomicInteger connErrCount = new AtomicInteger(0);
    static AtomicInteger httpErrCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        // 解决 JDK HttpURLConnection 默认 maxConnections=5 的瓶颈
        System.setProperty("http.maxConnections", String.valueOf(THREADS));
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(TOTAL);

        long start = System.currentTimeMillis();

        for (int i = 1; i <= TOTAL; i++) {
            final String userId = "testuser" + i;
            pool.execute(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("X-UserId", userId);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        success.incrementAndGet();
                    } else {
                        // 读取错误响应体
                        java.io.InputStream errStream = conn.getErrorStream();
                        String errBody = errStream != null ?
                                new java.io.BufferedReader(new java.io.InputStreamReader(errStream))
                                        .lines().collect(java.util.stream.Collectors.joining("\n")) : "";
                        System.err.println("HTTP " + code + " userId=" + userId + " body=" + errBody);
                        httpErrCount.incrementAndGet();
                        fail.incrementAndGet();
                    }
                    conn.disconnect();
                } catch (java.net.SocketTimeoutException e) {
                    timeoutCount.incrementAndGet();
                    fail.incrementAndGet();
                } catch (java.net.ConnectException e) {
                    connErrCount.incrementAndGet();
                    fail.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Error userId=" + userId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    fail.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await();
        long elapsed = System.currentTimeMillis() - start;
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);

        System.out.println("========== Results ==========");
        System.out.println("Total:   " + TOTAL);
        System.out.println("Threads: " + THREADS);
        System.out.println("Success: " + success.get());
        System.out.println("Failed:  " + fail.get() + " (Timeout=" + timeoutCount.get()
                + " ConnectErr=" + connErrCount.get() + " HTTPErr=" + httpErrCount.get() + ")");
        System.out.println("Time:    " + elapsed + " ms");
        System.out.println("QPS:     " + (TOTAL * 1000L / elapsed));
    }
}
