package com.medilux.blt.domain.notification.service

import com.medilux.blt.domain.notification.dto.DeviceRegisterRequest
import com.medilux.blt.domain.notification.dto.DeviceResponse
import com.medilux.blt.domain.user.entity.UserDevice
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DeviceService(private val userRepository: UserRepository, private val userDeviceRepository: UserDeviceRepository) {
    /**
     * FCM 토큰 기준 upsert. 동일 토큰이 이미 있으면 재활성화(revokedAt 해제) + lastActiveAt 갱신,
     * 없으면 신규 등록한다. 로그인 직후 + 알림 권한 허용 시 호출된다.
     */
    @Transactional
    fun register(userId: Long, request: DeviceRegisterRequest): DeviceResponse {
        val now = Instant.now()

        // 같은 토큰이 다른 사용자에 활성화돼 있으면 회수해 한 디바이스에 여러 사용자가 살아남지 않도록 한다.
        userDeviceRepository.revokeActiveByFcmTokenForOtherUsers(request.fcmToken, userId, now)

        val existing = userDeviceRepository.findByUserIdAndFcmToken(userId, request.fcmToken)

        val device = if (existing != null) {
            existing.lastActiveAt = now
            existing.revokedAt = null
            existing
        } else {
            val user = userRepository.findById(userId)
                .orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }
            userDeviceRepository.save(
                UserDevice(
                    user = user,
                    fcmToken = request.fcmToken,
                    platform = request.platform,
                    lastActiveAt = now,
                ),
            )
        }

        return DeviceResponse.from(device)
    }

    /** 로그아웃/알림 거부 시 디바이스 revoke (soft). 본인 소유 디바이스만 해제 가능. */
    @Transactional
    fun unregister(userId: Long, deviceId: Long) {
        val device = userDeviceRepository.findByIdAndUserId(deviceId, userId)
            ?: throw BltException(ErrorCode.DEVICE_NOT_FOUND)
        device.revokedAt = Instant.now()
    }
}
