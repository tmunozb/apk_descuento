package com.farenet.descuentos.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.farenet.descuentos.R;

public class OutlinedTextView extends AppCompatTextView {

    private int strokeColor;
    private float strokeWidthPx;
    private int fillColor;

    public OutlinedTextView(Context context) { super(context); init(null); }
    public OutlinedTextView(Context context, AttributeSet attrs) { super(context, attrs); init(attrs); }
    public OutlinedTextView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(attrs); }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OutlinedTextView);
            strokeColor = a.getColor(R.styleable.OutlinedTextView_strokeColor, 0xFF0E3A66);
            strokeWidthPx = a.getDimension(R.styleable.OutlinedTextView_strokeWidth, getResources().getDisplayMetrics().density * 2f);
            fillColor = a.getColor(R.styleable.OutlinedTextView_fillColor, 0xFF1F2A36);
            a.recycle();
        } else {
            strokeColor = 0xFF0E3A66;
            strokeWidthPx = getResources().getDisplayMetrics().density * 2f;
            fillColor = 0xFF1F2A36;
        }
        setIncludeFontPadding(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // trazo
        Paint p = getPaint();
        int oldColor = p.getColor();
        Paint.Style oldStyle = p.getStyle();
        float oldStroke = p.getStrokeWidth();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidthPx);
        p.setColor(strokeColor);
        super.onDraw(canvas);

        // relleno
        p.setStyle(Paint.Style.FILL);
        p.setColor(fillColor);
        p.setStrokeWidth(oldStroke);
        super.onDraw(canvas);

        // restore (por seguridad)
        p.setColor(oldColor);
        p.setStyle(oldStyle);
    }
}
