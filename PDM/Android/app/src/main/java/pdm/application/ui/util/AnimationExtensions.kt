package pdm.application.ui.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

fun View.fadeIn(duration: Long = 500) {
    alpha = 0f
    visibility = View.VISIBLE
    ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }
}

fun View.slideInFromRight(duration: Long = 300) {
    val distance = 200f
    translationX = distance
    alpha = 0f
    visibility = View.VISIBLE

    ObjectAnimator.ofFloat(this, "translationX", distance, 0f).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }

    ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
        this.duration = duration
        start()
    }
}

fun View.bounceIn(duration: Long = 500) {
    scaleX = 0.3f
    scaleY = 0.3f
    alpha = 0f
    visibility = View.VISIBLE

    ObjectAnimator.ofFloat(this, "scaleX", 0.3f, 1.1f, 0.9f, 1f).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }

    ObjectAnimator.ofFloat(this, "scaleY", 0.3f, 1.1f, 0.9f, 1f).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }

    ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
        this.duration = duration / 2
        start()
    }
}

fun View.rotateFab(isRotated: Boolean): Boolean {
    animate().setDuration(200)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
            }
        })
        .rotation(if (isRotated) 135f else 0f)
    return !isRotated
}

fun View.showFab() {
    visibility = View.VISIBLE
    alpha = 0f
    scaleX = 0.5f
    scaleY = 0.5f
    animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(200)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .start()
}

fun View.hideFab() {
    animate()
        .alpha(0f)
        .scaleX(0.5f)
        .scaleY(0.5f)
        .setDuration(200)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .withEndAction { visibility = View.GONE }
        .start()
}