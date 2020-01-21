package giraffine.dimmer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ColorPickerPreference extends DialogPreference implements ColorPickerView.OnColorChangedListener {

    View mView;
    public static int DEFAULT_COLOR = 0x50FFA757;    // 2700 K
    private int mValue = DEFAULT_COLOR;
    private int targetColor = DEFAULT_COLOR;
    private float mDensity = 0;
    private ColorPickerView mColorPickerView = null;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.e(Dimmer.TAG, "ColorPickerPreference");
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        setDialogLayoutResource(R.layout.color_picker);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
//		Log.e(Dimmer.TAG, "onBindView");
        mView = view;
        mValue = Prefs.getColor();
        targetColor = mValue;
        setPreviewColor();
    }

    @Override
    public void onBindDialogView(View view) {
//		Log.e(Dimmer.TAG, "onBindDialogView");
        mColorPickerView = (ColorPickerView) view.findViewById(R.id.color_picker_view);
        mColorPickerView.setOnColorChangedListener(this);
        mColorPickerView.setColor(mValue);
    }

    private void setPreviewColor() {
        if (mView == null) return;
        ImageView iView = new ImageView(getContext());
        LinearLayout widgetFrameView = ((LinearLayout) mView.findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null) return;
        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                (int) (mDensity * 8),
                widgetFrameView.getPaddingBottom()
        );
        // remove already create preview image
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            widgetFrameView.removeViews(0, count);
        }
        widgetFrameView.addView(iView);
        widgetFrameView.setMinimumWidth(0);
        iView.setBackgroundDrawable(new AlphaPatternDrawable((int) (5 * mDensity)));
        iView.setImageBitmap(getPreviewBitmap());
    }

    private Bitmap getPreviewBitmap() {
        int d = (int) (mDensity * 31); //30dip
        int color = mValue;
        Bitmap bm = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        int w = bm.getWidth();
        int h = bm.getHeight();
        int c = color;
        for (int i = 0; i < w; i++) {
            for (int j = i; j < h; j++) {
                c = (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2) ? Color.GRAY : color;
                bm.setPixel(i, j, c);
                if (i != j) {
                    bm.setPixel(j, i, c);
                }
            }
        }

        return bm;
    }

    @Override
    public void onColorChanged(int color) {
//		Log.e(Dimmer.TAG, "onColorChanged: " + color);
        targetColor = color;
        changeColor(color);
    }

    public void changeColor(int color) {
//		Log.e(Dimmer.TAG, "color=" + Integer.toHexString(color));
        Intent startServiceIntent = new Intent();
        startServiceIntent.setComponent(DimmerService.COMPONENT);
        startServiceIntent.setAction(DimmerService.COLORCHANGE);
        startServiceIntent.putExtra(DimmerService.COLORCHANGE, color);
        getContext().startService(startServiceIntent);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mValue = targetColor;
            setPreviewColor();
            Prefs.setColor(mValue);
        } else {
            changeColor(mValue);
        }
    }

}