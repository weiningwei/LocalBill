package com.localbill.util

import com.localbill.App
import com.localbill.R
import com.localbill.model.Category

/**
 * 分类 → 图标映射。
 * 优先按分类自身名称匹配（二级分类有独立图标）；
 * 未命中的二级分类回退到其一级分类的图标，仍未命中用默认图标。
 */
object CatIcon {

    fun of(cat: Category): Int {
        byNameOrNull(cat.name)?.let { return it }
        if (cat.parent != 0L) {
            val topName = App.db.categoryById(cat.parent)?.name
            if (topName != null) byNameOrNull(topName)?.let { return it }
        }
        return R.drawable.ic_cat_other
    }

    fun byName(name: String): Int = byNameOrNull(name) ?: R.drawable.ic_cat_other

    private fun byNameOrNull(name: String): Int? = when (name) {
        // 一级分类
        "餐饮" -> R.drawable.ic_cat_food
        "交通" -> R.drawable.ic_cat_car
        "购物" -> R.drawable.ic_cat_shopping
        "住宿", "居住" -> R.drawable.ic_cat_home // 「居住」为旧库别名
        "日常" -> R.drawable.ic_cat_daily
        "医疗" -> R.drawable.ic_cat_medical
        "娱乐" -> R.drawable.ic_cat_game
        "美妆" -> R.drawable.ic_cat_beauty
        "旅游" -> R.drawable.ic_cat_travel
        "会员租用" -> R.drawable.ic_cat_member
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

        // 餐饮
        "三餐" -> R.drawable.ic_sub_meals
        "外卖" -> R.drawable.ic_sub_takeout
        "夜宵" -> R.drawable.ic_sub_supper
        "奶茶" -> R.drawable.ic_sub_milktea
        "咖啡" -> R.drawable.ic_sub_coffee
        "零食" -> R.drawable.ic_sub_snack
        "水果" -> R.drawable.ic_sub_fruit
        "食材" -> R.drawable.ic_sub_grocery
        "柴米油盐" -> R.drawable.ic_sub_seasoning
        "烟酒" -> R.drawable.ic_sub_wine

        // 购物
        "超市" -> R.drawable.ic_sub_supermarket
        "鞋服" -> R.drawable.ic_sub_clothes
        "数码" -> R.drawable.ic_sub_digital
        "电器" -> R.drawable.ic_sub_appliance
        "家居" -> R.drawable.ic_sub_furniture
        "厨房用品" -> R.drawable.ic_sub_kitchen
        "包包" -> R.drawable.ic_sub_bag
        "日用百货" -> R.drawable.ic_sub_dailygoods
        "图书文具" -> R.drawable.ic_sub_stationery

        // 交通
        "公交地铁" -> R.drawable.ic_sub_bus
        "打车" -> R.drawable.ic_sub_taxi
        "共享单车" -> R.drawable.ic_sub_bike
        "私家车" -> R.drawable.ic_sub_car
        "火车" -> R.drawable.ic_sub_train
        "大巴" -> R.drawable.ic_sub_coach
        "飞机" -> R.drawable.ic_sub_plane
        "加油" -> R.drawable.ic_sub_gas
        "充电" -> R.drawable.ic_sub_charge
        "停车" -> R.drawable.ic_sub_parking
        "维修保养" -> R.drawable.ic_sub_repaircar

        // 住宿
        "房租" -> R.drawable.ic_sub_rent
        "房贷" -> R.drawable.ic_sub_mortgage
        "水费" -> R.drawable.ic_sub_water
        "电费" -> R.drawable.ic_sub_electricity
        "燃气" -> R.drawable.ic_sub_gasfuel
        "物业" -> R.drawable.ic_sub_property
        "维修" -> R.drawable.ic_sub_fix
        "装修" -> R.drawable.ic_sub_decorate

        // 日常
        "快递" -> R.drawable.ic_sub_express
        "理发" -> R.drawable.ic_sub_haircut
        "日用杂货" -> R.drawable.ic_sub_hanger

        // 学习
        "网课" -> R.drawable.ic_sub_online
        "书籍" -> R.drawable.ic_sub_book
        "培训" -> R.drawable.ic_sub_training
        "学费" -> R.drawable.ic_sub_tuition
        "考试报名" -> R.drawable.ic_sub_exam

        // 人情
        "送礼" -> R.drawable.ic_sub_giftsend
        "发红包" -> R.drawable.ic_cat_redpacket
        "孝心" -> R.drawable.ic_sub_filial
        "请客" -> R.drawable.ic_sub_treat
        "亲密付" -> R.drawable.ic_sub_intimate
        "随礼" -> R.drawable.ic_sub_cashgift

        // 娱乐
        "电影" -> R.drawable.ic_sub_movie
        "游戏" -> R.drawable.ic_sub_gamehandheld
        "休闲" -> R.drawable.ic_sub_leisure
        "健身" -> R.drawable.ic_sub_fitness
        "约会" -> R.drawable.ic_sub_date
        "演唱会" -> R.drawable.ic_sub_concert
        "K歌" -> R.drawable.ic_sub_ktv
        "宠物" -> R.drawable.ic_sub_pet

        // 美妆
        "洗面奶" -> R.drawable.ic_sub_cleanser
        "化妆品" -> R.drawable.ic_sub_cosmetics
        "面膜" -> R.drawable.ic_sub_facemask
        "美容仪器" -> R.drawable.ic_sub_beautydev
        "护肤品" -> R.drawable.ic_sub_skincare

        // 旅游
        "景点门票" -> R.drawable.ic_sub_ticket
        "酒店" -> R.drawable.ic_sub_hotel
        "团费" -> R.drawable.ic_sub_tour
        "伴手礼" -> R.drawable.ic_sub_souvenir
        "签证" -> R.drawable.ic_sub_visa

        // 医疗
        "药品" -> R.drawable.ic_sub_pills
        "就诊" -> R.drawable.ic_sub_doctor
        "治疗" -> R.drawable.ic_sub_treatment
        "住院" -> R.drawable.ic_sub_hospital
        "保健" -> R.drawable.ic_sub_health
        "体检" -> R.drawable.ic_sub_checkup

        // 会员租用
        "视频会员" -> R.drawable.ic_sub_video
        "音乐会员" -> R.drawable.ic_sub_music
        "书籍会员" -> R.drawable.ic_sub_bookvip
        "购物会员" -> R.drawable.ic_sub_shopvip
        "社交会员" -> R.drawable.ic_sub_socialvip
        "租赁" -> R.drawable.ic_sub_lease

        // 通讯
        "话费" -> R.drawable.ic_sub_phonefee
        "宽带" -> R.drawable.ic_sub_broadband
        "流量" -> R.drawable.ic_sub_data

        else -> null
    }
}
