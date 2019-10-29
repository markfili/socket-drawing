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
    private WebSocket webSocket;

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
        Request request = new Request.Builder().url(url).build();
        this.webSocket = this.client.newWebSocket(request, this);
        return this;
    }

    public void destroy() {
        this.webSocket.close(1000, "Normal closure. Goodbye!");
        this.client.dispatcher().executorService().shutdown();
        this.client = null;
    }

    public void send(String payload) {
        operator.sent(this.webSocket.send(payload), payload);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        operator.opened();
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
