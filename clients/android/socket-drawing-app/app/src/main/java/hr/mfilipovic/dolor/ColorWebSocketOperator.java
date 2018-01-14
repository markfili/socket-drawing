package hr.mfilipovic.dolor;

import okio.ByteString;

interface ColorWebSocketOperator {
    void sent();

    void received(String message);

    void received(ByteString bytes);

    void closing(int code, String reason);

    void closed(int code, String reason);

    void failed(String message);
}
