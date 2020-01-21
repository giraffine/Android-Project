package giraffine.dimmer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingNotifyLayout extends DialogPreference {

    public static String DEFAULT_LAYOUT = "+01231111";
    private LinearLayout root;
    private ImageView icon;
    private ImageButton up;
    private ImageButton down;
    private ImageButton pause;
    private ImageButton close;
    private String mCurrentOrder = null;
    private float mAlphaDisable = (float) 0.3;

    public SettingNotifyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.notification);
    }

    @Override
    public void onBindDialogView(View view) {
        root = (LinearLayout) view;
        root.setBackgroundColor(Color.DKGRAY);
        ((TextView) root.findViewById(R.id.noti_text)).setText("");
        for (int i = 1; i <= 3; i++)
            for (int j = 0; j < 4; j++)
                root.removeView(view.findViewById(getNotifyButtonID(i, j)));

        icon = (ImageView) root.findViewById(R.id.noti_icon);
        up = (ImageButton) view.findViewById(R.id.noti_0_up);
        down = (ImageButton) view.findViewById(R.id.noti_0_down);
        pause = (ImageButton) view.findViewById(R.id.noti_0_pause);
        close = (ImageButton) view.findViewById(R.id.noti_0_close);

        icon.setOnClickListener(mButtonClickListener);
        up.setOnClickListener(mButtonClickListener);
        down.setOnClickListener(mButtonClickListener);
        pause.setOnClickListener(mButtonClickListener);
        close.setOnClickListener(mButtonClickListener);

        up.setOnLongClickListener(mButtonLongClickListener);
        down.setOnLongClickListener(mButtonLongClickListener);
        pause.setOnLongClickListener(mButtonLongClickListener);
        close.setOnLongClickListener(mButtonLongClickListener);

        root.setOnDragListener(mButtonDragListener);
        mButtonDragListener.init = true;

        mCurrentOrder = Prefs.getNotifyLayout();
        setVisible(icon, mCurrentOrder.startsWith("+"));
        mCurrentOrder = mCurrentOrder.substring(1);
        reorder(mCurrentOrder, true);

        Toast.makeText(getContext(), R.string.pref_notify_layout_hint, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mCurrentOrder = mCurrentOrder.substring(0, 4);
            for (int i = 0; i < 4; i++) {
                ImageButton v = getImageButton(mCurrentOrder.charAt(i));
                if (v.getTag() == null)
                    mCurrentOrder += "1";
                else
                    mCurrentOrder += "0";
            }
            if (icon.getTag() == null)
                mCurrentOrder = "+" + mCurrentOrder;
            else
                mCurrentOrder = "-" + mCurrentOrder;
            Log.e(Dimmer.TAG, "onDialogClosed: mCurrentOrder=" + mCurrentOrder);
            Prefs.setNotifyLayout(mCurrentOrder);
            setSummary(getSummary(mCurrentOrder));
            changeNotifyLayout();
        }
    }

    public void changeNotifyLayout() {
        Intent startServiceIntent = new Intent();
        startServiceIntent.setComponent(DimmerService.COMPONENT);
        startServiceIntent.setAction(DimmerService.LAYOUTCHANGE);
        getContext().startService(startServiceIntent);
    }

    public static String getSummary(String layout) {
        String result = "";
        layout = layout.substring(1);
        for (int i = 0; i < 4; i++) {
            if (layout.charAt(i) == '0') {
                if (layout.charAt(i + 4) == '1')
                    result += "£N    ";
            } else if (layout.charAt(i) == '1') {
                if (layout.charAt(i + 4) == '1')
                    result += "V    ";
            } else if (layout.charAt(i) == '2') {
                if (layout.charAt(i + 4) == '1')
                    result += "II    ";
            } else if (layout.charAt(i) == '3') {
                if (layout.charAt(i + 4) == '1')
                    result += "X    ";
            }
        }
        return result;
    }

    public static int getNotifyButtonID(int position, int button) {
        switch (position) {
            case 0:
                switch (button) {
                    case 0:
                        return R.id.noti_0_up;
                    case 1:
                        return R.id.noti_0_down;
                    case 2:
                        return R.id.noti_0_pause;
                    case 3:
                        return R.id.noti_0_close;
                }
                break;
            case 1:
                switch (button) {
                    case 0:
                        return R.id.noti_1_up;
                    case 1:
                        return R.id.noti_1_down;
                    case 2:
                        return R.id.noti_1_pause;
                    case 3:
                        return R.id.noti_1_close;
                }
                break;
            case 2:
                switch (button) {
                    case 0:
                        return R.id.noti_2_up;
                    case 1:
                        return R.id.noti_2_down;
                    case 2:
                        return R.id.noti_2_pause;
                    case 3:
                        return R.id.noti_2_close;
                }
                break;
            case 3:
                switch (button) {
                    case 0:
                        return R.id.noti_3_up;
                    case 1:
                        return R.id.noti_3_down;
                    case 2:
                        return R.id.noti_3_pause;
                    case 3:
                        return R.id.noti_3_close;
                }
                break;
        }
        return 0;
    }

    private ButtonClickListener mButtonClickListener = new ButtonClickListener();

    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getTag() == null) {
                v.setAlpha(mAlphaDisable);
                v.setTag(new Object());
            } else {
                v.setAlpha((float) 1.0);
                v.setTag(null);
            }
        }
    }

    private ButtonLongClickListener mButtonLongClickListener = new ButtonLongClickListener();

    private class ButtonLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            v.startDrag(null, new MyShadow(v), null, 0);
            return true;
        }
    }

    private class MyShadow extends View.DragShadowBuilder {
        private Drawable mShadow;

        public MyShadow(View v) {
            super(v);
            mShadow = new ColorDrawable(Color.LTGRAY);
        }

        public void onDrawShadow(Canvas canvas) {
            mShadow.draw(canvas);
        }

        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            int width = getView().getWidth();
            int height = getView().getHeight();
            mShadow.setBounds(0, 0, width, height);
            size.set(width, height);
            touch.set(width / 2, height / 2);
        }
    }

    private ButtonDragListener mButtonDragListener = new ButtonDragListener();

    private class ButtonDragListener implements View.OnDragListener {
        public boolean init = true;
        private int mSrcSlot = -1;
        private int mDstSlot = -1;
        private char mItem = 0;
        private String tempOrder = "";
        private int slot;
        private int mPivot = 0;
        private int mGridWidth = 0;

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    if (init)
                        buildDragSlot(mCurrentOrder);
                    slot = getSlot((int) event.getX());
                    Log.d(Dimmer.TAG, "ACTION_DRAG_STARTED slot=" + slot);
                    mSrcSlot = slot;
                    mDstSlot = slot;
                    mItem = mCurrentOrder.charAt(slot);
                    tempOrder = mCurrentOrder.substring(0, slot) + mCurrentOrder.substring(slot + 1, 4);
                    Log.e(Dimmer.TAG, "mCurrentOrder= " + mCurrentOrder + ", tempOrder=" + tempOrder + ", mItem=" + mItem);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d(Dimmer.TAG, "ACTION_DRAG_ENDED");
                    mSrcSlot = -1;
                    mDstSlot = -1;
                    mItem = 0;
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    slot = getSlot((int) event.getX());
                    Log.d(Dimmer.TAG, "ACTION_DRAG_LOCATION : (" + event.getX() + ", " + event.getY() + ") slot=" + slot);
                    if (mDstSlot != slot) {
                        mDstSlot = slot;
                        switch (slot) {
                            case 0:
                                mCurrentOrder = String.valueOf(mItem) + tempOrder;
                                break;
                            case 1:
                                mCurrentOrder = tempOrder.substring(0, 1) + String.valueOf(mItem) + tempOrder.substring(1, 3);
                                break;
                            case 2:
                                mCurrentOrder = tempOrder.substring(0, 2) + String.valueOf(mItem) + tempOrder.substring(2, 3);
                                break;
                            case 3:
                                mCurrentOrder = tempOrder + String.valueOf(mItem);
                                break;
                        }
                        Log.e(Dimmer.TAG, "mCurrentOrder= " + mCurrentOrder + ", tempOrder=" + tempOrder + ", mItem=" + mItem);
                        reorder(mCurrentOrder, false);
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    Log.d(Dimmer.TAG, "ACTION_DROP");
                    break;
            }
            return true;
        }

        private void buildDragSlot(String order) {
            ImageButton pivotView = getImageButton(order.charAt(0));
            mPivot = pivotView.getLeft();
            mGridWidth = pivotView.getWidth();
            Log.e(Dimmer.TAG, "mPivot=" + mPivot + ", mGridWidth=" + mGridWidth);
        }

        private int getSlot(int x) {
            Log.e(Dimmer.TAG, "x=" + x);
            int r = (x - mPivot) / mGridWidth;
            if (r < 0) r = 0;
            if (r > 3) r = 3;
            return r;
        }
    }

    private void reorder(String order, boolean dealVisible) {
        Log.e(Dimmer.TAG, "reorder: " + order + ", dealVisible=" + dealVisible);
        root.removeView(up);
        root.removeView(down);
        root.removeView(pause);
        root.removeView(close);
        for (int i = 0; i < 4; i++) {
            ImageButton v = getImageButton(order.charAt(i));
            root.addView(v);
            if (dealVisible)
                setVisible(v, order.charAt(i + 4) == '1');
        }
    }

    private void setVisible(View v, boolean visible) {
        if (!visible) {
            v.setAlpha(mAlphaDisable);
            v.setTag(new Object());
        } else {
            v.setAlpha((float) 1.0);
            v.setTag(null);
        }
    }

    private ImageButton getImageButton(char c) {
        if (c == '0')
            return up;
        else if (c == '1')
            return down;
        else if (c == '2')
            return pause;
        else if (c == '3')
            return close;
        return null;
    }
}
