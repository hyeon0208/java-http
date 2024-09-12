package org.apache.catalina.connector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.coyote.http11.Dispatcher;
import org.apache.coyote.http11.Http11Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connector implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Connector.class);

    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_ACCEPT_COUNT = 100; // 동시에 수용할 수 있는 최대 클라이언트의 연결 요청 수
    private static final int MAX_THREAD_POOL_COUNT = 10; // 서버가 동시에 처리할 수 있는 최대 스레드 수

    private final ServerSocket serverSocket;
    private final Dispatcher dispatcher;
    private final ExecutorService executorService;
    private final LinkedBlockingQueue<Runnable> acceptQueue;
    private boolean stopped;

    public Connector(Dispatcher dispatcher) {
        this(DEFAULT_PORT, DEFAULT_ACCEPT_COUNT, MAX_THREAD_POOL_COUNT, dispatcher);
    }

    public Connector(final int port, final int acceptCount, final int maxThread, final Dispatcher dispatcher) {
        this.serverSocket = createServerSocket(port, acceptCount);
        this.stopped = false;
        this.executorService = Executors.newFixedThreadPool(maxThread);
        this.dispatcher = dispatcher;
        this.acceptQueue = new LinkedBlockingQueue<>(acceptCount);
    }

    private ServerSocket createServerSocket(final int port, final int acceptCount) {
        try {
            final int checkedPort = checkPort(port);
            final int checkedAcceptCount = checkAcceptCount(acceptCount);
            return new ServerSocket(checkedPort, checkedAcceptCount);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int checkPort(final int port) {
        final var MIN_PORT = 1;
        final var MAX_PORT = 65535;

        if (port < MIN_PORT || MAX_PORT < port) {
            return DEFAULT_PORT;
        }
        return port;
    }

    private int checkAcceptCount(final int acceptCount) {
        return Math.max(acceptCount, DEFAULT_ACCEPT_COUNT);
    }

    public void start() {
        var thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        stopped = false;
        log.info("Web Application Server started {} port.", serverSocket.getLocalPort());
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                acceptQueue.put(this::connect);
            } catch (InterruptedException e) {
                log.error("최대 요청 처리 수를 초과 했습니다. - 대기 중인 작업 : {}", acceptQueue.size());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void connect() {
        try {
            process(serverSocket.accept(), dispatcher);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void process(final Socket connection, final Dispatcher dispatcher) {
        if (connection == null) {
            return;
        }
        var processor = new Http11Processor(connection, dispatcher);
        executorService.execute(processor);
    }

    public void stop() {
        stopped = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
