package fi.thl.koronahaavi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import fi.thl.koronahaavi.R

/**
 * Compound control that programmatically creates ImageViews inside
 * linear layout to represent a step indicator, for example page index of view pager
 */
class StepIndicatorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var stepImages: List<View>

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.StepIndicatorView)
        val numSteps = a.getInteger(R.styleable.StepIndicatorView_numSteps, 0)
        a.recycle()

        stepImages = (0 until numSteps)
                .map {
                    LayoutInflater.from(context)
                            .inflate(R.layout.item_step_indicator, this, false)
                            .also { addView(it) }
                }
    }

    fun setStep (step: Int) {
        stepImages.forEachIndexed { i, image ->
            image.isActivated = i == step
        }
    }
}