package hr.mfilipovic.dolor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final boolean DEBUG = false;
    private ColorView mView;

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
        setupDrawingView();
    }

    private void setupDrawingView() {
        mView = new ColorView(getApplicationContext());
        FrameLayout frameLayout = findViewById(R.id.frame_layout);
        frameLayout.addView(mView);
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
        mView.startPath(x, y);
        logEvent("DOWN", "start", x, y);
    }

    private void moving(float x, float y) {
        mView.addToPath(x, y);
        logEvent("MOVE", "middle", x, y);
    }

    private void release(float x, float y) {
        mView.finishPath(x, y);
        logEvent("UP", "end", x, y);
    }

    private void logEvent(String action, String position, float X, float Y) {
        if (DEBUG) {
            Log.d(TAG, String.format("%s, %s: %f, %f", action, position, X, Y));
        }
    }

    private void logEvent(MotionEvent e) {
        if (DEBUG) {
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
}
