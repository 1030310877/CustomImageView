package joe.com.library.customimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * 更加灵活的ImageView
 * Created by joe on 2017/4/20.
 */
public class CustomImageView extends AppCompatImageView {

    public static final int SHAPE_NORMAL = 0;
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_ROUND = 2;

    private float leftTopCornerRadius = 0f;
    private float rightTopCornerRadius = 0f;
    private float leftBottomCornerRadius = 0f;
    private float rightBottomCornerRadius = 0f;

    private float shadowDepth = 0;
    private int shadowColor;

    private static final int MASK_LEFT = 0x01;
    private static final int MASK_RIGHT = 0x02;
    private static final int MASK_TOP = 0x04;
    private static final int MASK_BOTTOM = 0x08;

    private int borderWidth;
    private int borderColor;
    private int shape;
    //// TODO: 2017/4/20  
    private boolean supportZoomGesture = false;

    private Paint mPaint;
    private Path mPath;

    private boolean isLeftShadow = false;
    private boolean isTopShadow = false;
    private boolean isRightShadow = false;
    private boolean isBottomShadow = false;

    public CustomImageView(Context context) {
        this(context, null);
    }

    public CustomImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //read custom attrs
        int shadowMode = 0;
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
            shadowDepth = a.getDimensionPixelOffset(R.styleable.CustomImageView_shadow_depth, 0);
            shadowColor = a.getColor(R.styleable.CustomImageView_shadow_color, 0x44000000);
            shadowMode = a.getInt(R.styleable.CustomImageView_shadow_mode, 0);

            supportZoomGesture = a.getBoolean(R.styleable.CustomImageView_supportZoomGesture, false);
            a.recycle();
        }
        if (shadowDepth > 0) {
            isLeftShadow = (shadowMode & MASK_LEFT) != 0;
            if (isLeftShadow) {
                setPadding((int) shadowDepth, getPaddingTop(), getPaddingRight(), getPaddingBottom());
            }
            isTopShadow = (shadowMode & MASK_TOP) != 0;
            if (isTopShadow) {
                setPadding(getPaddingLeft(), (int) shadowDepth, getPaddingRight(), getPaddingBottom());
            }
            isRightShadow = (shadowMode & MASK_RIGHT) != 0;
            if (isRightShadow) {
                setPadding(getPaddingLeft(), getPaddingTop(), (int) shadowDepth, getPaddingBottom());
            }
            isBottomShadow = (shadowMode & MASK_BOTTOM) != 0;
            if (isBottomShadow) {
                setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), (int) shadowDepth);
            }
        }
        mPaint = new Paint();
        mPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (shape == SHAPE_NORMAL) {
            super.onDraw(canvas);
            drawNormalBorder(canvas);
            drawShadowNormal(canvas);
        } else if (shape == SHAPE_CIRCLE) {
            drawCircleRegion(canvas);
            super.onDraw(canvas);
            drawCircleBorder(canvas);
        } else if (shape == SHAPE_ROUND) {
            drawRoundRegion(canvas);
            super.onDraw(canvas);
            drawRoundBorder(canvas);
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
            canvas.drawRect(rect, mPaint);
        }
    }

    private void drawCircleRegion(Canvas canvas) {
        mPath.reset();
        float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f;
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
        if (shadowDepth > 0) {
            //画四边阴影
            if (isLeftShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new LinearGradient(0, 0, shadowDepth, 0, Color.TRANSPARENT, shadowColor, Shader
                        .TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(0, getPaddingTop(), shadowDepth, getMeasuredHeight() - getPaddingBottom(), mPaint);
            }
            if (isTopShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new LinearGradient(0, 0, 0, shadowDepth, Color.TRANSPARENT, shadowColor, Shader
                        .TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(getPaddingLeft(), 0, getMeasuredWidth() - getPaddingRight(), shadowDepth, mPaint);
            }
            if (isRightShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new LinearGradient(getMeasuredWidth() - shadowDepth, 0, getMeasuredWidth(), 0,
                        shadowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(getMeasuredWidth() - shadowDepth, getPaddingTop(), getMeasuredWidth(),
                        getMeasuredHeight() - getPaddingBottom(), mPaint);
            }
            if (isBottomShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new LinearGradient(0, getMeasuredHeight() - shadowDepth, 0, getMeasuredHeight(),
                        shadowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(getPaddingLeft(), getMeasuredHeight() - shadowDepth, getMeasuredWidth() -
                        getPaddingRight(), getMeasuredHeight(), mPaint);
            }

            //画四角阴影
            if (isLeftShadow && isTopShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new RadialGradient(getPaddingLeft(), getPaddingTop(), shadowDepth, shadowColor, Color
                        .TRANSPARENT, Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(0, 0, getPaddingLeft(), getPaddingTop(), mPaint);
            }
            if (isLeftShadow && isBottomShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new RadialGradient(getPaddingLeft(), getMeasuredHeight() -
                        getPaddingBottom(), shadowDepth, shadowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(0, getMeasuredHeight() - getPaddingBottom(), getPaddingLeft(), getMeasuredHeight(),
                        mPaint);
            }
            if (isRightShadow && isTopShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new RadialGradient(getMeasuredWidth() - getPaddingRight(), getPaddingTop(),
                        shadowDepth, shadowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(getMeasuredWidth() - getPaddingRight(), 0, getMeasuredWidth(), getPaddingTop(), mPaint);
            }
            if (isRightShadow && isBottomShadow) {
                mPaint.reset();
                mPaint.setAntiAlias(true);
                Shader shader = new RadialGradient(getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() -
                        getPaddingBottom(), shadowDepth, shadowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                canvas.drawRect(getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() - getPaddingBottom(),
                        getMeasuredWidth(), getMeasuredHeight(), mPaint);
            }
        }
    }

}
