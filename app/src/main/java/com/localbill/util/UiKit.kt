package com.localbill.util

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.localbill.model.Category

object UiKit {

    fun text(
        ctx: Context,
        value: String,
        sizeSp: Float = 14f,
        color: Int = Theme.mainText(ctx),
        bold: Boolean = false,
        gravity: Int = Gravity.LEFT
    ): TextView {
        return TextView(ctx).apply {
            text = value
            setTextSize(sizeSp)
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            this.gravity = gravity
        }
    }

    fun vertical(ctx: Context, paddingDp: Int = 0): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                Theme.dp(ctx, paddingDp), Theme.dp(ctx, paddingDp),
                Theme.dp(ctx, paddingDp), Theme.dp(ctx, paddingDp)
            )
        }
    }

    fun horizontal(ctx: Context): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }
    }

    fun circle(ctx: Context, color: Int, sizeDp: Int): View {
        val dot = View(ctx)
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(color)
        dot.background = d
        dot.layoutParams = ViewGroup.LayoutParams(
            Theme.dp(ctx, sizeDp), Theme.dp(ctx, sizeDp)
        )
        return dot
    }

    /** 彩色圆 + 分类图标 */
    fun catIcon(ctx: Context, cat: Category?, sizeDp: Int): View {
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = circleBg(ctx, cat?.color ?: C.ANT_GRAY, sizeDp)
        }
        if (cat != null) {
            val iv = ImageView(ctx).apply {
                setImageResource(CatIcon.of(cat))
            }
            val s = (sizeDp * 0.58f).toInt()
            wrap.addView(iv, LinearLayout.LayoutParams(Theme.dp(ctx, s), Theme.dp(ctx, s)))
        }
        wrap.layoutParams = ViewGroup.LayoutParams(
            Theme.dp(ctx, sizeDp), Theme.dp(ctx, sizeDp)
        )
        return wrap
    }

    private fun circleBg(ctx: Context, color: Int, sizeDp: Int): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(color)
        return d
    }

    fun rounded(ctx: Context, color: Int, radiusDp: Int): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = Theme.dp(ctx, radiusDp).toFloat()
        d.setColor(color)
        return d
    }
}
