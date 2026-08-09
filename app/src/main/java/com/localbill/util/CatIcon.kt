package com.localbill.util

import com.localbill.App
import com.localbill.R
import com.localbill.model.Category

/** 分类 → 图标映射。子分类使用其一级分类的图标，未匹配到的用默认图标 */
object CatIcon {

    fun of(cat: Category): Int {
        val topName = if (cat.parent == 0L) {
            cat.name
        } else {
            App.db.categoryById(cat.parent)?.name ?: cat.name
        }
        return byName(topName)
    }

    fun byName(name: String): Int = when (name) {
        "餐饮" -> R.drawable.ic_cat_food
        "交通" -> R.drawable.ic_cat_car
        "购物" -> R.drawable.ic_cat_shopping
        "居住" -> R.drawable.ic_cat_home
        "医疗" -> R.drawable.ic_cat_medical
        "娱乐" -> R.drawable.ic_cat_game
        "通讯" -> R.drawable.ic_cat_phone
        "人情" -> R.drawable.ic_cat_gift
        "学习" -> R.drawable.ic_cat_study
        "工资" -> R.drawable.ic_cat_salary
        "兼职" -> R.drawable.ic_cat_work
        "理财" -> R.drawable.ic_cat_finance
        "红包" -> R.drawable.ic_cat_redpacket
        "报销" -> R.drawable.ic_cat_receipt
        "其他收入" -> R.drawable.ic_cat_income
        "其他" -> R.drawable.ic_cat_other
        else -> R.drawable.ic_cat_other
    }
}
