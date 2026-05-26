package com.medilux.blt.domain.auth.entity

import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = ["token_hash"]),
    ],
    indexes = [
        Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class RefreshToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val user: User,
    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    val tokenHash: String,
    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
