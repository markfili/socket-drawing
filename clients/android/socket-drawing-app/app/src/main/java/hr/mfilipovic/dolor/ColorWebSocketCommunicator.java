package hr.mfilipovic.dolor;


import android.support.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ColorWebSocketCommunicator extends WebSocketListener {

    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private OkHttpClient client;
    private ColorWebSocketOperator operator;
    private String url;
    private String payload;

    public ColorWebSocketCommunicator operator(ColorWebSocketOperator operator) {
        this.operator = operator;
        return this;
    }

    public ColorWebSocketCommunicator url(String url) {
        this.url = url;
        return this;
    }

    public ColorWebSocketCommunicator build() {
        this.client = new OkHttpClient.Builder().build();
        return this;
    }

    public void destroy() {
        this.client = null;
    }

    public void send(String payload) {
        if (this.client == null) {
            throw new RuntimeException("Forgot to call build method, have you? I know it's redundant but who gives?");
        }
        this.payload = payload;

        Request request = new Request.Builder().url(url).build();
        this.client.newWebSocket(request, this);
        this.client.dispatcher().executorService().shutdown();
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send(payload);
        webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye!");
        operator.sent();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        operator.received(text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        operator.received(bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        operator.closing(code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        operator.closed(code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        operator.failed(t.getMessage());
    }
}
