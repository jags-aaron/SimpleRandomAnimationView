package com.example.aaronjags.animations;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Created by aaronjags on 2/13/18.
 */
public class FigureAnimationView extends View {

    private final String TAG = this.getClass().getSimpleName();

    private static class Figure {
        private float x;
        private float y;
        private float scale;
        private float alpha;
        private float theta;
        private float speed;
        private float centerX;
        private float centerY;
        private int lifeTime;
        private boolean invertedTrack;
        private boolean renewed = false;
    }

    Paint circle_paint1 = new Paint();
    Paint circle_paint2 = new Paint();
    Paint arc_paint = new Paint();
    float globalTheta = 0;

    /**
     * The TimeAnimator doesn’t actually animate anything itself.
     * It relies on a TimeListener to do the work and is only responsible for calling the
     * TimeListener every time the animation is updated.
     * TimeListener is an interface that follows the observer design pattern.
     * TimeAnimator defines only two public methods.
     */
    private TimeAnimator mTimeAnimator;

    // DP per Second
    private static final float SPEED = 0.02f;
    private static final int COUNT = 15;
    private float mBaseSpeed;
    // Figure Size
    private static final float SCALE_MIN = 0.1f;
    private static final float SCALE_MAX = 0.8f;
    private float imageRealSize;
    // Figure Alpha
    private static final float ALPHA_SCALE_PART = 0.4f;
    private static final float ALPHA_RANDOM_PART = 0.5f;
    // Figures Array
    private final Figure[] mFigures = new Figure[COUNT];
    // Random object
    private final Random mRnd = new Random(1500);

    private Drawable mDrawable;
    private long mCurrentPlayTime;

    public FigureAnimationView(Context context) {
        super(context);
        if (!isInEditMode())
            init();
    }

    public FigureAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode())
            init();
    }

    public FigureAnimationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode())
            init();
    }

    private void init() {
        // Get Image
        mDrawable = ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher);
        // Measure
        imageRealSize = Math.max(mDrawable != null ? mDrawable.getIntrinsicWidth() : 0, mDrawable != null ? mDrawable.getIntrinsicHeight() : 0) / 2f;
        // Get the speed based on screen density
        mBaseSpeed = SPEED * getResources().getDisplayMetrics().density;
    }

    /**
     * onSizeChanged() is called when your view is first assigned a size,
     * and again if the size of your view changes for any reason.
     * Calculate positions, dimensions, and any other values related
     * to your view's size in onSizeChanged(), instead of recalculating them every time you draw
     *
     * @link - https://developer.android.com/training/custom-views/custom-drawing.html
     * @link - https://developer.android.com/reference/android/view/View.html#onSizeChanged(int, int, int, int)
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        // Setting initial values to figures
        for (int i = 0; i < mFigures.length; i++) {
            final Figure figure = new Figure();
            initFigure(figure);
            mFigures[i] = figure;
        }
    }

    /**
     * This is called when the view is attached to a window.
     *
     * @link - https://developer.android.com/reference/android/view/View.html#onAttachedToWindow()
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isInEditMode()) {
            mTimeAnimator = new TimeAnimator();
            // Registering mTimeAnimator with the animation
            mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                    /*
                    * TimeAnimator will call this method onTimeUpdate every time that the animation is updated.
                    *
                    * {animation} - current animation that was updated.
                    * {totalTime} - time in milliseconds that the animation has been running.
                    * {deltaTime} - time in milliseconds that has elapsed since the last update of the animation.
                    *
                    * {View.isLaidOut} Returns true if this view has been through at least one layout since it was last attached to or detached from a window.
                    * */
                    if (!isLaidOut()) {
                        return;
                    }
                    updateFigures(deltaTime);
                    updateDots();
                    invalidate();
                }
            });
            mTimeAnimator.start();
        }
    }

    /**
     * TimeAnimator extends the Animator class.
     * This means that you can still call all of the methods defined in Animator,
     * like control the animation’s flow with start, end, cancel, pause and resume
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mTimeAnimator.cancel();
        mTimeAnimator.setTimeListener(null);
        mTimeAnimator.removeAllListeners();
        mTimeAnimator = null;
    }

    /**
     * Pause the animation if it's running
     */
    public void pause() {
        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            // Store the current play time for later.
            mCurrentPlayTime = mTimeAnimator.getCurrentPlayTime();
            mTimeAnimator.pause();
        }
    }

    /**
     * Resume the animation if not already running
     */
    public void resume() {
        if (mTimeAnimator != null && mTimeAnimator.isPaused()) {
            mTimeAnimator.start();
            // Why set the current play time?
            // TimeAnimator uses timestamps internally to determine the delta given
            // in the TimeListener. When resumed, the next delta received will the whole
            // pause duration, which might cause a huge jank in the animation.
            // By setting the current play time, it will pick of where it left off.
            mTimeAnimator.setCurrentPlayTime(mCurrentPlayTime);
        }
    }

    /**
     * Renders all the figures every time they're modified
     *
     * @link - https://developer.android.com/reference/android/view/View.html#onDraw(android.graphics.Canvas)
     * @link - https://developer.android.com/reference/android/graphics/Canvas.html#restoreToCount(int)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        final int viewHeight = getHeight();
        final int viewWidth = getWidth();

        drawFigures(canvas, viewWidth, viewHeight);
        drawDots(canvas, viewWidth, viewHeight);
    }

    /**
     * Drow the current Dots
     *
     * @param canvas canvas used to draw
     */
    private void drawDots(Canvas canvas, Integer viewWidth, Integer viewHeight) {

        // Rotating the Circles
        canvas.translate(0, dip2px(70));
        canvas.rotate(360 * globalTheta * 0.003f, viewWidth / 2, viewHeight / 2);

        /*
        * Drawing Dotted circles
        * */
        int circleSize = 70;

        circle_paint1.setStrokeWidth(5);
        circle_paint1.setAntiAlias(false);
        circle_paint1.setStyle(Paint.Style.STROKE);
        circle_paint1.setPathEffect(new DashPathEffect(new float[]{dip2px(1.5f), dip2px(10)}, 0));
        canvas.drawCircle(viewWidth / 2, viewHeight / 2, dip2px(dip2px(circleSize)), circle_paint1);

        circle_paint2.setStrokeWidth(5);
        circle_paint2.setAntiAlias(false);
        circle_paint2.setStyle(Paint.Style.STROKE);
        circle_paint2.setPathEffect(new DashPathEffect(new float[]{dip2px(1.5f), dip2px(5)}, 0));
        canvas.drawCircle(viewWidth / 2, viewHeight / 2, dip2px(dip2px(circleSize + 5)), circle_paint2);

        /*
        * Drawing Arc circle
        * */
        arc_paint.setStrokeWidth(2);
        arc_paint.setAntiAlias(false);
        arc_paint.setStyle(Paint.Style.STROKE);
        arc_paint.setPathEffect(new DashPathEffect(new float[]{dip2px(240), dip2px(100)}, 0));
        canvas.drawCircle(viewWidth / 2, viewHeight / 2, dip2px(dip2px(circleSize + 35)), arc_paint);
    }

    /**
     * Drow the current Dots
     *
     * @param canvas     canvas used to draw
     * @param viewHeight current View height
     * @param viewWidth  current View width
     */
    private void drawFigures(Canvas canvas, Integer viewWidth, Integer viewHeight) {
        for (final Figure figure : mFigures) {
            // Saving canvas state
            final int save = canvas.save();

            // Ignore the Figure if it's renewed its values
            if (figure.renewed) {
                figure.renewed = false;
                continue;
            }

            // Moving and rotating the canvas
            canvas.translate(figure.x, figure.y);
            canvas.rotate(360 * figure.theta * figure.scale);

            // Setting Image size and alpha
            final int size = Math.round(figure.scale * imageRealSize);
            mDrawable.setBounds(-size, -size, size, size);
            mDrawable.setAlpha(Math.round(255 * figure.alpha));

            // Draw the figure to the canvas
            mDrawable.draw(canvas);

            // Restore the canvas to it's previous position and rotation
            // Efficient way to pop any calls to save() that happened after the save count reached saveCount.
            canvas.restoreToCount(save);
        }
    }

    /**
     * Modifies every Dot with the next position
     */
    private void updateDots() {
        double step = 0.2f * Math.PI / 20;
        globalTheta += step;
    }

    /**
     * Modifies every Figure with the next position
     *
     * @param deltaMs time delta since the last frame, in millis
     * @link - https://gamedev.stackexchange.com/questions/9607/moving-an-object-in-a-circular-path
     * @link - https://www.mathopenref.com/coordcirclealgorithm.html
     */
    private void updateFigures(float deltaMs) {

        final float deltaSeconds = deltaMs / 100f;
        final int viewWidth = getWidth();
        final int viewHeight = getHeight();

        for (final Figure figure : mFigures) {

            /*
             * Implementing the Parametric equation of a circle
             *
             * theta = angle that will be increased each loop
             * h     = x coordinate of circle center
             * k     = y coordinate of circle center
             * step  = amount to add to theta each time (degrees)
             *
             * var new_x = h + r * Math.cos(theta);
             * var new_y = k - r * Math.sin(theta);
             */
            double step = (figure.speed * deltaSeconds) * Math.PI / 20;
            figure.x = (float) ((figure.centerX) + ((viewWidth / 2) * Math.cos(figure.theta)));
            figure.y = (float) ((figure.centerY) - ((viewWidth / 2) * Math.sin(figure.theta)));

            if (figure.invertedTrack) {
                figure.theta -= step;
            } else {
                figure.theta += step;
            }

            // Removing Figure lifeTime
            figure.lifeTime -= 1;

            // updating figure when it's lifeTime has finished
            if (figure.lifeTime < 0) {
                figure.lifeTime = 0;
                figure.renewed = true;
                initFigure(figure);
            }
        }
    }

    /**
     * Initialize the given figure by randomizing it's position, scale and alpha
     *
     * @param figure the figure to initialize
     */
    private void initFigure(Figure figure) {

        Random mRandom = new Random();

        // Set the Image size
        figure.scale = SCALE_MIN + SCALE_MAX * mRnd.nextFloat();
        // The alpha is determined by the scale of the figure and a random multiplier.
        figure.alpha = ALPHA_SCALE_PART * figure.scale + ALPHA_RANDOM_PART * mRnd.nextFloat();
        // The bigger and brighter a figure is, the faster it moves
        figure.speed = mBaseSpeed * figure.alpha * figure.scale;
        // angle that will be increased each loop
        figure.theta = 0;

        // Set X, Y to a random value within the width and height of the view
        figure.x = mRandom.nextInt(getWidth());
        figure.y = mRandom.nextInt(getHeight());

        /*
         * Set the (X,Y) Circle Center (where the figure is gonna be moving through)
         * (I'm avoiding to draw in the middle X, cuz the card is gonna be obstructing, I draw starting -10 to 10)
         * ---> NOTE: X = 0 is the center {I don't know why hehe, but I will search about it, I promise}
         * - X goes from -150dp to 150dp as maximum and -10dp to 10dp minimum
         * - Y goes from 80dp to 400dp
        */
        figure.centerX = (dip2px(mRandom.nextInt(150 - 10) + 10) * (mRandom.nextBoolean() ? 1 : -1));
        figure.centerY = dip2px(mRandom.nextInt(400 - 80) + 80);

        // randomize the figure lifeTime
        figure.lifeTime = mRandom.nextInt(3000 - 2000) + 1000;
        // This changes the figure animation (left or right direction)
        figure.invertedTrack = mRandom.nextBoolean();
    }

    /**
     * @param dipValue value to be transformed to pixel
     */
    private int dip2px(float dipValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * @param pxValue value to be transformed to dip
     */
    private int px2dip(float pxValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}