package com.localbill.accessibility

/**
 * 商家名 → 一级支出分类关键词映射。
 * 按顺序匹配，命中的关键词对应的分类名返回给调用方；未命中返回 null，由调用方归入「其他」。
 */
object MerchantMap {

    /** 有序映射：关键词列表 → 分类名 */
    private val RULES = listOf(
        listOf("美团", "饿了么", "瑞幸", "星巴克", "麦当劳", "肯德基", "海底捞", "蜜雪", "茶百道", "沪上阿姨", "塔斯汀") to "餐饮",
        listOf("滴滴", "高德", "地铁", "公交", "加油", "加油站", "中石化", "中石油", "哈啰", "T3出行") to "交通",
        listOf("淘宝", "京东", "拼多多", "天猫", "唯品会", "抖音商城", "苏宁", "得物") to "购物",
        listOf("水电", "房租", "物业", "燃气", "供电", "水务", "供暖") to "居住",
        listOf("医院", "药房", "药店", "诊所", "体检", "医保") to "医疗",
        listOf("电影", "游戏", "王者", "原神", "bilibili", "哔哩哔哩", "腾讯视频", "爱奇艺", "优酷", "KTV") to "娱乐",
        listOf("话费", "流量", "中国移动", "中国联通", "中国电信", "宽带", "会员订阅") to "通讯",
        listOf("书店", "培训", "课程", "教育", "当当", "得到") to "学习"
    )

    /** 匹配商家名，返回分类名；未匹配返回 null */
    fun match(merchant: String): String? {
        val m = merchant.trim()
        if (m.isEmpty()) return null
        for ((keywords, category) in RULES) {
            if (keywords.any { m.contains(it) }) return category
        }
        return null
    }
}
