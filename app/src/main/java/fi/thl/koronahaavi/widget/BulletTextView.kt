package fi.thl.koronahaavi.widget

import android.content.Context
import android.text.SpannableString
import android.text.style.DrawableMarginSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import fi.thl.koronahaavi.R

/**
 * Uses a DrawableMarginSpan to insert a top aligned bullet symbol before text.
 * Text start position is controlled by icon size and textStartPadding attribute.
 *
 * Attributes: icon, iconColor, textStartPadding
 *
 * Theme style attribute: bulletTextViewStyle
 */
class BulletTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int = R.attr.bulletTextViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var drawableSpan: DrawableMarginSpan? = null

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.BulletTextView,
            defStyleAttr,
            R.style.Widget_Vilkku_BulletTextView) // default style if none found with theme attribute

        if (a.hasValue(R.styleable.BulletTextView_icon)) {
            val iconResId = a.getResourceIdOrThrow(R.styleable.BulletTextView_icon)

            ContextCompat.getDrawable(context, iconResId)?.let { icon ->
                if (a.hasValue(R.styleable.BulletTextView_iconColor)) {
                    icon.setTint(a.getColorOrThrow(R.styleable.BulletTextView_iconColor))
                }

                // Replace text with a DrawableMarginSpan which has the drawable icon.
                // The span uses drawable intrinsic size, so given icon needs to be already sized correctly
                drawableSpan = DrawableMarginSpan(
                    icon,
                    a.getDimensionPixelSize(R.styleable.BulletTextView_textStartPadding, 0)
                )
            }

            text = text // trigger setter
        }

        a.recycle()
    }

    /**
     * Text can be set in constructor, or later through data binding, so we need to apply
     * drawable span here in setter
     */
    override fun setText(text: CharSequence?, type: BufferType?) {
        if (drawableSpan != null) {
            val spannable = SpannableString(text).apply {
                setSpan(drawableSpan, 0, length, 0)
            }
            super.setText(spannable, BufferType.SPANNABLE)
        }
        else {
            super.setText(text, type)
        }
    }
}