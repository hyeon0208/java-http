package org.apache.catalina.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.techcourse.servlet.DispatcherServlet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.coyote.http11.Http11Processor;
import org.junit.jupiter.api.Test;
import support.StubSocket;

class ConnectorTest {

    private static final int DEFAULT_PORT = 8081;
    private static final int DEFAULT_ACCEPT_COUNT = 100;
    private static final int MAX_THREAD_POOL_COUNT = 10;
    private static final int CLIENT_REQUEST_COUNT = 100;

    @Test
    void concurrencyRequestTest() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Connector connector = new Connector(
                DEFAULT_PORT,
                DEFAULT_ACCEPT_COUNT,
                MAX_THREAD_POOL_COUNT,
                new DispatcherServlet()
        );
        connector.start();

        CountDownLatch latch = new CountDownLatch(CLIENT_REQUEST_COUNT);
        ExecutorService executorService = Executors.newFixedThreadPool(CLIENT_REQUEST_COUNT);
        for (int i = 0; i < CLIENT_REQUEST_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    StubSocket socket = new StubSocket();
                    Http11Processor processor = new Http11Processor(socket, new DispatcherServlet());
                    processor.process(socket);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        latch.await(1, TimeUnit.SECONDS);
        connector.stop();

        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(CLIENT_REQUEST_COUNT),
                () -> assertThat(failCount.get()).isEqualTo(0)
        );
    }
}
