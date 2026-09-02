package me.ranko.autodark.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class MeasuredViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var height = 0
        val childWidthSpec = MeasureSpec.makeMeasureSpec(
            maxOf(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight),
            MeasureSpec.getMode(widthMeasureSpec)
        )
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(childWidthSpec, MeasureSpec.UNSPECIFIED)
            val h = child.measuredHeight
            if (h > height) height = h
        }

        var finalHeightSpec = heightMeasureSpec
        if (height != 0) {
            finalHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, finalHeightSpec)
    }
}
