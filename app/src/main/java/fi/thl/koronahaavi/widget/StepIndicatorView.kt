package fi.thl.koronahaavi.widget

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import fi.thl.koronahaavi.R
import kotlin.math.roundToInt

/**
 * Compound control that programmatically creates ImageViews inside
 * linear layout to represent a step indicator, for example page index of view pager
 *
 * Attributes
 * - numSteps: number of step indicators
 * - indicatorSize: dimension size for indicator, also controls spacing between indicators
 */
class StepIndicatorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = R.attr.stepIndicatorViewStyle
) : LinearLayout(context, attrs, defStyleAttr) {

    private var stepImages: List<View>
    private var currentStep: Int? = null
    private val size: Int

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.StepIndicatorView,
            defStyleAttr,
            R.style.Widget_Vilkku_StepIndicatorView  // default style if none found with attribute
        )

        val numSteps = a.getInteger(R.styleable.StepIndicatorView_numSteps, 0)
        size = a.getDimension(R.styleable.StepIndicatorView_indicatorSize, 10f).roundToInt()
        a.recycle()

        // put a transparent shape between images
        dividerDrawable = createDividerShape()
        showDividers = SHOW_DIVIDER_MIDDLE

        stepImages = createImages(numSteps)
    }

    fun setStep (step: Int) {
        stepImages.forEachIndexed { i, image ->
            if (i == step) {
                image.isActivated = true
                currentStep = i
            }
            else {
                image.isActivated = false
            }
        }
    }

    fun setNumSteps(count: Int) {
        removeAllViews()
        stepImages = createImages(count)

        // try to restore current step
        currentStep?.let {
            stepImages.getOrNull(it)?.isActivated = true
        }
    }

    private fun createDividerShape() = ShapeDrawable().apply {
        intrinsicWidth = size
        intrinsicHeight = size
        alpha = 0
    }

    private fun createImages(count: Int): List<View> {
        val images = List(count) {
            ImageView(context).apply {
                layoutParams = LayoutParams(size, size)
                setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.shape_circle, context.theme))
                imageTintList = ResourcesCompat.getColorStateList(resources, R.color.step_indicator_color, context.theme)
            }
        }

        images.forEach { addView(it) }
        return images
    }
}