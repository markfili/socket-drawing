package hr.mfilipovic.dolor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private float mFieldBlockSize;
    private DrawingRunnable mDrawingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupWebSocket();
        setupDrawingView();
        sendInitMessage();
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

    private void sendInitMessage() {
        try {
            mCommunicator.send(initMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String initMessage() throws JSONException {
        JSONObject message = new JSONObject();
        message.put("method", "init");
        JSONObject field = new JSONObject();
        field.put("x", 100);
        field.put("y", 100);
        message.put("field", field);
        return message.toString();
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
//        mColorView.startPath(x, y);
        mCommunicator.send(getCoordinatesJson(getActionName(MotionEvent.ACTION_DOWN), x, y));
        logEvent("DOWN", "start", x, y);
    }

    private void moving(float x, float y) {
//        mColorView.addToPath(x, y);
        mCommunicator.send(getCoordinatesJson(getActionName(MotionEvent.ACTION_MOVE), x, y));
        logEvent("MOVE", "middle", x, y);
    }

    private void release(float x, float y) {
//        mColorView.finishPath(x, y);
        mCommunicator.send(getCoordinatesJson(getActionName(MotionEvent.ACTION_UP), x, y));
        logEvent("UP", "end", x, y);
    }

    JSONObject response = new JSONObject();
    JSONObject block = new JSONObject();

    private String getCoordinatesJson(String action, float x, float y) {
        try {
            response.put("method", "draw");
            response.put("action", action);
            block.put("x", getFieldX(x));
            block.put("y", getFieldY(y));
            response.put("block", block);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private int getFieldX(float x) {
        return (int) (x / mFieldBlockSize);
    }

    private int getFieldY(float y) {
        return (int) (y / mFieldBlockSize);
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
        try {
            JSONObject response = new JSONObject(message);
            if (response.has("method")) {
                if (response.get("method").equals("init")) {
                    float widthPixels = getResources().getDisplayMetrics().widthPixels;
                    float heightPixels = getResources().getDisplayMetrics().heightPixels;
                    float availablePixels = widthPixels > heightPixels ? heightPixels : widthPixels;

                    int fieldSizeWidth = response.getJSONObject("field").getInt("x");
                    int fieldSizeHeight = response.getJSONObject("field").getInt("y");
                    int requestedFieldSize = fieldSizeHeight > fieldSizeWidth ? fieldSizeHeight : fieldSizeWidth;
                    mFieldBlockSize = availablePixels / requestedFieldSize;

                    console("Base field size: %f", mFieldBlockSize);
                } else if (response.get("method").equals("draw")) {
                    float x = response.getJSONObject("block").getInt("x") * mFieldBlockSize;
                    float y = response.getJSONObject("block").getInt("y") * mFieldBlockSize;
//                    if (!alreadyDrawn(x, y)) {
                    if (mDrawingRunnable == null) {
                        mDrawingRunnable = new DrawingRunnable(mColorView);
                    }
                    mDrawingRunnable.setCoordinates(response.getString("action"), x, y);
                    runOnUiThread(mDrawingRunnable);
//                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    HashMap<Integer, ArrayList<Integer>> field = new HashMap<>();

    private boolean alreadyDrawn(float x, float y) {
        List<Integer> ys;
        if ((ys = field.get(((int) x))) == null) {
            field.put(((int) x), new ArrayList<Integer>());
            ys = field.get(((int) x));
        }
        if (ys.contains((int) y)) {
            return true;
        }
        ys.add((int) y);
        return false;
    }

    static class DrawingRunnable implements Runnable {
        private ColorView mColorView;
        private float y;
        private float x;
        private String action;

        public DrawingRunnable(ColorView view) {
            this.mColorView = view;
        }

        void setCoordinates(String action, float x, float y) {
            this.action = action;
            this.x = x;
            this.y = y;
        }

        @Override
        public void run() {
            switch (action) {
                case "DOWN":
                    mColorView.startPath(x, y);
                    break;
                case "MOVE":
                    mColorView.addToPath(x, y);
                    break;
                case "UP":
                    mColorView.finishPath(x, y);
                    break;
            }
        }
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
