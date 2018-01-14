package hr.mfilipovic.dolor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

import okio.ByteString;

public class MainActivity extends AppCompatActivity implements ColorWebSocketOperator {

    private static final boolean DEBUG_LOGCAT = BuildConfig.DEBUG_LOGCAT;
    private static final boolean DEBUG_CONSOLE_OUT = BuildConfig.DEBUG_CONSOLE_OUT;
    public static final String TAG = "MainActivity";

    private TextView mConsoleView;
    private ColorView mColorView;
    private ColorWebSocketCommunicator mCommunicator;

    float startX;
    float startY;
    float middleX;
    float middleY;
    float endX;
    float endY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupWebSocket();
        setupDrawingView();
    }

    private void setupWebSocket() {
        mCommunicator = new ColorWebSocketCommunicator();
        mCommunicator.url("wss://echo.websocket.org")
                .operator(this)
                .build();
    }

    private void setupDrawingView() {
        mColorView = new ColorView(getApplicationContext());
        FrameLayout frameLayout = findViewById(R.id.frame_layout);
        frameLayout.addView(mColorView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleEvent(event);
    }

    private boolean handleEvent(MotionEvent event) {
        float X = event.getX();
        float Y = event.getY();
        logEvent(event);
        int action = event.getAction();

        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                startX = X;
                startY = Y;
                press(X, Y);
                return true;
            case (MotionEvent.ACTION_MOVE):
                middleX = X;
                middleY = Y;
                if (Math.abs(middleX - startX) > 10 || Math.abs(middleY - startY) > 10) {
                    startX = middleX;
                    startY = middleY;
                    release(middleX, middleY);
                    press(startX, startY);
                }
                moving(X, Y);
                return true;
            case (MotionEvent.ACTION_UP):
                endX = X;
                endY = Y;
                release(X, Y);
                return true;
            case (MotionEvent.ACTION_CANCEL):
                logEvent("CANCEL", "cancel", X, Y);
                return true;
            case (MotionEvent.ACTION_OUTSIDE):
                Log.d(TAG, "Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void press(float x, float y) {
        mColorView.startPath(x, y);
        logEvent("DOWN", "start", x, y);
    }

    private void moving(float x, float y) {
        mColorView.addToPath(x, y);
        logEvent("MOVE", "middle", x, y);
    }

    private void release(float x, float y) {
        mCommunicator.send(String.format(Locale.getDefault(), "{'x': %f, 'y': %f}", x, y));
        mColorView.finishPath(x, y);
        logEvent("UP", "end", x, y);
    }

    private void logEvent(String action, String position, float X, float Y) {
        if (DEBUG_LOGCAT) {
            Log.d(TAG, String.format("%s, %s: %f, %f", action, position, X, Y));
        }
    }

    private void logEvent(MotionEvent e) {
        if (DEBUG_LOGCAT) {
            String action = getActionName(e.getAction());
            Log.d(TAG, String.format("%s, %f, %f", action, e.getX(), e.getY()));
        }
    }

    private String getActionName(int action) {
        String result;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                result = "DOWN";
                break;
            case MotionEvent.ACTION_MOVE:
                result = "MOVE";
                break;
            case MotionEvent.ACTION_UP:
                result = "UP";
                break;
            default:
                result = "NOTDEF";
                break;
        }
        return result;
    }

    @Override
    public void opened() {
        console("Socket opened!");
    }

    @Override
    public void sent(boolean success, String payload) {
        if (success) {
            console("Message sent!");
        } else {
            console("Queue rejected the message! Message: %s", payload);
        }
    }

    @Override
    public void received(String message) {
        console("New message: " + message);
    }

    @Override
    public void received(ByteString bytes) {
        console("New message: " + bytes.toString());
    }

    @Override
    public void closing(int code, String reason) {
        console("Closing! %d/%s", code, reason);
    }

    @Override
    public void closed(int code, String reason) {
        console("Closed! %d/%s", code, reason);
    }

    @Override
    public void failed(String message) {
        console("Failed: " + message);
    }

    private void console(String format, Object... value) {
        console(String.format(Locale.getDefault(), format, value));
    }

    private void console(String out) {
        if (DEBUG_CONSOLE_OUT) {
            runOnUiThread(new Runnable() {
                String out;

                @Override
                public void run() {
                    if (mConsoleView == null) {
                        mConsoleView = findViewById(R.id.console_out);
                        mConsoleView.setMovementMethod(new ScrollingMovementMethod());
                        mConsoleView.setVisibility(View.VISIBLE);
                    }
                    StringBuilder currentText = new StringBuilder(mConsoleView.getText().toString());
                    mConsoleView.setText(currentText.append("\n").append(out));
                    Log.i(TAG, "console: " + out);
                }

                Runnable content(String out) {
                    this.out = out;
                    return this;
                }
            }.content(out));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCommunicator.destroy();
    }
}
