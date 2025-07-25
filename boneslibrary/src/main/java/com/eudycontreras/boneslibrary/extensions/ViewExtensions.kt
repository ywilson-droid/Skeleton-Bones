package com.eudycontreras.boneslibrary.extensions

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.View.NO_ID
import android.view.ViewGroup
import androidx.core.view.*
import com.eudycontreras.boneslibrary.bindings.addBoneLoader
import com.eudycontreras.boneslibrary.bindings.addSkeletonLoader
import com.eudycontreras.boneslibrary.bindings.getParentSkeletonDrawable
import com.eudycontreras.boneslibrary.doWith
import com.eudycontreras.boneslibrary.framework.bones.BoneDrawable
import com.eudycontreras.boneslibrary.framework.bones.BoneProperties
import com.eudycontreras.boneslibrary.framework.skeletons.SkeletonDrawable
import com.eudycontreras.boneslibrary.properties.Bounds
import com.eudycontreras.boneslibrary.properties.MutableColor
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.lang.ref.WeakReference

/**
 * Copyright (C) 2019 Project X
 *
 * @Project ProjectX
 * @author Eudy Contreras.
 * @since January 2019
 */

internal val View.horizontalMargin: Float
    get() = (this.marginStart + this.marginEnd).toFloat()

internal val View.horizontalPadding: Float
    get() = (this.paddingStart + this.paddingEnd).toFloat()

internal val View.verticalMargin: Float
    get() = (this.marginTop + this.marginBottom).toFloat()

internal val View.verticalPadding: Float
    get() = (this.paddingTop + this.paddingBottom).toFloat()

internal fun View.generateId(): Int {
    return if (id == NO_ID) {
        View.generateViewId().apply {
            id = this
        }
    } else id
}
/**
 * Applies a skeleton drawable to this ViewGroup
 */
fun ViewGroup.applySkeletonDrawable(): SkeletonDrawable {
    return SkeletonDrawable.create(this)
}

/**
 * Applies a bone drawable to this View
 */
fun View.applyBoneDrawable(): BoneDrawable {
    return BoneDrawable.create(this)
}

internal fun View.compareBounds(bounds: Bounds): Int {
    val xDiff = left - bounds.left.toInt()
    val yDiff = top - bounds.top.toInt()
    val widthDiff = measuredWidth - bounds.width.toInt()
    val heightDiff = measuredHeight - bounds.height.toInt()

    return xDiff + yDiff + widthDiff + heightDiff
}

internal fun View.getBounds(): Bounds {
    return Bounds(
        x = this.left.toFloat(),
        y = this.top.toFloat(),
        width = this.measuredWidth.toFloat(),
        height = this.measuredHeight.toFloat()
    )
}

internal fun View.hasValidMinBounds(boneProps: BoneProperties): Boolean {
    boneProps.minHeight?.let {
        if (measuredHeight < it) {
            return false
        }
    }
    boneProps.minWidth?.let {
        if (measuredWidth < it) {
            return false
        }
    }
    return true
}

internal fun View.hasValidBounds(): Boolean {
    return (measuredWidth > 0 && measuredHeight > 0)
}

internal fun View.hasDrawableBounds(boneProps: BoneProperties): Boolean {
    return minimumWidth > 0 || (measuredWidth > 0 && measuredHeight > boneProps.minThickness)
}

internal fun View.getBackgroundColor(): MutableColor? {
    if (backgroundTintList != null) {
        return MutableColor.fromColor(backgroundTintList?.defaultColor)
    } else {
       background?.let {
           if (it is ColorDrawable) {
               return MutableColor.fromColor(it.color)
           } else if (it is GradientDrawable) {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                   if (it.color != null) {
                       return MutableColor.fromColor(it.color?.defaultColor)
                   }
               }
           }
       }
    }
    return null
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> View.getProps(propId: Int): T? {
    val props = getTag(propId)
    if (props != null) {
        if (props is WeakReference<*>) {
            val reference: WeakReference<T> = props as WeakReference<T>
            return reference.get()
        } else if (props is T) {
            return props
        }
    }
    return null
}

internal fun View.hasProps(propId: Int): Boolean {
    val props = getTag(propId)
    if (props != null) {
        return true
    }
    return false
}

internal inline fun <reified T> View.saveProps(propId: Int, props: T?, weak: Boolean = false) {
    val tagValue: Any? = if (weak && props != null) {
        FirebaseCrashlytics.getInstance().recordException(
            Exception("SkeletonBones: saveProps() used with weak ref — propId=$propId, type=${T::class.java.simpleName}")
        )
        WeakReference(props)
    } else {
        FirebaseCrashlytics.getInstance().recordException(
            Exception("SkeletonBones: saveProps() used with strong ref — propId=$propId, type=${T::class.java.simpleName}")
        )
        props
    }
    setTag(propId, tagValue)
}

internal fun View.clearProps(propId: Int) {
    setTag(propId, null)
}

internal tailrec fun View.findParent(criteria: ((parent: View) -> Boolean)? = null): ViewGroup? {
    val parent: ViewGroup? = this.parent as? ViewGroup?

    if (parent != null) {
        if (criteria?.invoke(parent) == true) {
            return parent
        }
    } else {
        return if (criteria != null) {
            null
        } else this as ViewGroup
    }

    return parent.findParent(criteria)
}

internal fun View.findView(criteria: (child: View) -> Boolean): View? {
    if (criteria(this)) {
       return this
    }
    if (this is ViewGroup) {
        for (child in children) {
            val match = child.findView(criteria)
            if (match != null) {
                if (criteria.invoke(match)) {
                    return match
                }
            }
        }
    }
    return null
}

internal fun ViewGroup.descendantViews(predicate: ((view: View) -> Boolean)? = null): List<View> {
    val views = mutableListOf<View>()
    findViews(this, predicate, views)
    return views
}

internal fun ViewGroup.removeAllViews(predicate: ((view: View) -> Boolean)? = null) {
    val views = mutableListOf<View>()
    findViews(this, predicate, views)
    views.forEach {
        it.removeFromHierarchy()
    }
}

internal fun View.removeFromHierarchy() {
    val parent = parent as? ViewGroup?
    parent?.removeView(this)
}

private fun findViews(
    viewGroup: ViewGroup,
    predicate: ((View) -> Boolean)? = null,
    views: MutableList<View>
) {
    viewGroup.children.forEach {
        if (it !is ViewGroup) {
            if (predicate != null) {
                if (predicate(it)) {
                    views.add(it)
                }
            } else {
                views.add(it)
            }
        } else {
            findViews(it, predicate, views)
        }
    }
}

/**
 * @Project Project Bones
 * @author Eudy Contreras
 * @since Feburary 2021
 *
 * Enables skeleton loading for this view and its descendants
 */
fun View.enableSkeletonLoading() = this.toggleSkeletonLoading(true)

/**
 * @Project Project Bones
 * @author Eudy Contreras
 * @since Feburary 2021
 *
 * Disables skeleton loading for this view and its descendants
 */
fun View.disableSkeletonLoading() = this.toggleSkeletonLoading(false)

/**
 * @Project Project Bones
 * @author Eudy Contreras
 * @since Feburary 2021
 *
 * Enables skeleton loading for this view and its descendants
 */
fun ViewGroup.enableSkeletonLoading() = this.toggleSkeletonLoading(true)

/**
 * @Project Project Bones
 * @author Eudy Contreras
 * @since Feburary 2021
 *
 * Disables skeleton loading for this view and its descendants
 */
fun ViewGroup.disableSkeletonLoading() = this.toggleSkeletonLoading(false)

/**
 * @Project Project Bones
 * @author Eudy Contreras
 * @since Feburary 2021
 *
 * Toggles skeleton loading for this view and its descendants
 *
 * @return The bone loader associated with this View or null if known is found
 */
fun View.toggleSkeletonLoading(enabled: Boolean): BoneDrawable? {
    val id = generateId()
    val parent = getParentSkeletonDrawable()
    if (parent != null) {
        parent.getProps().setStateOwner(id, false)
        parent.getProps().getBoneProps(id).apply {
            this.enabled = enabled
        }
        return null
    } else {
        var loaderDrawable: BoneDrawable? = null
        doWith(foreground) {
            if (it is BoneDrawable) {
                it.enabled = enabled
                loaderDrawable = it
            } else {
                loaderDrawable = addBoneLoader(enabled = enabled)
            }
        }
        return loaderDrawable
    }
}

/**
 * @Project Project Bones
 * @author Eudy Contreras
 * @since Feburary 2021
 *
 * Toggles skeleton loading for this view and its descendants
 *
 * @return The skeleton associated with this ViewGroup or null if known is found
 */
fun ViewGroup.toggleSkeletonLoading(enabled: Boolean): SkeletonDrawable? {
    val id = generateId()
    val parent = getParentSkeletonDrawable()
    if (parent != null) {
        parent.getProps().setStateOwner(id, false)
        parent.getProps().getBoneProps(id).apply {
            this.enabled = enabled
        }
        return null
    } else {
        var loaderDrawable: SkeletonDrawable? = null
        doWith(foreground) {
            if (it is SkeletonDrawable) {
                it.enabled = enabled
                loaderDrawable = it
            } else {
                loaderDrawable = addSkeletonLoader(enabled = enabled)
            }
        }
        return loaderDrawable
    }
}