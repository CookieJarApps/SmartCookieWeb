/*
 * Copyright 2016 Anthony Restaino
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cookiegames.smartcookie.browser.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import com.cookiegames.smartcookie.R
import java.util.ArrayDeque
import java.util.Queue
import kotlin.math.abs

@Suppress("unused")
class AnimatedProgressBar : View {
    // State variables
    private var mProgress = 0
    private var mDrawWidth = 0

    // Consumer variables
    private var mProgressColor = 0
    private var mBidirectionalAnimate = true
    private var mAnimationDuration = 0

    // Animation interpolators
    private val mAlphaInterpolator: Interpolator = LinearInterpolator()
    private val mProgressInterpolator: Interpolator = BezierEaseInterpolator()

    private val mAnimationQueue: Queue<Animation> = ArrayDeque()

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    /**
     * Initialize the AnimatedProgressBar
     *
     * @param context is the context passed by the constructor
     * @param attrs   is the attribute set passed by the constructor
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        val array =
            context.theme.obtainStyledAttributes(attrs, R.styleable.AnimatedProgressBar, 0, 0)
        try {
            // Retrieve the style of the progress bar that the user hopefully set
            mProgressColor =
                array.getColor(R.styleable.AnimatedProgressBar_progressColor, Color.RED)
            mBidirectionalAnimate =
                array.getBoolean(R.styleable.AnimatedProgressBar_bidirectionalAnimate, false)
            mAnimationDuration = array.getInteger(
                R.styleable.AnimatedProgressBar_animationDuration,
                PROGRESS_DURATION
            )
        } finally {
            array.recycle()
        }
    }

    /**
     * Sets the duration of the animation that
     * runs on the progress bar.
     *
     * @param duration the duration of the animation,
     * in milliseconds.
     */
    fun setDuration(duration: Int) {
        mAnimationDuration = duration
    }

    /**
     * Sets whether or not the view should animate
     * in both directions, or whether is should only
     * animate up.
     *
     * @param bidirectionalAnimate true to animate in both
     * directions, false to animate
     * only up.
     */
    fun setBidirectionalAnimate(bidirectionalAnimate: Boolean) {
        mBidirectionalAnimate = bidirectionalAnimate
    }

    /**
     * Sets the color that the progress bar will be.
     * Calling this method will trigger a redraw.
     *
     * @param color the color that should be used to draw
     * the progress bar.
     */
    fun setProgressColor(@ColorInt color: Int) {
        mProgressColor = color
        invalidate()
    }

    var progress: Int
        /**
         * Returns the current progress value between 0 and 100
         *
         * @return progress of the view
         */
        get() = mProgress
        /**
         * sets the progress as an integer value between 0 and 100.
         * Values above or below that interval will be adjusted to their
         * nearest value within the interval, i.e. setting a value of 150 will have
         * the effect of setting the progress to 100. You cannot trick us.
         *
         * @param progress an integer between 0 and 100
         */
        set(progress) {
            // Progress cannot be greater than 100
            var progress = progress
            if (progress > MAX_PROGRESS) {
                progress = MAX_PROGRESS
            } else if (progress < 0) {
                // progress cannot be less than 0
                progress = 0
            }

            val width = measuredWidth

            // If the view is not laid out yet, then we can't
            // render the progress, so we post a runnable to
            // the view to set the progress, and return.
            val finalProgress = progress
            if (width == 0 && !ViewCompat.isLaidOut(this)) {
                post { progress = (finalProgress) }

                return
            }

            if (alpha < 1.0f) {
                fadeIn()
            }

            // Set the drawing bounds for the ProgressBar
            mRect.left = 0
            mRect.top = 0
            mRect.bottom = bottom - top
            if (progress < mProgress && !mBidirectionalAnimate) {
                // Reset the view width if it is less than the
                // previous progress and we aren't using bidirectional animation.
                mDrawWidth = 0
            } else if (progress == mProgress) {
                if (progress == MAX_PROGRESS) {
                    fadeOut()
                }
            }

            // Store the current progress
            mProgress = progress

            // Calculate the width delta
            val deltaWidth = (width * mProgress / MAX_PROGRESS) - mDrawWidth

            if (deltaWidth != 0) {
                // Animate the width change
                animateView(mDrawWidth, deltaWidth, width)
            }
        }

    private val mPaint = Paint()
    private val mRect = Rect()

    override fun onDraw(canvas: Canvas) {
        mPaint.color = mProgressColor
        mPaint.strokeWidth = 10f
        mRect.right = mRect.left + mDrawWidth
        canvas.drawRect(mRect, mPaint)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        invalidate()
    }

    /**
     * private method used to create and run the animation used to change the progress
     *
     * @param initialWidth is the width at which the progress starts at
     * @param deltaWidth   is the amount by which the width of the progress view will change
     * @param maxWidth     is the maximum width (total width of the view)
     */
    private fun animateView(initialWidth: Int, deltaWidth: Int, maxWidth: Int) {
        val fill: Animation = ProgressAnimation(initialWidth, deltaWidth, maxWidth)

        fill.duration = mAnimationDuration.toLong()
        fill.interpolator = mProgressInterpolator

        if (!mAnimationQueue.isEmpty()) {
            mAnimationQueue.add(fill)
        } else {
            startAnimation(fill)
        }
    }

    /**
     * fades in the progress bar
     */
    private fun fadeIn() {
        animate().alpha(1f)
            .setDuration(ALPHA_DURATION.toLong())
            .setInterpolator(mAlphaInterpolator)
            .start()
    }

    /**
     * fades out the progress bar
     */
    private fun fadeOut() {
        animate().alpha(0f)
            .setDuration(ALPHA_DURATION.toLong())
            .setInterpolator(mAlphaInterpolator)
            .start()
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        var state: Parcelable? = state
        if (state is Bundle) {
            val bundle = state
            mProgress = bundle.getInt("progressState")
            state = bundle.getParcelable("instanceState")
        }
        super.onRestoreInstanceState(state)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        bundle.putInt("progressState", mProgress)
        return bundle
    }

    private inner class ProgressAnimation(
        private val mInitialWidth: Int,
        private val mDeltaWidth: Int,
        private val mMaxWidth: Int
    ) :
        Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val width = mInitialWidth + (mDeltaWidth * interpolatedTime).toInt()
            if (width <= mMaxWidth) {
                mDrawWidth = width
                invalidate()
            }
            if (abs((1.0f - interpolatedTime).toDouble()) < 0.00001) {
                if (mProgress >= MAX_PROGRESS) {
                    fadeOut()
                }
                if (!mAnimationQueue.isEmpty()) {
                    startAnimation(mAnimationQueue.poll())
                }
            }
        }

        override fun willChangeBounds(): Boolean {
            return false
        }

        override fun willChangeTransformationMatrix(): Boolean {
            return false
        }
    }

    companion object {
        private const val PROGRESS_DURATION = 500
        private const val ALPHA_DURATION = 200

        private const val MAX_PROGRESS = 100
    }
}