package com.medilux.blt.domain.user.entity

import com.medilux.blt.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@Entity
@Table(
    name = "consent_logs",
    indexes = [
        Index(name = "idx_consent_logs_user_id", columnList = "user_id"),
        Index(name = "idx_consent_logs_audit_trail", columnList = "user_id, consent_type, agreed_at DESC"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class ConsentLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30, updatable = false)
    val consentType: ConsentType,
    @Column(name = "policy_version", nullable = false, length = 10, updatable = false)
    val policyVersion: String,
    @Column(name = "agreed", nullable = false, updatable = false)
    val agreed: Boolean,
    @Column(name = "agreed_at", nullable = false, updatable = false)
    val agreedAt: Instant,
    @Column(name = "client_ip", columnDefinition = "inet", updatable = false)
    val clientIp: String? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}

enum class ConsentType {
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    HEALTH_DATA,
    MARKETING,
}
