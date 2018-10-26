package google.architecture.common.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

public class CommDrawerLayout extends ViewGroup {
    private static final int[] THEME_ATTRS = {
            android.R.attr.colorPrimaryDark
    };

    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    @IntDef({LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_CLOSED, LOCK_MODE_LOCKED_OPEN,
            LOCK_MODE_UNDEFINED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LockMode {}

    public static final int LOCK_MODE_UNLOCKED = 0;
    public static final int LOCK_MODE_LOCKED_CLOSED = 1;
    public static final int LOCK_MODE_LOCKED_OPEN = 2;
    public static final int LOCK_MODE_UNDEFINED = 3;

    @IntDef(value = {Gravity.LEFT, Gravity.RIGHT, GravityCompat.START, GravityCompat.END},
            flag = true)
    @Retention(RetentionPolicy.SOURCE)
    private @interface EdgeGravity {}

    private static final int MIN_DRAWER_MARGIN = 52; // dp
    private static final int DRAWER_ELEVATION = 10; //dp
    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
    private static final int PEEK_DELAY = 160; // ms
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    private static final boolean ALLOW_EDGE_LOCK = false;
    private static final boolean CHILDREN_DISALLOW_INTERCEPT = true;
    private static final float TOUCH_SLOP_SENSITIVITY = 1.f;

    static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.layout_gravity
    };

    static final boolean CAN_HIDE_DESCENDANTS = Build.VERSION.SDK_INT >= 19;
    private static final boolean SET_DRAWER_SHADOW_FROM_ELEVATION =
            Build.VERSION.SDK_INT >= 21;
    private final CommDrawerLayout.ChildAccessibilityDelegate mChildAccessibilityDelegate =
            new CommDrawerLayout.ChildAccessibilityDelegate();

    private float mDrawerElevation;
    private int mMinDrawerMargin;
    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private float mScrimOpacity;
    private Paint mScrimPaint = new Paint();

    private final ViewDragHelper mLeftDragger;
    private final ViewDragHelper mRightDragger;
    private final CommDrawerLayout.ViewDragCallback mLeftCallback;
    private final CommDrawerLayout.ViewDragCallback mRightCallback;
    private int mDrawerState;
    private boolean mInLayout;
    private boolean mFirstLayout = true;

    private @CommDrawerLayout.LockMode
    int mLockModeLeft = LOCK_MODE_UNDEFINED;
    private @CommDrawerLayout.LockMode
    int mLockModeRight = LOCK_MODE_UNDEFINED;
    private @CommDrawerLayout.LockMode
    int mLockModeStart = LOCK_MODE_UNDEFINED;
    private @CommDrawerLayout.LockMode
    int mLockModeEnd = LOCK_MODE_UNDEFINED;

    private boolean mDisallowInterceptRequested;
    private boolean mChildrenCanceledTouch;

    private @Nullable
    CommDrawerLayout.DrawerListener mListener;
    private List<DrawerListener> mListeners;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private Drawable mStatusBarBackground;
    private Drawable mShadowLeftResolved;
    private Drawable mShadowRightResolved;

    private CharSequence mTitleLeft;
    private CharSequence mTitleRight;

    private Object mLastInsets;
    private boolean mDrawStatusBarBackground;

    private Drawable mShadowStart = null;
    private Drawable mShadowEnd = null;
    private Drawable mShadowLeft = null;
    private Drawable mShadowRight = null;

    private final ArrayList<View> mNonDrawerViews;

    public interface DrawerListener {
        void onDrawerSlide(@NonNull View drawerView, float slideOffset);
        void onDrawerOpened(@NonNull View drawerView);
        void onDrawerClosed(@NonNull View drawerView);
        void onDrawerStateChanged(@CommDrawerLayout.State int newState);
    }

    public abstract static class SimpleDrawerListener implements CommDrawerLayout.DrawerListener {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(View drawerView) {
        }

        @Override
        public void onDrawerClosed(View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }
    }

    public CommDrawerLayout(@NonNull Context context) {
        this(context, null);
    }

    public CommDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        final float density = getResources().getDisplayMetrics().density;
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
        final float minVel = MIN_FLING_VELOCITY * density;

        mLeftCallback = new CommDrawerLayout.ViewDragCallback(Gravity.LEFT);
        mRightCallback = new CommDrawerLayout.ViewDragCallback(Gravity.RIGHT);

        mLeftDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mLeftCallback);
        mLeftDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
        mLeftDragger.setMinVelocity(minVel);
        mLeftCallback.setDragger(mLeftDragger);

        mRightDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mRightCallback);
        mRightDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
        mRightDragger.setMinVelocity(minVel);
        mRightCallback.setDragger(mRightDragger);

        // So that we can catch the back button
        setFocusableInTouchMode(true);

        ViewCompat.setImportantForAccessibility(this,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        ViewCompat.setAccessibilityDelegate(this, new CommDrawerLayout.AccessibilityDelegate());
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
        if (ViewCompat.getFitsSystemWindows(this)) {
            if (Build.VERSION.SDK_INT >= 21) {
                setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
                    @TargetApi(21)
                    @Override
                    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                        final CommDrawerLayout CommDrawerLayout = (CommDrawerLayout) view;
                        CommDrawerLayout.setChildInsets(insets, insets.getSystemWindowInsetTop() > 0);
                        return insets.consumeSystemWindowInsets();
                    }
                });
                setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                final TypedArray a = context.obtainStyledAttributes(THEME_ATTRS);
                try {
                    mStatusBarBackground = a.getDrawable(0);
                } finally {
                    a.recycle();
                }
            } else {
                mStatusBarBackground = null;
            }
        }

        mDrawerElevation = DRAWER_ELEVATION * density;

        mNonDrawerViews = new ArrayList<View>();
    }

    public void setDrawerElevation(float elevation) {
        mDrawerElevation = elevation;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (isDrawerView(child)) {
                ViewCompat.setElevation(child, mDrawerElevation);
            }
        }
    }

    public float getDrawerElevation() {
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            return mDrawerElevation;
        }
        return 0f;
    }

    @RestrictTo(LIBRARY_GROUP)
    public void setChildInsets(Object insets, boolean draw) {
        mLastInsets = insets;
        mDrawStatusBarBackground = draw;
        setWillNotDraw(!draw && getBackground() == null);
        requestLayout();
    }

    public void setDrawerShadow(Drawable shadowDrawable, @CommDrawerLayout.EdgeGravity int gravity) {
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            return;
        }
        if ((gravity & GravityCompat.START) == GravityCompat.START) {
            mShadowStart = shadowDrawable;
        } else if ((gravity & GravityCompat.END) == GravityCompat.END) {
            mShadowEnd = shadowDrawable;
        } else if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
            mShadowLeft = shadowDrawable;
        } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
            mShadowRight = shadowDrawable;
        } else {
            return;
        }
        resolveShadowDrawables();
        invalidate();
    }

    public void setDrawerShadow(@DrawableRes int resId, @CommDrawerLayout.EdgeGravity int gravity) {
        setDrawerShadow(ContextCompat.getDrawable(getContext(), resId), gravity);
    }

    public void setScrimColor(@ColorInt int color) {
        mScrimColor = color;
        invalidate();
    }

    @Deprecated
    public void setDrawerListener(CommDrawerLayout.DrawerListener listener) {
        // The logic in this method emulates what we had before support for multiple
        // registered listeners.
        if (mListener != null) {
            removeDrawerListener(mListener);
        }
        if (listener != null) {
            addDrawerListener(listener);
        }
        // Update the deprecated field so that we can remove the passed listener the next
        // time we're called
        mListener = listener;
    }

    public void addDrawerListener(@NonNull CommDrawerLayout.DrawerListener listener) {
        if (listener == null) {
            return;
        }
        if (mListeners == null) {
            mListeners = new ArrayList<DrawerListener>();
        }
        mListeners.add(listener);
    }

    public void removeDrawerListener(@NonNull CommDrawerLayout.DrawerListener listener) {
        if (listener == null) {
            return;
        }
        if (mListeners == null) {
            // This can happen if this method is called before the first call to addDrawerListener
            return;
        }
        mListeners.remove(listener);
    }

    public void setDrawerLockMode(@CommDrawerLayout.LockMode int lockMode) {
        setDrawerLockMode(lockMode, Gravity.LEFT);
        setDrawerLockMode(lockMode, Gravity.RIGHT);
    }

    public void setDrawerLockMode(@CommDrawerLayout.LockMode int lockMode, @CommDrawerLayout.EdgeGravity int edgeGravity) {
        final int absGravity = GravityCompat.getAbsoluteGravity(edgeGravity,
                ViewCompat.getLayoutDirection(this));

        switch (edgeGravity) {
            case Gravity.LEFT:
                mLockModeLeft = lockMode;
                break;
            case Gravity.RIGHT:
                mLockModeRight = lockMode;
                break;
            case GravityCompat.START:
                mLockModeStart = lockMode;
                break;
            case GravityCompat.END:
                mLockModeEnd = lockMode;
                break;
        }

        if (lockMode != LOCK_MODE_UNLOCKED) {
            // Cancel interaction in progress
            final ViewDragHelper helper = absGravity == Gravity.LEFT ? mLeftDragger : mRightDragger;
            helper.cancel();
        }
        switch (lockMode) {
            case LOCK_MODE_LOCKED_OPEN:
                final View toOpen = findDrawerWithGravity(absGravity);
                if (toOpen != null) {
                    openDrawer(toOpen);
                }
                break;
            case LOCK_MODE_LOCKED_CLOSED:
                final View toClose = findDrawerWithGravity(absGravity);
                if (toClose != null) {
                    closeDrawer(toClose);
                }
                break;
            // default: do nothing
        }
    }

    public void setDrawerLockMode(@CommDrawerLayout.LockMode int lockMode, @NonNull View drawerView) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a "
                    + "drawer with appropriate layout_gravity");
        }
        final int gravity = ((CommDrawerLayout.LayoutParams) drawerView.getLayoutParams()).gravity;
        setDrawerLockMode(lockMode, gravity);
    }

    @CommDrawerLayout.LockMode
    public int getDrawerLockMode(@CommDrawerLayout.EdgeGravity int edgeGravity) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);

        switch (edgeGravity) {
            case Gravity.LEFT:
                if (mLockModeLeft != LOCK_MODE_UNDEFINED) {
                    return mLockModeLeft;
                }
                int leftLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeStart : mLockModeEnd;
                if (leftLockMode != LOCK_MODE_UNDEFINED) {
                    return leftLockMode;
                }
                break;
            case Gravity.RIGHT:
                if (mLockModeRight != LOCK_MODE_UNDEFINED) {
                    return mLockModeRight;
                }
                int rightLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeEnd : mLockModeStart;
                if (rightLockMode != LOCK_MODE_UNDEFINED) {
                    return rightLockMode;
                }
                break;
            case GravityCompat.START:
                if (mLockModeStart != LOCK_MODE_UNDEFINED) {
                    return mLockModeStart;
                }
                int startLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeLeft : mLockModeRight;
                if (startLockMode != LOCK_MODE_UNDEFINED) {
                    return startLockMode;
                }
                break;
            case GravityCompat.END:
                if (mLockModeEnd != LOCK_MODE_UNDEFINED) {
                    return mLockModeEnd;
                }
                int endLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeRight : mLockModeLeft;
                if (endLockMode != LOCK_MODE_UNDEFINED) {
                    return endLockMode;
                }
                break;
        }

        return LOCK_MODE_UNLOCKED;
    }

    @CommDrawerLayout.LockMode
    public int getDrawerLockMode(@NonNull View drawerView) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a drawer");
        }
        final int drawerGravity = ((CommDrawerLayout.LayoutParams) drawerView.getLayoutParams()).gravity;
        return getDrawerLockMode(drawerGravity);
    }

    public void setDrawerTitle(@CommDrawerLayout.EdgeGravity int edgeGravity, @Nullable CharSequence title) {
        final int absGravity = GravityCompat.getAbsoluteGravity(
                edgeGravity, ViewCompat.getLayoutDirection(this));
        if (absGravity == Gravity.LEFT) {
            mTitleLeft = title;
        } else if (absGravity == Gravity.RIGHT) {
            mTitleRight = title;
        }
    }

    @Nullable
    public CharSequence getDrawerTitle(@CommDrawerLayout.EdgeGravity int edgeGravity) {
        final int absGravity = GravityCompat.getAbsoluteGravity(
                edgeGravity, ViewCompat.getLayoutDirection(this));
        if (absGravity == Gravity.LEFT) {
            return mTitleLeft;
        } else if (absGravity == Gravity.RIGHT) {
            return mTitleRight;
        }
        return null;
    }

    void updateDrawerState(int forGravity, @CommDrawerLayout.State int activeState, View activeDrawer) {
        final int leftState = mLeftDragger.getViewDragState();
        final int rightState = mRightDragger.getViewDragState();

        final int state;
        if (leftState == STATE_DRAGGING || rightState == STATE_DRAGGING) {
            state = STATE_DRAGGING;
        } else if (leftState == STATE_SETTLING || rightState == STATE_SETTLING) {
            state = STATE_SETTLING;
        } else {
            state = STATE_IDLE;
        }

        if (activeDrawer != null && activeState == STATE_IDLE) {
            final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0) {
                dispatchOnDrawerClosed(activeDrawer);
            } else if (lp.onScreen == 1) {
                dispatchOnDrawerOpened(activeDrawer);
            }
        }

        if (state != mDrawerState) {
            mDrawerState = state;

            if (mListeners != null) {
                // Notify the listeners. Do that from the end of the list so that if a listener
                // removes itself as the result of being called, it won't mess up with our iteration
                int listenerCount = mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    mListeners.get(i).onDrawerStateChanged(state);
                }
            }
        }
    }

    void dispatchOnDrawerClosed(View drawerView) {
        final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & CommDrawerLayout.LayoutParams.FLAG_IS_OPENED) == 1) {
            lp.openState = 0;

            if (mListeners != null) {
                // Notify the listeners. Do that from the end of the list so that if a listener
                // removes itself as the result of being called, it won't mess up with our iteration
                int listenerCount = mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    mListeners.get(i).onDrawerClosed(drawerView);
                }
            }

            updateChildrenImportantForAccessibility(drawerView, false);

            // Only send WINDOW_STATE_CHANGE if the host has window focus. This
            // may change if support for multiple foreground windows (e.g. IME)
            // improves.
            if (hasWindowFocus()) {
                final View rootView = getRootView();
                if (rootView != null) {
                    rootView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                }
            }
        }
    }

    void dispatchOnDrawerOpened(View drawerView) {
        final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & CommDrawerLayout.LayoutParams.FLAG_IS_OPENED) == 0) {
            lp.openState = CommDrawerLayout.LayoutParams.FLAG_IS_OPENED;
            if (mListeners != null) {
                // Notify the listeners. Do that from the end of the list so that if a listener
                // removes itself as the result of being called, it won't mess up with our iteration
                int listenerCount = mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    mListeners.get(i).onDrawerOpened(drawerView);
                }
            }

            updateChildrenImportantForAccessibility(drawerView, true);

            // Only send WINDOW_STATE_CHANGE if the host has window focus.
            if (hasWindowFocus()) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }
        }
    }

    private void updateChildrenImportantForAccessibility(View drawerView, boolean isDrawerOpen) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if ((!isDrawerOpen && !isDrawerView(child)) || (isDrawerOpen && child == drawerView)) {
                // Drawer is closed and this is a content view or this is an
                // open drawer view, so it should be visible.
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            } else {
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        }
    }

    void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (mListeners != null) {
            // Notify the listeners. Do that from the end of the list so that if a listener
            // removes itself as the result of being called, it won't mess up with our iteration
            int listenerCount = mListeners.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                mListeners.get(i).onDrawerSlide(drawerView, slideOffset);
            }
        }
    }

    void setDrawerViewOffset(View drawerView, float slideOffset) {
        final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.onScreen) {
            return;
        }

        lp.onScreen = slideOffset;
        dispatchOnDrawerSlide(drawerView, slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((CommDrawerLayout.LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    int getDrawerViewAbsoluteGravity(View drawerView) {
        final int gravity = ((CommDrawerLayout.LayoutParams) drawerView.getLayoutParams()).gravity;
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
    }

    boolean checkDrawerViewAbsoluteGravity(View drawerView, int checkFor) {
        final int absGravity = getDrawerViewAbsoluteGravity(drawerView);
        return (absGravity & checkFor) == checkFor;
    }

    View findOpenDrawer() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final CommDrawerLayout.LayoutParams childLp = (CommDrawerLayout.LayoutParams) child.getLayoutParams();
            if ((childLp.openState & CommDrawerLayout.LayoutParams.FLAG_IS_OPENED) == 1) {
                return child;
            }
        }
        return null;
    }

    void moveDrawerToOffset(View drawerView, float slideOffset) {
        final float oldOffset = getDrawerViewOffset(drawerView);
        final int width = drawerView.getWidth();
        final int oldPos = (int) (width * oldOffset);
        final int newPos = (int) (width * slideOffset);
        final int dx = newPos - oldPos;

        drawerView.offsetLeftAndRight(
                checkDrawerViewAbsoluteGravity(drawerView, Gravity.LEFT) ? dx : -dx);
        setDrawerViewOffset(drawerView, slideOffset);
    }

    View findDrawerWithGravity(int gravity) {
        final int absHorizGravity = GravityCompat.getAbsoluteGravity(
                gravity, ViewCompat.getLayoutDirection(this)) & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final int childAbsGravity = getDrawerViewAbsoluteGravity(child);
            if ((childAbsGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == absHorizGravity) {
                return child;
            }
        }
        return null;
    }

    static String gravityToString(@CommDrawerLayout.EdgeGravity int gravity) {
        if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
            return "LEFT";
        }
        if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
            return "RIGHT";
        }
        return Integer.toHexString(gravity);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            if (isInEditMode()) {
                // Don't crash the layout editor. Consume all of the space if specified
                // or pick a magic number from thin air otherwise.
                // TODO Better communication with tools of this bogus state.
                // It will crash on a real device.
                if (widthMode == MeasureSpec.AT_MOST) {
                    widthMode = MeasureSpec.EXACTLY;
                } else if (widthMode == MeasureSpec.UNSPECIFIED) {
                    widthMode = MeasureSpec.EXACTLY;
                    widthSize = 300;
                }
                if (heightMode == MeasureSpec.AT_MOST) {
                    heightMode = MeasureSpec.EXACTLY;
                } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                    heightMode = MeasureSpec.EXACTLY;
                    heightSize = 300;
                }
            } else {
                throw new IllegalArgumentException(
                        "CommDrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);

        final boolean applyInsets = mLastInsets != null && ViewCompat.getFitsSystemWindows(this);
        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        // Only one drawer is permitted along each vertical edge (left / right). These two booleans
        // are tracking the presence of the edge drawers.
        boolean hasDrawerOnLeftEdge = false;
        boolean hasDrawerOnRightEdge = false;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) child.getLayoutParams();

            if (applyInsets) {
                final int cgrav = GravityCompat.getAbsoluteGravity(lp.gravity, layoutDirection);
                if (ViewCompat.getFitsSystemWindows(child)) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        WindowInsets wi = (WindowInsets) mLastInsets;
                        if (cgrav == Gravity.LEFT) {
                            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(),
                                    wi.getSystemWindowInsetTop(), 0,
                                    wi.getSystemWindowInsetBottom());
                        } else if (cgrav == Gravity.RIGHT) {
                            wi = wi.replaceSystemWindowInsets(0, wi.getSystemWindowInsetTop(),
                                    wi.getSystemWindowInsetRight(),
                                    wi.getSystemWindowInsetBottom());
                        }
                        child.dispatchApplyWindowInsets(wi);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 21) {
                        WindowInsets wi = (WindowInsets) mLastInsets;
                        if (cgrav == Gravity.LEFT) {
                            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(),
                                    wi.getSystemWindowInsetTop(), 0,
                                    wi.getSystemWindowInsetBottom());
                        } else if (cgrav == Gravity.RIGHT) {
                            wi = wi.replaceSystemWindowInsets(0, wi.getSystemWindowInsetTop(),
                                    wi.getSystemWindowInsetRight(),
                                    wi.getSystemWindowInsetBottom());
                        }
                        lp.leftMargin = wi.getSystemWindowInsetLeft();
                        lp.topMargin = wi.getSystemWindowInsetTop();
                        lp.rightMargin = wi.getSystemWindowInsetRight();
                        lp.bottomMargin = wi.getSystemWindowInsetBottom();
                    }
                }
            }

            if (isContentView(child)) {
                // Content views get measured at exactly the layout's size.
                final int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                        widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                        heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            } else if (isDrawerView(child)) {
                if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
                    if (ViewCompat.getElevation(child) != mDrawerElevation) {
                        ViewCompat.setElevation(child, mDrawerElevation);
                    }
                }
                final @CommDrawerLayout.EdgeGravity int childGravity =
                        getDrawerViewAbsoluteGravity(child) & Gravity.HORIZONTAL_GRAVITY_MASK;
                // Note that the isDrawerView check guarantees that childGravity here is either
                // LEFT or RIGHT
                boolean isLeftEdgeDrawer = (childGravity == Gravity.LEFT);
                if ((isLeftEdgeDrawer && hasDrawerOnLeftEdge)
                        || (!isLeftEdgeDrawer && hasDrawerOnRightEdge)) {
                }
                if (isLeftEdgeDrawer) {
                    hasDrawerOnLeftEdge = true;
                } else {
                    hasDrawerOnRightEdge = true;
                }
                final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                        mMinDrawerMargin + lp.leftMargin + lp.rightMargin,
                        lp.width);
                final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                        lp.topMargin + lp.bottomMargin,
                        lp.height);
                child.measure(drawerWidthSpec, drawerHeightSpec);
            } else {
                throw new IllegalStateException("Child " + child + " at index " + i
                        + " does not have a valid layout_gravity - must be Gravity.LEFT, "
                        + "Gravity.RIGHT or Gravity.NO_GRAVITY");
            }
        }
    }

    private void resolveShadowDrawables() {
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            return;
        }
        mShadowLeftResolved = resolveLeftShadow();
        mShadowRightResolved = resolveRightShadow();
    }

    private Drawable resolveLeftShadow() {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        // Prefer shadows defined with start/end gravity over left and right.
        if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
            if (mShadowStart != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowStart, layoutDirection);
                return mShadowStart;
            }
        } else {
            if (mShadowEnd != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowEnd, layoutDirection);
                return mShadowEnd;
            }
        }
        return mShadowLeft;
    }

    private Drawable resolveRightShadow() {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
            if (mShadowEnd != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowEnd, layoutDirection);
                return mShadowEnd;
            }
        } else {
            if (mShadowStart != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowStart, layoutDirection);
                return mShadowStart;
            }
        }
        return mShadowRight;
    }

    private boolean mirror(Drawable drawable, int layoutDirection) {
        if (drawable == null || !DrawableCompat.isAutoMirrored(drawable)) {
            return false;
        }

        DrawableCompat.setLayoutDirection(drawable, layoutDirection);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        final int width = r - l;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) child.getLayoutParams();

            if (isContentView(child)) {
                child.layout(lp.leftMargin, lp.topMargin,
                        lp.leftMargin + child.getMeasuredWidth(),
                        lp.topMargin + child.getMeasuredHeight());
            } else { // Drawer, if it wasn't onMeasure would have thrown an exception.
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                int childLeft;

                final float newOffset;
                if (checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
                    childLeft = -childWidth + (int) (childWidth * lp.onScreen);
                    newOffset = (float) (childWidth + childLeft) / childWidth;
                } else { // Right; onMeasure checked for us.
                    childLeft = width - (int) (childWidth * lp.onScreen);
                    newOffset = (float) (width - childLeft) / childWidth;
                }

                final boolean changeOffset = newOffset != lp.onScreen;

                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (vgrav) {
                    default:
                    case Gravity.TOP: {
                        child.layout(childLeft, lp.topMargin, childLeft + childWidth,
                                lp.topMargin + childHeight);
                        break;
                    }

                    case Gravity.BOTTOM: {
                        final int height = b - t;
                        child.layout(childLeft,
                                height - lp.bottomMargin - child.getMeasuredHeight(),
                                childLeft + childWidth,
                                height - lp.bottomMargin);
                        break;
                    }

                    case Gravity.CENTER_VERTICAL: {
                        final int height = b - t;
                        int childTop = (height - childHeight) / 2;

                        // Offset for margins. If things don't fit right because of
                        // bad measurement before, oh well.
                        if (childTop < lp.topMargin) {
                            childTop = lp.topMargin;
                        } else if (childTop + childHeight > height - lp.bottomMargin) {
                            childTop = height - lp.bottomMargin - childHeight;
                        }
                        child.layout(childLeft, childTop, childLeft + childWidth,
                                childTop + childHeight);
                        break;
                    }
                }

                if (changeOffset) {
                    setDrawerViewOffset(child, newOffset);
                }

                final int newVisibility = lp.onScreen > 0 ? VISIBLE : INVISIBLE;
                if (child.getVisibility() != newVisibility) {
                    child.setVisibility(newVisibility);
                }
            }
        }
        mInLayout = false;
        mFirstLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            final float onscreen = ((CommDrawerLayout.LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        mScrimOpacity = scrimOpacity;

        boolean leftDraggerSettling = mLeftDragger.continueSettling(true);
        boolean rightDraggerSettling = mRightDragger.continueSettling(true);
        if (leftDraggerSettling || rightDraggerSettling) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    public void setStatusBarBackground(@Nullable Drawable bg) {
        mStatusBarBackground = bg;
        invalidate();
    }

    @Nullable
    public Drawable getStatusBarBackgroundDrawable() {
        return mStatusBarBackground;
    }

    public void setStatusBarBackground(int resId) {
        mStatusBarBackground = resId != 0 ? ContextCompat.getDrawable(getContext(), resId) : null;
        invalidate();
    }

    public void setStatusBarBackgroundColor(@ColorInt int color) {
        mStatusBarBackground = new ColorDrawable(color);
        invalidate();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        resolveShadowDrawables();
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (mDrawStatusBarBackground && mStatusBarBackground != null) {
            final int inset;
            if (Build.VERSION.SDK_INT >= 21) {
                inset = mLastInsets != null
                        ? ((WindowInsets) mLastInsets).getSystemWindowInsetTop() : 0;
            } else {
                inset = 0;
            }
            if (inset > 0) {
                mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                mStatusBarBackground.draw(c);
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int height = getHeight();
        final boolean drawingContent = isContentView(child);
        int clipLeft = 0, clipRight = getWidth();

        final int restoreCount = canvas.save();
        if (drawingContent) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                if (v == child || v.getVisibility() != VISIBLE
                        || !hasOpaqueBackground(v) || !isDrawerView(v)
                        || v.getHeight() < height) {
                    continue;
                }

                if (checkDrawerViewAbsoluteGravity(v, Gravity.LEFT)) {
                    final int vright = v.getRight();
                    if (vright > clipLeft) clipLeft = vright;
                } else {
                    final int vleft = v.getLeft();
                    if (vleft < clipRight) clipRight = vleft;
                }
            }
            canvas.clipRect(clipLeft, 0, clipRight, getHeight());
        }
        final boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreCount);

        if (mScrimOpacity > 0 && drawingContent) {
            final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * mScrimOpacity);
            final int color = imag << 24 | (mScrimColor & 0xffffff);
            mScrimPaint.setColor(color);

            canvas.drawRect(clipLeft, 0, clipRight, getHeight(), mScrimPaint);
        } else if (mShadowLeftResolved != null
                &&  checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
            final int shadowWidth = mShadowLeftResolved.getIntrinsicWidth();
            final int childRight = child.getRight();
            final int drawerPeekDistance = mLeftDragger.getEdgeSize();
            final float alpha =
                    Math.max(0, Math.min((float) childRight / drawerPeekDistance, 1.f));
            mShadowLeftResolved.setBounds(childRight, child.getTop(),
                    childRight + shadowWidth, child.getBottom());
            mShadowLeftResolved.setAlpha((int) (0xff * alpha));
            mShadowLeftResolved.draw(canvas);
        } else if (mShadowRightResolved != null
                &&  checkDrawerViewAbsoluteGravity(child, Gravity.RIGHT)) {
            final int shadowWidth = mShadowRightResolved.getIntrinsicWidth();
            final int childLeft = child.getLeft();
            final int showing = getWidth() - childLeft;
            final int drawerPeekDistance = mRightDragger.getEdgeSize();
            final float alpha =
                    Math.max(0, Math.min((float) showing / drawerPeekDistance, 1.f));
            mShadowRightResolved.setBounds(childLeft - shadowWidth, child.getTop(),
                    childLeft, child.getBottom());
            mShadowRightResolved.setAlpha((int) (0xff * alpha));
            mShadowRightResolved.draw(canvas);
        }
        return result;
    }

    boolean isContentView(View child) {
        return ((CommDrawerLayout.LayoutParams) child.getLayoutParams()).gravity == Gravity.NO_GRAVITY;
    }

    boolean isDrawerView(View child) {
        final int gravity = ((CommDrawerLayout.LayoutParams) child.getLayoutParams()).gravity;
        final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                ViewCompat.getLayoutDirection(child));
        if ((absGravity & Gravity.LEFT) != 0) {
            // This child is a left-edge drawer
            return true;
        }
        if ((absGravity & Gravity.RIGHT) != 0) {
            // This child is a right-edge drawer
            return true;
        }
        return false;
    }

    @SuppressWarnings("ShortCircuitBoolean")
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        // "|" used deliberately here; both methods should be invoked.
        final boolean interceptForDrag = mLeftDragger.shouldInterceptTouchEvent(ev)
                | mRightDragger.shouldInterceptTouchEvent(ev);

        boolean interceptForTap = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (mScrimOpacity > 0) {
                    final View child = mLeftDragger.findTopChildUnder((int) x, (int) y);
                    if (child != null && isContentView(child)) {
                        interceptForTap = true;
                    }
                }
                mDisallowInterceptRequested = false;
                mChildrenCanceledTouch = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // If we cross the touch slop, don't perform the delayed peek for an edge touch.
                if (mLeftDragger.checkTouchSlop(ViewDragHelper.DIRECTION_ALL)) {
                    mLeftCallback.removeCallbacks();
                    mRightCallback.removeCallbacks();
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                closeDrawers(true);
                mDisallowInterceptRequested = false;
                mChildrenCanceledTouch = false;
            }
        }

        return interceptForDrag || interceptForTap || hasPeekingDrawer() || mChildrenCanceledTouch;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mLeftDragger.processTouchEvent(ev);
        mRightDragger.processTouchEvent(ev);

        final int action = ev.getAction();
        boolean wantTouchEvents = true;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                mDisallowInterceptRequested = false;
                mChildrenCanceledTouch = false;
                break;
            }

            case MotionEvent.ACTION_UP: {
                final float x = ev.getX();
                final float y = ev.getY();
                boolean peekingOnly = true;
                final View touchedView = mLeftDragger.findTopChildUnder((int) x, (int) y);
                if (touchedView != null && isContentView(touchedView)) {
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    final int slop = mLeftDragger.getTouchSlop();
                    if (dx * dx + dy * dy < slop * slop) {
                        // Taps close a dimmed open drawer but only if it isn't locked open.
                        final View openDrawer = findOpenDrawer();
                        if (openDrawer != null) {
                            peekingOnly = getDrawerLockMode(openDrawer) == LOCK_MODE_LOCKED_OPEN;
                        }
                    }
                }
                closeDrawers(peekingOnly);
                mDisallowInterceptRequested = false;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                closeDrawers(true);
                mDisallowInterceptRequested = false;
                mChildrenCanceledTouch = false;
                break;
            }
        }

        return wantTouchEvents;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (CHILDREN_DISALLOW_INTERCEPT
                || (!mLeftDragger.isEdgeTouched(ViewDragHelper.EDGE_LEFT)
                && !mRightDragger.isEdgeTouched(ViewDragHelper.EDGE_RIGHT))) {
            // If we have an edge touch we want to skip this and track it for later instead.
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
        mDisallowInterceptRequested = disallowIntercept;
        if (disallowIntercept) {
            closeDrawers(true);
        }
    }

    public void closeDrawers() {
        closeDrawers(false);
    }

    void closeDrawers(boolean peekingOnly) {
        boolean needsInvalidate = false;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) child.getLayoutParams();

            if (!isDrawerView(child) || (peekingOnly && !lp.isPeeking)) {
                continue;
            }

            final int childWidth = child.getWidth();

            if (checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
                needsInvalidate |= mLeftDragger.smoothSlideViewTo(child,
                        -childWidth, child.getTop());
            } else {
                needsInvalidate |= mRightDragger.smoothSlideViewTo(child,
                        getWidth(), child.getTop());
            }

            lp.isPeeking = false;
        }

        mLeftCallback.removeCallbacks();
        mRightCallback.removeCallbacks();

        if (needsInvalidate) {
            invalidate();
        }
    }

    public void openDrawer(@NonNull View drawerView) {
        openDrawer(drawerView, true);
    }

    public void openDrawer(@NonNull View drawerView, boolean animate) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }

        final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) drawerView.getLayoutParams();
        if (mFirstLayout) {
            lp.onScreen = 1.f;
            lp.openState = CommDrawerLayout.LayoutParams.FLAG_IS_OPENED;

            updateChildrenImportantForAccessibility(drawerView, true);
        } else if (animate) {
            lp.openState |= CommDrawerLayout.LayoutParams.FLAG_IS_OPENING;

            if (checkDrawerViewAbsoluteGravity(drawerView, Gravity.LEFT)) {
                mLeftDragger.smoothSlideViewTo(drawerView, 0, drawerView.getTop());
            } else {
                mRightDragger.smoothSlideViewTo(drawerView, getWidth() - drawerView.getWidth(),
                        drawerView.getTop());
            }
        } else {
            moveDrawerToOffset(drawerView, 1.f);
            updateDrawerState(lp.gravity, STATE_IDLE, drawerView);
            drawerView.setVisibility(VISIBLE);
        }
        invalidate();
    }

    public void openDrawer(@CommDrawerLayout.EdgeGravity int gravity) {
        openDrawer(gravity, true);
    }

    public void openDrawer(@CommDrawerLayout.EdgeGravity int gravity, boolean animate) {
        final View drawerView = findDrawerWithGravity(gravity);
        if (drawerView == null) {
            throw new IllegalArgumentException("No drawer view found with gravity "
                    + gravityToString(gravity));
        }
        openDrawer(drawerView, animate);
    }

    public void closeDrawer(@NonNull View drawerView) {
        closeDrawer(drawerView, true);
    }

    public void closeDrawer(@NonNull View drawerView, boolean animate) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }

        final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) drawerView.getLayoutParams();
        if (mFirstLayout) {
            lp.onScreen = 0.f;
            lp.openState = 0;
        } else if (animate) {
            lp.openState |= CommDrawerLayout.LayoutParams.FLAG_IS_CLOSING;

            if (checkDrawerViewAbsoluteGravity(drawerView, Gravity.LEFT)) {
                mLeftDragger.smoothSlideViewTo(drawerView, -drawerView.getWidth(),
                        drawerView.getTop());
            } else {
                mRightDragger.smoothSlideViewTo(drawerView, getWidth(), drawerView.getTop());
            }
        } else {
            moveDrawerToOffset(drawerView, 0.f);
            updateDrawerState(lp.gravity, STATE_IDLE, drawerView);
            drawerView.setVisibility(INVISIBLE);
        }
        invalidate();
    }

    public void closeDrawer(@CommDrawerLayout.EdgeGravity int gravity) {
        closeDrawer(gravity, true);
    }

    public void closeDrawer(@CommDrawerLayout.EdgeGravity int gravity, boolean animate) {
        final View drawerView = findDrawerWithGravity(gravity);
        if (drawerView == null) {
            throw new IllegalArgumentException("No drawer view found with gravity "
                    + gravityToString(gravity));
        }
        closeDrawer(drawerView, animate);
    }

    public boolean isDrawerOpen(@NonNull View drawer) {
        if (!isDrawerView(drawer)) {
            throw new IllegalArgumentException("View " + drawer + " is not a drawer");
        }
        CommDrawerLayout.LayoutParams drawerLp = (CommDrawerLayout.LayoutParams) drawer.getLayoutParams();
        return (drawerLp.openState & CommDrawerLayout.LayoutParams.FLAG_IS_OPENED) == 1;
    }

    public boolean isDrawerOpen(@CommDrawerLayout.EdgeGravity int drawerGravity) {
        final View drawerView = findDrawerWithGravity(drawerGravity);
        if (drawerView != null) {
            return isDrawerOpen(drawerView);
        }
        return false;
    }

    public boolean isDrawerVisible(@NonNull View drawer) {
        if (!isDrawerView(drawer)) {
            throw new IllegalArgumentException("View " + drawer + " is not a drawer");
        }
        return ((CommDrawerLayout.LayoutParams) drawer.getLayoutParams()).onScreen > 0;
    }

    public boolean isDrawerVisible(@CommDrawerLayout.EdgeGravity int drawerGravity) {
        final View drawerView = findDrawerWithGravity(drawerGravity);
        if (drawerView != null) {
            return isDrawerVisible(drawerView);
        }
        return false;
    }

    private boolean hasPeekingDrawer() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) getChildAt(i).getLayoutParams();
            if (lp.isPeeking) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new CommDrawerLayout.LayoutParams(CommDrawerLayout.LayoutParams.MATCH_PARENT, CommDrawerLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CommDrawerLayout.LayoutParams
                ? new CommDrawerLayout.LayoutParams((CommDrawerLayout.LayoutParams) p)
                : p instanceof MarginLayoutParams
                ? new CommDrawerLayout.LayoutParams((MarginLayoutParams) p)
                : new CommDrawerLayout.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CommDrawerLayout.LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CommDrawerLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }

        // Only the views in the open drawers are focusables. Add normal child views when
        // no drawers are opened.
        final int childCount = getChildCount();
        boolean isDrawerOpen = false;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (isDrawerView(child)) {
                if (isDrawerOpen(child)) {
                    isDrawerOpen = true;
                    child.addFocusables(views, direction, focusableMode);
                }
            } else {
                mNonDrawerViews.add(child);
            }
        }

        if (!isDrawerOpen) {
            final int nonDrawerViewsCount = mNonDrawerViews.size();
            for (int i = 0; i < nonDrawerViewsCount; ++i) {
                final View child = mNonDrawerViews.get(i);
                if (child.getVisibility() == View.VISIBLE) {
                    child.addFocusables(views, direction, focusableMode);
                }
            }
        }

        mNonDrawerViews.clear();
    }

    private boolean hasVisibleDrawer() {
        return findVisibleDrawer() != null;
    }

    View findVisibleDrawer() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (isDrawerView(child) && isDrawerVisible(child)) {
                return child;
            }
        }
        return null;
    }

    void cancelChildViewTouch() {
        // Cancel child touches
        if (!mChildrenCanceledTouch) {
            final long now = SystemClock.uptimeMillis();
            final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).dispatchTouchEvent(cancelEvent);
            }
            cancelEvent.recycle();
            mChildrenCanceledTouch = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && hasVisibleDrawer()) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final View visibleDrawer = findVisibleDrawer();
            if (visibleDrawer != null && getDrawerLockMode(visibleDrawer) == LOCK_MODE_UNLOCKED) {
                closeDrawers();
            }
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof CommDrawerLayout.SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final CommDrawerLayout.SavedState ss = (CommDrawerLayout.SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.openDrawerGravity != Gravity.NO_GRAVITY) {
            final View toOpen = findDrawerWithGravity(ss.openDrawerGravity);
            if (toOpen != null) {
                openDrawer(toOpen);
            }
        }

        if (ss.lockModeLeft != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeLeft, Gravity.LEFT);
        }
        if (ss.lockModeRight != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeRight, Gravity.RIGHT);
        }
        if (ss.lockModeStart != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeStart, GravityCompat.START);
        }
        if (ss.lockModeEnd != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeEnd, GravityCompat.END);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final CommDrawerLayout.SavedState ss = new CommDrawerLayout.SavedState(superState);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) child.getLayoutParams();
            // Is the current child fully opened (that is, not closing)?
            boolean isOpenedAndNotClosing = (lp.openState == CommDrawerLayout.LayoutParams.FLAG_IS_OPENED);
            // Is the current child opening?
            boolean isClosedAndOpening = (lp.openState == CommDrawerLayout.LayoutParams.FLAG_IS_OPENING);
            if (isOpenedAndNotClosing || isClosedAndOpening) {
                // If one of the conditions above holds, save the child's gravity
                // so that we open that child during state restore.
                ss.openDrawerGravity = lp.gravity;
                break;
            }
        }

        ss.lockModeLeft = mLockModeLeft;
        ss.lockModeRight = mLockModeRight;
        ss.lockModeStart = mLockModeStart;
        ss.lockModeEnd = mLockModeEnd;

        return ss;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        final View openDrawer = findOpenDrawer();
        if (openDrawer != null || isDrawerView(child)) {
            // A drawer is already open or the new view is a drawer, so the
            // new view should start out hidden.
            ViewCompat.setImportantForAccessibility(child,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        } else {
            // Otherwise this is a content view and no drawer is open, so the
            // new view should start out visible.
            ViewCompat.setImportantForAccessibility(child,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        // We only need a delegate here if the framework doesn't understand
        // NO_HIDE_DESCENDANTS importance.
        if (!CAN_HIDE_DESCENDANTS) {
            ViewCompat.setAccessibilityDelegate(child, mChildAccessibilityDelegate);
        }
    }

    static boolean includeChildForAccessibility(View child) {
        // If the child is not important for accessibility we make
        // sure this hides the entire subtree rooted at it as the
        // IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDATS is not
        // supported on older platforms but we want to hide the entire
        // content and not opened drawers if a drawer is opened.
        return ViewCompat.getImportantForAccessibility(child)
                != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                && ViewCompat.getImportantForAccessibility(child)
                != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
    }

    /**
     * State persisted across instances
     */
    protected static class SavedState extends AbsSavedState {
        int openDrawerGravity = Gravity.NO_GRAVITY;
        @CommDrawerLayout.LockMode
        int lockModeLeft;
        @CommDrawerLayout.LockMode
        int lockModeRight;
        @CommDrawerLayout.LockMode
        int lockModeStart;
        @CommDrawerLayout.LockMode
        int lockModeEnd;

        public SavedState(@NonNull Parcel in, @Nullable ClassLoader loader) {
            super(in, loader);
            openDrawerGravity = in.readInt();
            lockModeLeft = in.readInt();
            lockModeRight = in.readInt();
            lockModeStart = in.readInt();
            lockModeEnd = in.readInt();
        }

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(openDrawerGravity);
            dest.writeInt(lockModeLeft);
            dest.writeInt(lockModeRight);
            dest.writeInt(lockModeStart);
            dest.writeInt(lockModeEnd);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public CommDrawerLayout.SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new CommDrawerLayout.SavedState(in, loader);
            }

            @Override
            public CommDrawerLayout.SavedState createFromParcel(Parcel in) {
                return new CommDrawerLayout.SavedState(in, null);
            }

            @Override
            public CommDrawerLayout.SavedState[] newArray(int size) {
                return new CommDrawerLayout.SavedState[size];
            }
        };
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private final int mAbsGravity;
        private ViewDragHelper mDragger;

        private final Runnable mPeekRunnable = new Runnable() {
            @Override public void run() {
//                peekDrawer();
            }
        };

        ViewDragCallback(int gravity) {
            mAbsGravity = gravity;
        }

        public void setDragger(ViewDragHelper dragger) {
            mDragger = dragger;
        }

        public void removeCallbacks() {
            CommDrawerLayout.this.removeCallbacks(mPeekRunnable);
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // Only capture views where the gravity matches what we're looking for.
            // This lets us use two ViewDragHelpers, one for each side drawer.
            return isDrawerView(child) && checkDrawerViewAbsoluteGravity(child, mAbsGravity)
                    && getDrawerLockMode(child) == LOCK_MODE_UNLOCKED;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateDrawerState(mAbsGravity, state, mDragger.getCapturedView());
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset;
            final int childWidth = changedView.getWidth();

            // This reverses the positioning shown in onLayout.
            if (checkDrawerViewAbsoluteGravity(changedView, Gravity.LEFT)) {
                offset = (float) (childWidth + left) / childWidth;
            } else {
                final int width = getWidth();
                offset = (float) (width - left) / childWidth;
            }
            setDrawerViewOffset(changedView, offset);
            changedView.setVisibility(offset == 0 ? INVISIBLE : VISIBLE);
            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) capturedChild.getLayoutParams();
            lp.isPeeking = false;

            closeOtherDrawer();
        }

        private void closeOtherDrawer() {
            final int otherGrav = mAbsGravity == Gravity.LEFT ? Gravity.RIGHT : Gravity.LEFT;
            final View toClose = findDrawerWithGravity(otherGrav);
            if (toClose != null) {
                closeDrawer(toClose);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            // Offset is how open the drawer is, therefore left/right values
            // are reversed from one another.
            final float offset = getDrawerViewOffset(releasedChild);
            final int childWidth = releasedChild.getWidth();

            int left;
            if (checkDrawerViewAbsoluteGravity(releasedChild, Gravity.LEFT)) {
                left = xvel > 0 || (xvel == 0 && offset > 0.5f) ? 0 : -childWidth;
            } else {
                final int width = getWidth();
                left = xvel < 0 || (xvel == 0 && offset > 0.5f) ? width - childWidth : width;
            }

            mDragger.settleCapturedViewAt(left, releasedChild.getTop());
            invalidate();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            postDelayed(mPeekRunnable, PEEK_DELAY);
        }

        void peekDrawer() {
            final View toCapture;
            final int childLeft;
            final int peekDistance = mDragger.getEdgeSize();
            final boolean leftEdge = mAbsGravity == Gravity.LEFT;
            if (leftEdge) {
                toCapture = findDrawerWithGravity(Gravity.LEFT);
                childLeft = (toCapture != null ? -toCapture.getWidth() : 0) + peekDistance;
            } else {
                toCapture = findDrawerWithGravity(Gravity.RIGHT);
                childLeft = getWidth() - peekDistance;
            }
            // Only peek if it would mean making the drawer more visible and the drawer isn't locked
            if (toCapture != null && ((leftEdge && toCapture.getLeft() < childLeft)
                    || (!leftEdge && toCapture.getLeft() > childLeft))
                    && getDrawerLockMode(toCapture) == LOCK_MODE_UNLOCKED) {
                final CommDrawerLayout.LayoutParams lp = (CommDrawerLayout.LayoutParams) toCapture.getLayoutParams();
                mDragger.smoothSlideViewTo(toCapture, childLeft, toCapture.getTop());
                lp.isPeeking = true;
                invalidate();

                closeOtherDrawer();

                cancelChildViewTouch();
            }
        }

        @Override
        public boolean onEdgeLock(int edgeFlags) {
            if (ALLOW_EDGE_LOCK) {
                final View drawer = findDrawerWithGravity(mAbsGravity);
                if (drawer != null && !isDrawerOpen(drawer)) {
                    closeDrawer(drawer);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            final View toCapture;
            if ((edgeFlags & ViewDragHelper.EDGE_LEFT) == ViewDragHelper.EDGE_LEFT) {
                toCapture = findDrawerWithGravity(Gravity.LEFT);
            } else {
                toCapture = findDrawerWithGravity(Gravity.RIGHT);
            }

            if (toCapture != null && getDrawerLockMode(toCapture) == LOCK_MODE_UNLOCKED) {
                mDragger.captureChildView(toCapture, pointerId);
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return isDrawerView(child) ? child.getWidth() : 0;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
                return Math.max(-child.getWidth(), Math.min(left, 0));
            } else {
                final int width = getWidth();
                return Math.max(width - child.getWidth(), Math.min(left, width));
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        private static final int FLAG_IS_OPENED = 0x1;
        private static final int FLAG_IS_OPENING = 0x2;
        private static final int FLAG_IS_CLOSING = 0x4;

        public int gravity = Gravity.NO_GRAVITY;
        float onScreen;
        boolean isPeeking;
        int openState;

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            this(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(@NonNull CommDrawerLayout.LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull MarginLayoutParams source) {
            super(source);
        }
    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            if (CAN_HIDE_DESCENDANTS) {
                super.onInitializeAccessibilityNodeInfo(host, info);
            } else {
                // Obtain a node for the host, then manually generate the list
                // of children to only include non-obscured views.
                final AccessibilityNodeInfoCompat superNode =
                        AccessibilityNodeInfoCompat.obtain(info);
                super.onInitializeAccessibilityNodeInfo(host, superNode);

                info.setSource(host);
                final ViewParent parent = ViewCompat.getParentForAccessibility(host);
                if (parent instanceof View) {
                    info.setParent((View) parent);
                }
                copyNodeInfoNoChildren(info, superNode);
                superNode.recycle();

                addChildrenForAccessibility(info, (ViewGroup) host);
            }

            info.setClassName(CommDrawerLayout.class.getName());

            // This view reports itself as focusable so that it can intercept
            // the back button, but we should prevent this view from reporting
            // itself as focusable to accessibility services.
            info.setFocusable(false);
            info.setFocused(false);
            info.removeAction(AccessibilityActionCompat.ACTION_FOCUS);
            info.removeAction(AccessibilityActionCompat.ACTION_CLEAR_FOCUS);
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);

            event.setClassName(CommDrawerLayout.class.getName());
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            // Special case to handle window state change events. As far as
            // accessibility services are concerned, state changes from
            // CommDrawerLayout invalidate the entire contents of the screen (like
            // an Activity or Dialog) and they should announce the title of the
            // new content.
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                final List<CharSequence> eventText = event.getText();
                final View visibleDrawer = findVisibleDrawer();
                if (visibleDrawer != null) {
                    final int edgeGravity = getDrawerViewAbsoluteGravity(visibleDrawer);
                    final CharSequence title = getDrawerTitle(edgeGravity);
                    if (title != null) {
                        eventText.add(title);
                    }
                }

                return true;
            }

            return super.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                                                       AccessibilityEvent event) {
            if (CAN_HIDE_DESCENDANTS || includeChildForAccessibility(child)) {
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
            return false;
        }

        private void addChildrenForAccessibility(AccessibilityNodeInfoCompat info, ViewGroup v) {
            final int childCount = v.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = v.getChildAt(i);
                if (includeChildForAccessibility(child)) {
                    info.addChild(child);
                }
            }
        }

        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                                            AccessibilityNodeInfoCompat src) {
            final Rect rect = mTmpRect;

            src.getBoundsInParent(rect);
            dest.setBoundsInParent(rect);

            src.getBoundsInScreen(rect);
            dest.setBoundsInScreen(rect);

            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());

            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());

            dest.addAction(src.getActions());
        }
    }

    static final class ChildAccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public void onInitializeAccessibilityNodeInfo(View child,
                                                      AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(child, info);

            if (!includeChildForAccessibility(child)) {
                // If we are ignoring the sub-tree rooted at the child,
                // break the connection to the rest of the node tree.
                // For details refer to includeChildForAccessibility.
                info.setParent(null);
            }
        }
    }
}