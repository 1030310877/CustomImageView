package joe.com.library.customimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * 更加灵活的ImageView
 * Created by joe on 2017/4/20.
 */
public class CustomImageView extends AppCompatImageView implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final int SHAPE_NORMAL = 0;
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_ROUND = 2;

    private float leftTopCornerRadius = 0f;
    private float rightTopCornerRadius = 0f;
    private float leftBottomCornerRadius = 0f;
    private float rightBottomCornerRadius = 0f;

    private int shadowRadius = 0;
    private int shadowColor;
    private int shadow_dx = 0;
    private int shadow_dy = 0;

    private int borderWidth;
    private int borderColor;
    private int shape;

    //// TODO: 2017/4/20
    private boolean supportZoomGesture = false;
    private float mScale = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private Paint mPaint;
    private Path mPath;
    private Matrix zoomMatrix;
    private boolean firstDraw = true;

    public CustomImageView(Context context) {
        this(context, null);
    }

    public CustomImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //read custom attrs
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomImageView);
            shape = a.getInt(R.styleable.CustomImageView_shape, SHAPE_NORMAL);
            borderWidth = a.getDimensionPixelOffset(R.styleable.CustomImageView_borderWidth, 0);
            borderColor = a.getColor(R.styleable.CustomImageView_borderColor, Color.WHITE);
            if (shape == SHAPE_ROUND) {
                leftTopCornerRadius = a.getDimensionPixelOffset(R.styleable.CustomImageView_leftTop_cornerRadius, -1);
                leftBottomCornerRadius = a.getDimensionPixelOffset(R.styleable
                        .CustomImageView_leftBottom_cornerRadius, -1);
                rightTopCornerRadius = a.getDimensionPixelOffset(R.styleable.CustomImageView_rightTop_cornerRadius, -1);
                rightBottomCornerRadius = a.getDimensionPixelOffset(R.styleable
                        .CustomImageView_rightBottom_cornerRadius, -1);
            }
            shadowRadius = a.getDimensionPixelOffset(R.styleable.CustomImageView_shadow_radius, 0);
            shadowColor = a.getColor(R.styleable.CustomImageView_shadow_color, 0x44000000);
            shadow_dx = a.getDimensionPixelOffset(R.styleable.CustomImageView_shadow_dx, 0);
            shadow_dy = a.getDimensionPixelOffset(R.styleable.CustomImageView_shadow_dy, 0);

            supportZoomGesture = a.getBoolean(R.styleable.CustomImageView_supportZoomGesture, false);
            a.recycle();
        }
        mPaint = new Paint();
        mPath = new Path();

        if (shadowRadius > 0) {
            //// TODO: 2017/4/21  并不精确，需要通过偏移量和角度进行计算
            if (shadow_dx == 0) {
                setPadding(shadowRadius, getPaddingTop(), shadowRadius, getPaddingBottom());
            } else if (shadow_dx > 0) {
                int paddingLeft = shadowRadius - shadow_dx;
                if (paddingLeft < 0) {
                    paddingLeft = 0;
                }
                int paddingRight = shadowRadius + shadow_dx;
                setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());
            } else if (shadow_dx < 0) {
                int paddingLeft = shadowRadius - shadow_dx;
                int paddingRight = shadowRadius + shadow_dx;
                if (paddingRight < 0) {
                    paddingRight = 0;
                }
                setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());
            }
            if (shadow_dy == 0) {
                setPadding(getPaddingLeft(), shadowRadius, getPaddingRight(), shadowRadius);
            } else if (shadow_dy > 0) {
                int paddingTop = shadowRadius - shadow_dy;
                if (paddingTop < 0) {
                    paddingTop = 0;
                }
                int paddingBottom = shadowRadius + shadow_dy;
                setPadding(getPaddingLeft(), paddingTop, getPaddingRight(), paddingBottom);
            } else if (shadow_dy < 0) {
                int paddingTop = shadowRadius - shadow_dy;
                int paddingBottom = shadowRadius + shadow_dy;
                if (paddingBottom < 0) {
                    paddingBottom = 0;
                }
                setPadding(getPaddingLeft(), paddingTop, getPaddingRight(), paddingBottom);
            }
        }

        if (supportZoomGesture) {
            setScaleType(ScaleType.MATRIX);
            initGestureDetector();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    private void initGestureDetector() {
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener
                () {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mScale *= detector.getScaleFactor();

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return supportZoomGesture;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (shape == SHAPE_NORMAL) {
            drawShadowNormal(canvas);
            super.onDraw(canvas);
            drawNormalBorder(canvas);
        } else if (shape == SHAPE_CIRCLE) {
            drawShadowCircle(canvas);
            canvas.save();
            drawCircleRegion(canvas);
            super.onDraw(canvas);
            canvas.restore();
            drawCircleBorder(canvas);
        } else if (shape == SHAPE_ROUND) {
            drawShadowRound(canvas);
            canvas.save();
            drawRoundRegion(canvas);
            super.onDraw(canvas);
            canvas.restore();
            drawRoundBorder(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (supportZoomGesture) {
            if (scaleGestureDetector != null) {
                return scaleGestureDetector.onTouchEvent(event);
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (getDrawable() != drawable) {
            firstDraw = true;
        }
        super.setImageDrawable(drawable);
    }

    @Override
    public void onGlobalLayout() {
        if (firstDraw && supportZoomGesture) {
            Drawable drawable = getDrawable();
            if (drawable == null)
                return;
            Log.d("CustomImageView", "onGlobalLayout: " + drawable.getIntrinsicWidth() + ":" + drawable
                    .getIntrinsicHeight());
            int width = getWidth();
            int height = getHeight();
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHegiht = drawable.getIntrinsicHeight();
            mScale = 1.0f;
            firstDraw = false;
        }
    }

    private void drawNormalBorder(Canvas canvas) {
        if (borderWidth > 0) {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setColor(borderColor);
            mPaint.setStrokeWidth(borderWidth);
            mPaint.setStyle(Paint.Style.STROKE);
            Rect rect = new Rect();
            getDrawingRect(rect);
            rect.left += getPaddingLeft();
            rect.top += getPaddingTop();
            rect.right -= getPaddingRight();
            rect.bottom -= getPaddingBottom();
            canvas.drawRect(rect, mPaint);
        }
    }

    private void drawCircleRegion(Canvas canvas) {
        mPath.reset();
        float radius = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), getMeasuredHeight() -
                getPaddingTop() - getPaddingBottom()) / 2f;
        mPath.addCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2, radius, Path.Direction.CCW);
        canvas.clipPath(mPath, Region.Op.INTERSECT);
    }

    private void drawCircleBorder(Canvas canvas) {
        if (borderWidth > 0) {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setColor(borderColor);
            mPaint.setStrokeWidth(borderWidth);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(mPath, mPaint);
        }
    }

    private void drawRoundRegion(Canvas canvas) {
        RectF rectF = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
        rectF.left += (getPaddingLeft() + 1);
        rectF.top += (getPaddingTop() + 1);
        rectF.right -= (getPaddingRight() + 1);
        rectF.bottom -= (getPaddingBottom() + 1);

        mPath.reset();
        float[] radii = new float[8];
        if (leftTopCornerRadius <= 0) {
            radii[0] = 0;
            radii[1] = 0;
        } else {
            radii[0] = leftTopCornerRadius;
            radii[1] = leftTopCornerRadius;
        }
        if (rightTopCornerRadius <= 0) {
            radii[2] = 0;
            radii[3] = 0;
        } else {
            radii[2] = rightTopCornerRadius;
            radii[3] = rightTopCornerRadius;
        }
        if (rightBottomCornerRadius <= 0) {
            radii[4] = 0;
            radii[5] = 0;
        } else {
            radii[4] = rightBottomCornerRadius;
            radii[5] = rightBottomCornerRadius;
        }
        if (leftBottomCornerRadius <= 0) {
            radii[6] = 0;
            radii[7] = 0;
        } else {
            radii[6] = leftBottomCornerRadius;
            radii[7] = leftBottomCornerRadius;
        }
        mPath.addRoundRect(rectF, radii, Path.Direction.CCW);
        canvas.clipPath(mPath, Region.Op.INTERSECT);
    }

    private void drawRoundBorder(Canvas canvas) {
        if (borderWidth > 0) {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setColor(borderColor);
            mPaint.setStrokeWidth(borderWidth);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(mPath, mPaint);
        }
    }

    private void drawShadowNormal(Canvas canvas) {
        if (shadowRadius > 0) {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShadowLayer(shadowRadius, shadow_dx, shadow_dy, shadowColor);
            setLayerType(View.LAYER_TYPE_SOFTWARE, mPaint);
            canvas.drawRect(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(),
                    getMeasuredHeight() - getPaddingBottom(), mPaint);
        }
    }

    private void drawShadowCircle(Canvas canvas) {
        if (shadowRadius > 0) {
            mPath.reset();
            float radius = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), getMeasuredHeight() -
                    getPaddingTop() - getPaddingBottom()) / 2f;
            mPath.addCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2, radius, Path.Direction.CCW);

            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShadowLayer(shadowRadius, shadow_dx, shadow_dy, shadowColor);
            setLayerType(View.LAYER_TYPE_SOFTWARE, mPaint);
            canvas.drawPath(mPath, mPaint);
        }
    }

    private void drawShadowRound(Canvas canvas) {
        if (shadowRadius > 0) {
            RectF rectF = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
            rectF.left += getPaddingLeft();
            rectF.top += getPaddingTop();
            rectF.right -= getPaddingRight();
            rectF.bottom -= getPaddingBottom();

            mPath.reset();
            float[] radii = new float[8];
            if (leftTopCornerRadius <= 0) {
                radii[0] = 0;
                radii[1] = 0;
            } else {
                radii[0] = leftTopCornerRadius;
                radii[1] = leftTopCornerRadius;
            }
            if (rightTopCornerRadius <= 0) {
                radii[2] = 0;
                radii[3] = 0;
            } else {
                radii[2] = rightTopCornerRadius;
                radii[3] = rightTopCornerRadius;
            }
            if (rightBottomCornerRadius <= 0) {
                radii[4] = 0;
                radii[5] = 0;
            } else {
                radii[4] = rightBottomCornerRadius;
                radii[5] = rightBottomCornerRadius;
            }
            if (leftBottomCornerRadius <= 0) {
                radii[6] = 0;
                radii[7] = 0;
            } else {
                radii[6] = leftBottomCornerRadius;
                radii[7] = leftBottomCornerRadius;
            }
            mPath.addRoundRect(rectF, radii, Path.Direction.CCW);

            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShadowLayer(shadowRadius, shadow_dx, shadow_dy, shadowColor);
            setLayerType(View.LAYER_TYPE_SOFTWARE, mPaint);
            canvas.drawPath(mPath, mPaint);
        }
    }
}
