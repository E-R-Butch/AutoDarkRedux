package me.ranko.autodark.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import me.ranko.autodark.R

/**
 * MaterialCircleIconView
 *
 * From https://github.com/RikkaApps/Shizuku commit 6615c55fee84558faceae352e0480365cbd1f172
 */
class MaterialCircleIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCircleIconViewStyle,
    defStyleRes: Int = R.style.MaterialCircleIconView
) : ImageView(context, attrs, defStyleAttr, defStyleRes) {

    var iconForegroundChroma: String = "50"
        set(value) {
            field = value
            updateIconForegroundColor()
        }

    var iconBackgroundChroma: String = "50"
        set(value) {
            field = value
            updateIconBackgroundColor()
        }

    var colorName: String = "blue"
        set(value) {
            field = value
            updateIconBackgroundColor()
            updateIconForegroundColor()
        }

    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MaterialCircleIconView, defStyleAttr, defStyleRes
        )

        if (a.hasValue(R.styleable.MaterialCircleIconView_iconBackgroundChroma)) {
            iconBackgroundChroma = a.getString(R.styleable.MaterialCircleIconView_iconBackgroundChroma) ?: "50"
        }
        if (a.hasValue(R.styleable.MaterialCircleIconView_iconForegroundChroma)) {
            iconForegroundChroma = a.getString(R.styleable.MaterialCircleIconView_iconForegroundChroma) ?: "50"
        }
        if (a.hasValue(R.styleable.MaterialCircleIconView_iconColorName)) {
            colorName = a.getString(R.styleable.MaterialCircleIconView_iconColorName) ?: "blue"
        }

        a.recycle()

        // Ensure tints are applied even when setters weren't triggered by defaults
        updateIconBackgroundColor()
        updateIconForegroundColor()
    }

    @get:ColorRes
    val iconForegroundColorResource: Int
        get() = resources.getIdentifier(
            "material_${colorName}_${iconForegroundChroma}", "color", context.packageName
        )

    @get:ColorRes
    val iconBackgroundColorResource: Int
        get() = resources.getIdentifier(
            "material_${colorName}_${iconBackgroundChroma}", "color", context.packageName
        )

    @ColorInt
    fun getIconForegroundColor(): Int = ContextCompat.getColor(context, iconForegroundColorResource)

    @ColorInt
    fun getIconBackgroundColor(): Int = ContextCompat.getColor(context, iconBackgroundColorResource)

    private fun updateIconForegroundColor() {
        imageTintList = ColorStateList.valueOf(getIconForegroundColor())
    }

    private fun updateIconBackgroundColor() {
        backgroundTintList = ColorStateList.valueOf(getIconBackgroundColor())
    }
}
