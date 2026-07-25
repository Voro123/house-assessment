package com.voro.houseassessment.util

import com.voro.houseassessment.data.RoomRecord
import kotlin.math.round

data class AssessmentResult(
    val score: Double?,
    val label: String,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
    val missing: List<String>,
    val confidence: String,
    val filledCount: Int,
    val importantCount: Int,
    val warnings: List<String>
)

object AssessmentEngine {
    private data class Metric(
        val key: String,
        val score: Double,
        val weight: Double,
        val positive: String,
        val negative: String
    )

    fun evaluate(room: RoomRecord): AssessmentResult {
        val metrics = mutableListOf<Metric>()
        val missing = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        fun addRating(
            key: String,
            value: Int?,
            weight: Double,
            positive: String,
            negative: String,
            missingName: String = key
        ) {
            if (value == null) missing += missingName
            else metrics += Metric(key, value.coerceIn(1, 5).toDouble(), weight, positive, negative)
        }

        if (room.rentMonthly != null && room.targetBudget != null && room.targetBudget > 0) {
            val ratio = room.rentMonthly / room.targetBudget
            val score = when {
                ratio <= 0.85 -> 5.0
                ratio <= 1.0 -> 4.3
                ratio <= 1.1 -> 3.4
                ratio <= 1.25 -> 2.4
                else -> 1.3
            }
            metrics += Metric("租金性价比", score, 20.0, "租金低于或符合预算", "租金明显超出预算")
        } else {
            missing += if (room.rentMonthly == null) "月租金" else "目标预算"
        }

        room.orientation?.let {
            val score = when (it) {
                "南", "东南" -> 4.8
                "东" -> 4.2
                "西南" -> 3.8
                "东北" -> 3.3
                "西" -> 3.0
                "西北" -> 2.8
                "北" -> 2.5
                else -> 3.4
            }
            metrics += Metric("朝向", score, 7.0, "朝向有利于采光", "朝向可能影响采光和体感")
        } ?: run { missing += "房屋朝向" }

        room.hasBalcony?.let {
            metrics += Metric("独立阳台", if (it) 4.6 else 2.8, 3.0, "带独立阳台", "没有独立阳台")
        } ?: run { missing += "独立阳台" }

        addRating("水质", room.waterQuality, 7.0, "水质表现较好", "水质需要留意")

        room.acLevel?.let {
            val score = when (it) {
                0 -> 1.5
                1 -> 2.2
                2 -> 3.4
                3 -> 4.7
                else -> 3.0
            }
            val positive = if (it == 3) "空调较新且能耗较低" else "空调条件可接受"
            val negative = if (it == 0) "没有空调" else "空调较旧或能耗偏高"
            metrics += Metric("空调能耗", score, 6.0, positive, negative)
        } ?: run { missing += "空调及能耗" }

        room.hasWasher?.let {
            metrics += Metric("洗衣机", if (it) 4.5 else 2.2, 4.0, "配有洗衣机", "没有洗衣机")
        } ?: run { missing += "洗衣机" }

        addRating("空间布局", room.spaceRating, 8.0, "空间和布局较舒适", "空间偏小或布局不合理")
        addRating("插座", room.outletRating, 5.0, "插座数量和位置实用", "插座不足或位置不便")
        addRating("噪音", room.noiseRating, 12.0, "室内较安静", "噪音较明显")
        addRating("采光", room.lightingRating, 8.0, "自然采光良好", "室内采光偏弱")
        addRating("通风", room.ventilationRating, 6.0, "通风条件良好", "通风条件一般")
        addRating("卫生", room.cleanlinessRating, 4.0, "房屋维护和卫生较好", "房屋卫生或维护较差")
        addRating("潮湿霉菌", room.dampMoldRating, 8.0, "未见明显潮湿或霉菌", "存在潮湿、霉味或霉菌风险", "潮湿与霉菌")
        addRating("卫生间", room.bathroomRating, 5.0, "卫生间条件不错", "卫生间条件较弱")
        addRating("厨房", room.kitchenRating, 4.0, "厨房使用条件不错", "厨房配置或卫生一般")
        addRating("安全", room.securityRating, 8.0, "门锁、消防和周边安全较好", "安全条件需要重点确认")
        addRating("交通", room.transitRating, 6.0, "通勤和交通便利", "交通或通勤不够方便")
        addRating("周边", room.neighborhoodRating, 4.0, "周边生活便利", "周边环境或配套一般")
        addRating("网络", room.networkRating, 3.0, "网络安装和信号条件较好", "网络条件需要改善")
        addRating("收纳", room.storageRating, 3.0, "收纳空间充足", "收纳空间不足")
        addRating("家具家电", room.furnishingRating, 3.0, "家具家电状态较好", "家具家电老旧或不齐全")
        addRating("合同风险", room.leaseRiskRating, 8.0, "房源和合同信息较清晰", "合同或房源信息存在风险", "合同与房源风险")

        if (room.floor != null && room.floor >= 4) {
            room.hasElevator?.let {
                metrics += Metric("电梯", if (it) 4.5 else 1.8, 4.0, "高楼层配有电梯", "楼层较高但没有电梯")
            } ?: run { missing += "电梯" }
        }

        if (room.dampMoldRating != null && room.dampMoldRating <= 1) {
            warnings += "发现严重潮湿或霉菌风险"
        }
        if (room.securityRating != null && room.securityRating <= 1) {
            warnings += "门锁、消防或周边存在明显安全风险"
        }
        if (room.leaseRiskRating != null && room.leaseRiskRating <= 1) {
            warnings += "合同或房源真实性存在高风险"
        }

        val importantCount = 20
        val filledImportant = listOf(
            room.rentMonthly != null && room.targetBudget != null,
            room.orientation != null,
            room.hasBalcony != null,
            room.waterQuality != null,
            room.acLevel != null,
            room.hasWasher != null,
            room.spaceRating != null,
            room.outletRating != null,
            room.noiseRating != null,
            room.lightingRating != null,
            room.ventilationRating != null,
            room.cleanlinessRating != null,
            room.dampMoldRating != null,
            room.bathroomRating != null,
            room.kitchenRating != null,
            room.securityRating != null,
            room.transitRating != null,
            room.neighborhoodRating != null,
            room.networkRating != null,
            room.leaseRiskRating != null
        ).count { it }

        val confidence = when {
            filledImportant >= 15 -> "高"
            filledImportant >= 9 -> "中"
            else -> "低"
        }

        if (metrics.isEmpty()) {
            return AssessmentResult(
                score = null,
                label = "待评估",
                summary = "还没有足够的评估数据。可以先保存房源，再在看房过程中逐项补充。",
                pros = emptyList(),
                cons = emptyList(),
                missing = missing.distinct().take(6),
                confidence = confidence,
                filledCount = filledImportant,
                importantCount = importantCount,
                warnings = warnings
            )
        }

        val weightSum = metrics.sumOf { it.weight }
        var score = metrics.sumOf { it.score * it.weight } / weightSum
        if (warnings.isNotEmpty()) score = minOf(score, 2.5)
        score = round(score.coerceIn(1.0, 5.0) * 10.0) / 10.0

        val pros = metrics
            .filter { it.score >= 4.15 }
            .sortedByDescending { (it.score - 3.0) * it.weight }
            .map { it.positive }
            .distinct()
            .take(3)

        val cons = metrics
            .filter { it.score <= 2.65 }
            .sortedByDescending { (3.0 - it.score) * it.weight }
            .map { it.negative }
            .distinct()
            .take(3)

        val label = when {
            score >= 4.5 -> "强烈推荐"
            score >= 3.8 -> "值得考虑"
            score >= 3.0 -> "中规中矩"
            score >= 2.0 -> "谨慎考虑"
            else -> "不推荐"
        }

        val summary = buildString {
            if (confidence == "低") append("当前信息较少，暂评为${score}分。")
            else append("综合来看，这套房源${label}。")
            if (pros.isNotEmpty()) append(" 优点是${pros.take(2).joinToString("、")}。")
            if (cons.isNotEmpty()) append(" 需要注意${cons.take(2).joinToString("、")}。")
            if (warnings.isNotEmpty()) append(" 存在重要风险，建议确认后再签约。")
            else if (missing.isNotEmpty()) append(" 仍有部分关键项目未检查。")
        }

        return AssessmentResult(
            score = score,
            label = label,
            summary = summary,
            pros = pros,
            cons = cons,
            missing = missing.distinct().take(6),
            confidence = confidence,
            filledCount = filledImportant,
            importantCount = importantCount,
            warnings = warnings
        )
    }
}
