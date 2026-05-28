package com.topsky.laserremove.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.blankj.utilcode.util.ColorUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.topsky.laserremove.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//发送转动 0下 1左 2上 3右
public class PanelView extends View implements View.OnTouchListener {
    private final String TAG = "PanelView";
    boolean[] isTouch = new boolean[]{ false, false, false, false };
    private Paint paint = new Paint();
    private int smallCircleRadius = 180;
    private final int strokeWidth = 2;
    private final int halfStrokeWidth = strokeWidth / 2;
    private int arrowLeft = -20;
    private int arrowTop = 280;
    private int arrowRight = 70;
    private int arrowBottom = 450;
    private final int smallCircleRadiusDP = 30;
    private int arrowLeftDP = -15;
    private int arrowTopDP = -68;
    private int arrowRightDP = 19;
    private int arrowBottomDP = -43;

    private final int strokeColor = ColorUtils.getColor(R.color.white_a50);
    private final int bg_color = ColorUtils.getColor(R.color.black_a50);
    private final int pressed_color = ColorUtils.getColor(R.color.white_a50);

    private PanelListener listener;

    public PanelView(Context context) {
        super(context);
        setOnTouchListener(this);
        dpToPx(context);
    }

    public PanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
        dpToPx(context);
    }

    public PanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
        dpToPx(context);
    }

    public PanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnTouchListener(this);
        dpToPx(context);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int area = inWitchArea(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                boolean isInCircle = IsPointInCircle(event.getX(), event.getY());
                if (!isInCircle) break;
                if (area == -1) break;
                isTouch[area] = true;
                invalidate();
                if (listener != null) {
                    listener.onStatesChange(area, true);
                }
                return true;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (area == -1) break;
                isTouch[0] = false;
                isTouch[1] = false;
                isTouch[2] = false;
                isTouch[3] = false;
                invalidate();
                if (listener != null) {
                    listener.onStatesChange(area, false);
                }
                break;
        }
        return false;
    }

    private int inWitchArea(float x1, float y1) {
        int width = getWidth();
        int height = getHeight();
        float x = width / 2.0f;
        float y = height / 2.0f;
        boolean XGreaterThanY = Math.abs((x - x1)) > Math.abs((y - y1));
        if (x1 > x && y1 < y) {
            //第一象限
            if (XGreaterThanY)
                return 3;
            else
                return 2;
        } else if (x1 > x && y1 > y) {
            if (XGreaterThanY)
                return 3;
            else
                return 0;
            //第四象限
        } else if (x1 < x && y1 > y) {
            if (XGreaterThanY)
                return 1;
            else
                return 0;
            //第三象限
        } else if (x1 < x && y1 < y) {
            //第二象限
            if (XGreaterThanY)
                return 1;
            else
                return 2;
        }
        return -1;
    }


    private boolean IsPointInCircle(float x1, float y1) {
        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height);
        //到圆心的距离 是否大于半径。半径是R
        //如O(x,y)点圆心，任意一点P（x1,y1） （x-x1）*(x-x1)+(y-y1)*(y-y1)>R*R 那么在圆外 反之在圆内
        float x = width / 2.0f;
        float y = height / 2.0f;
        float r = radius / 2.0f;
        if (!((x - x1) * (x - x1) + (y - y1) * (y - y1) > smallCircleRadius * smallCircleRadius))
            return false;
        else if (!((x - x1) * (x - x1) + (y - y1) * (y - y1) > r * r))
            return true;        //当前点在圆内
        else
            return false;       //当前点在圆外
    }

    RectF rectF = new RectF();
    RectF rectFStroke = new RectF();
    Bitmap ic_arrow = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_arrow);
    Rect rectFBitmap = new Rect();
    RectF rect = new RectF();

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height);
        paint.setAntiAlias(true);

        rectF.set((width - radius) / 2.0f,
                (height - radius) / 2.0f,
                width - (width - radius) / 2.0f,
                height - (height - radius) / 2.0f);

        rectFStroke.set((width - radius) / 2.0f + halfStrokeWidth,
                (height - radius) / 2.0f + halfStrokeWidth,
                width - (width - radius) / 2.0f - halfStrokeWidth,
                height - (height - radius) / 2.0f - halfStrokeWidth);

        float startAngle = 45.0f; //0° 3点钟方向
        for (int i = 0; i < 4; i++) {
            if (isTouch[i])
                paint.setColor(pressed_color);
            else
                paint.setColor(bg_color);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawArc(rectF, startAngle, 90.0f, true, paint);

            paint.setStrokeWidth(strokeWidth);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(strokeColor);
            canvas.drawArc(rectFStroke, startAngle, 90.0f, true, paint);
            startAngle += 90;
        }

        float bitmapStartAngle = 90f;

        //58 32
        rectFBitmap.set(0,
                0,
                ic_arrow.getWidth(),
                ic_arrow.getHeight());
        rect.set(arrowLeft, arrowTop, arrowRight, arrowBottom);
        Log.i(TAG, rect.toString());
        canvas.save();
        canvas.translate(width / 2.0f, height / 2.0f);
        for (int i = 0; i < 4; i++) {
            canvas.drawBitmap(ic_arrow, rectFBitmap, rect, paint);
            canvas.rotate(bitmapStartAngle);
        }
        canvas.restore();

        /*paint.setColor(white);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width / 2.0f, height / 2.0f, smallCircleRadius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        canvas.drawCircle(width / 2.0f, height / 2.0f, smallCircleRadius - halfStrokeWidth, paint);*/
    }

    private void dpToPx(Context context) {
        /*LogUtils.i(TAG, "small Circle Radius " + SizeUtils.px2dp(smallCircleRadius) + "\r"
                + "stroke Width " + SizeUtils.px2dp(strokeWidth) + "\r"
                + "arrow Left " + SizeUtils.px2dp(arrowLeft) + "\r"
                + "arrow Top " + SizeUtils.px2dp(arrowTop) + "\r"
                + "arrow Right " + SizeUtils.px2dp(arrowRight) + "\r"
                + "arrow Bottom " + SizeUtils.px2dp(arrowBottom));*/
        smallCircleRadius = SizeUtils.dp2px(smallCircleRadiusDP);
        arrowLeft = SizeUtils.dp2px(arrowLeftDP);
        arrowTop = SizeUtils.dp2px(arrowTopDP);
        arrowRight = SizeUtils.dp2px(arrowRightDP);
        arrowBottom = SizeUtils.dp2px(arrowBottomDP);
    }

    public PanelListener getPanelListener() {
        return listener;
    }

    public void setPanelListener(PanelListener listener) {
        this.listener = listener;
    }

    public interface PanelListener {
        void onStatesChange(int direction, boolean isPress);
    }
}
