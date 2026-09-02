package me.ranko.autodark.ui.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import me.ranko.autodark.R

/**
 * ExpandableLayout
 *
 * From https://github.com/RikkaApps/Shizuku commit 6615c55fee84558faceae352e0480365cbd1f172
 */
class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var mHeight = 0
    private var mAnimHeight = 0
    private var mLastAnimHeight = 0
    private var mExpanded: Boolean
    private var mAnimating = false
    private var mValueAnimator: ValueAnimator? = null

    private var mOnHeightUpdatedListener: OnHeightUpdatedListener? = null

    fun interface OnHeightUpdatedListener {
        fun OnHeightUpdate(v: ExpandableLayout, height: Int, changed: Int)
    }

    fun setOnHeightUpdatedListener(listener: OnHeightUpdatedListener?) {
        mOnHeightUpdatedListener = listener
    }

    fun setOnHeightUpdatedListener(listener: (ExpandableLayout, Int, Int) -> Unit) {
        mOnHeightUpdatedListener = OnHeightUpdatedListener { v, h, c -> listener(v, h, c) }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout, defStyleAttr, 0)
        mExpanded = a.getBoolean(R.styleable.ExpandableLayout_isExpanded, false)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (isInEditMode) {
            return
        }

        mHeight = measuredHeight

        if (mAnimating) {
            setMeasuredDimension(measuredWidth, mAnimHeight)
        } else if (!mExpanded) {
            setMeasuredDimension(measuredWidth, 0)
        }
    }

    fun toggle() {
        isExpanded = !mExpanded
    }

    var isExpanded: Boolean
        get() = mExpanded
        set(expanded) {
            setExpanded(expanded, true)
        }

    fun setExpanded(expanded: Boolean, anim: Boolean) {
        if (mExpanded == expanded) {
            return
        }

        mExpanded = expanded

        if (mAnimating) {
            mValueAnimator?.cancel()
        }

        if (!anim) {
            mAnimating = false
            requestLayout()
            return
        }

        val from: Int
        val to: Int
        if (mExpanded) {
            from = 0
            to = mHeight
        } else {
            from = mHeight
            to = 0
        }

        mLastAnimHeight = from
        mValueAnimator = ValueAnimator.ofInt(from, to).apply {
            duration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            addUpdateListener { animation ->
                mAnimHeight = animation.animatedValue as Int
                requestLayout()

                mOnHeightUpdatedListener?.OnHeightUpdate(this@ExpandableLayout, mAnimHeight, mAnimHeight - mLastAnimHeight)

                mLastAnimHeight = mAnimHeight
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    mAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    mOnHeightUpdatedListener?.OnHeightUpdate(this@ExpandableLayout, mHeight, mHeight - mLastAnimHeight)
                    mAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    // Keep Java-style getter/setter for binary compatibility
    fun getIsExpanded(): Boolean = mExpanded
}
