package com.medilux.blt.domain.roi.service

import com.medilux.blt.domain.pvt.entity.PvtSession
import com.medilux.blt.domain.roi.entity.BrainRoiScore
import com.medilux.blt.domain.roi.entity.Recommendation
import com.medilux.blt.domain.sleep.entity.SleepRecord
import com.medilux.blt.domain.user.entity.User

/**
 * 추천 생성 입력 컨텍스트 — 점수 + 원천 데이터 + 개인화 정보.
 *
 * 현재 rule-based 구현은 quadrant만 사용, 추후 확장성을 위해 입력 계약을 미리 넓혀 둔다.
 * → AI 도입 시 호출부 구현만 교체.
 */
data class RecommendationContext(val score: BrainRoiScore, val sleep: SleepRecord?, val session: PvtSession, val user: User)

/**
 * 추천 생성 전략(seam). 현재 [RuleBasedRecommendationProvider],
 * 추후 AI 구현을 @Primary 빈으로 추가하면 이 인터페이스에 의존하는 호출부는 변경 없이 확장된다.
 */
interface RecommendationProvider {
    fun generate(context: RecommendationContext): List<Recommendation>
}
