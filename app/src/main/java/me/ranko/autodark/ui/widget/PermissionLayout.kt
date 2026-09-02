package me.ranko.autodark.ui.widget

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import me.ranko.autodark.R

class PermissionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    private var mExpandableLayout: ExpandableLayout
    private var mExpandableButton: CheckedImageView

    private val mTitle: TextView
    private val mDescription: TextView

    private var isExpanded: Boolean = false

    private var mIcon: MaterialCircleIconView

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PermissionLayout, defStyleAttr, 0)
        orientation = VERTICAL

        val container = LayoutInflater.from(context).inflate(R.layout.widget_permission, this, true)

        mIcon = container.findViewById(R.id.icon)
        mIcon.setImageResource(
            a.getResourceId(R.styleable.PermissionLayout_srcIcon, android.R.drawable.ic_btn_speak_now)
        )
        if (a.hasValue(R.styleable.PermissionLayout_iconColor)) {
            mIcon.colorName = requireNotNull(a.getString(R.styleable.PermissionLayout_iconColor))
        }

        mTitle = container.findViewById(R.id.title)
        if (a.hasValue(R.styleable.PermissionLayout_title)) {
            mTitle.text = a.getText(R.styleable.PermissionLayout_title)
        }

        mExpandableButton = container.findViewById(R.id.button)
        if (a.getBoolean(R.styleable.PermissionLayout_expandable, true)) {
            mExpandableButton.setOnClickListener(this)
        } else {
            mExpandableButton.visibility = View.GONE
        }

        mExpandableLayout = container.findViewById(R.id.expandable)
        if (a.hasValue(R.styleable.PermissionLayout_expandDescription)) {
            isExpanded = a.getBoolean(R.styleable.PermissionLayout_expandDescription, false)
            if (isExpanded xor mExpandableLayout.isExpanded) {
                mExpandableLayout.setExpanded(isExpanded, false)
                mExpandableButton.isChecked = isExpanded
            }
        }

        mDescription = container.findViewById(R.id.description)
        if (a.hasValue(R.styleable.PermissionLayout_description)) {
            mDescription.text = a.getText(R.styleable.PermissionLayout_description)
        }
        mDescription.movementMethod = LinkMovementMethod.getInstance()

        a.recycle()
    }

    // Public API — properties provide getTitleIcon()/getTitle()/setTitle(String)/etc. for Java compat
    val titleIcon: MaterialCircleIconView
        get() = mIcon

    var iconColor: String
        get() = mIcon.colorName
        set(value) { mIcon.colorName = value }

    var title: String
        get() = mTitle.text.toString()
        set(value) { mTitle.text = value }

    fun setTitle(@StringRes titleRes: Int) {
        mTitle.setText(titleRes)
    }

    var description: String
        get() = mDescription.text.toString()
        set(value) { mDescription.text = value }

    fun setDescription(@StringRes descriptionRes: Int) {
        mDescription.setText(descriptionRes)
    }

    override fun onClick(v: View?) {
        isExpanded = !isExpanded
        mExpandableButton.isChecked = isExpanded
        mExpandableLayout.isExpanded = isExpanded
    }
}
