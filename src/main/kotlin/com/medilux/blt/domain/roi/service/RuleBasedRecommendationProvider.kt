package com.medilux.blt.domain.roi.service

import com.medilux.blt.domain.roi.entity.Recommendation
import com.medilux.blt.domain.roi.entity.RoiQuadrant
import org.springframework.stereotype.Component

/**
 * rule-based 추천 로직 — quadrant별 고정 문구. 추후 AI 구현으로 교체 예정.
 * ⚠️ 임시 placeholder. 확정 시 교체 필요.
 */
@Component
class RuleBasedRecommendationProvider : RecommendationProvider {
    override fun generate(context: RecommendationContext): List<Recommendation> {
        val score = context.score
        val copy = COPY_BY_QUADRANT.getValue(score.quadrant)
        return listOf(
            Recommendation(
                roiScore = score,
                quadrantKey = score.quadrant.name,
                title = copy.title,
                message = copy.message,
                suggestedTasks = copy.suggestedTasks,
            ),
        )
    }

    private data class QuadrantCopy(val title: String, val message: String, val suggestedTasks: List<Map<String, Any?>>)

    companion object {
        private val COPY_BY_QUADRANT: Map<RoiQuadrant, QuadrantCopy> = mapOf(
            RoiQuadrant.HIGH_SLEEP_HIGH_PVT to QuadrantCopy(
                title = "지금이 골든타임이에요",
                message = "수면 회복과 각성이 모두 충분해요. 가장 중요한 집중 업무를 지금 처리하세요.",
                suggestedTasks = listOf(
                    mapOf("time" to "now", "task" to "가장 어려운 핵심 업무 시작", "type" to "focus"),
                    mapOf("time" to "+90m", "task" to "짧은 휴식으로 컨디션 유지", "type" to "rest"),
                ),
            ),
            RoiQuadrant.HIGH_SLEEP_LOW_PVT to QuadrantCopy(
                title = "가벼운 워밍업부터",
                message = "수면은 충분하지만 각성이 덜 올라왔어요. 가벼운 활동으로 시동을 건 뒤 집중하세요.",
                suggestedTasks = listOf(
                    mapOf("time" to "now", "task" to "10분 산책 또는 스트레칭", "type" to "activation"),
                    mapOf("time" to "+15m", "task" to "물 한 잔 마시기", "type" to "hydration"),
                ),
            ),
            RoiQuadrant.LOW_SLEEP_HIGH_PVT to QuadrantCopy(
                title = "각성은 좋지만 무리는 금물",
                message = "지금은 반응이 좋지만 수면 부채가 쌓여 있어요. 집중 업무는 짧게 끊고 회복을 챙기세요.",
                suggestedTasks = listOf(
                    mapOf("time" to "now", "task" to "단기 집중 업무 처리", "type" to "focus"),
                    mapOf("time" to "tonight", "task" to "평소보다 일찍 취침", "type" to "sleep"),
                ),
            ),
            RoiQuadrant.LOW_SLEEP_LOW_PVT to QuadrantCopy(
                title = "오늘은 회복이 우선",
                message = "수면과 각성이 모두 낮아요. 중요한 결정·고난도 작업은 미루고 컨디션 회복에 집중하세요.",
                suggestedTasks = listOf(
                    mapOf("time" to "now", "task" to "가벼운 단순 업무 위주로 진행", "type" to "lowload"),
                    mapOf("time" to "afternoon", "task" to "20분 이내 낮잠", "type" to "nap"),
                ),
            ),
        )
    }
}
