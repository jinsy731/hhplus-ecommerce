package kr.hhplus.be.server.coupon.infrastructure.kvstore

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kr.hhplus.be.server.coupon.application.CouponKeyGenerator
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RedisCouponKVStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : CouponKVStore {
    
    // Lua 스크립트 정의
    private val markAsIssuedScript = """
        local statusKey = KEYS[1]
        local issuedSetKey = KEYS[2]
        local userId = ARGV[1]
        
        -- 상태를 ISSUED로 설정
        redis.call('SET', statusKey, 'ISSUED')
        
        -- 발급된 사용자 세트에 추가
        return redis.call('SADD', issuedSetKey, userId)
    """.trimIndent()
    
    private val rollbackIssuedMarkScript = """
        local statusKey = KEYS[1]
        local issuedSetKey = KEYS[2]
        local userId = ARGV[1]
        
        -- 상태를 PENDING으로 설정
        redis.call('SET', statusKey, 'PENDING')
        
        -- 발급된 사용자 세트에서 제거
        return redis.call('SREM', issuedSetKey, userId)
    """.trimIndent()
    
    override fun existsIssuedUser(userId: Long, couponId: Long): Boolean {
        val key = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        return redisTemplate.opsForSet().isMember(key, userId.toString()) ?: false
    }

    override fun setIssuedUser(userId: Long, couponId: Long) {
        val key = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        redisTemplate.opsForSet().add(key, userId.toString())
    }

    override fun countIssuedUser(couponId: Long): Long {
        val key = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        return redisTemplate.opsForSet().size(key) ?: 0
    }

    override fun pushToIssueReqeustQueue(issueRequest: CouponIssueRequest) {
        val couponId = issueRequest.couponId
        val key = CouponKeyGenerator.getIssueRequestQueueKey(couponId)
        val serializedRequest = objectMapper.writeValueAsString(issueRequest)
        val currentTimestamp = Instant.now().toEpochMilli().toDouble()
                
        redisTemplate.opsForZSet().add(key, serializedRequest, currentTimestamp)
    }

    override fun popBatchFromIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest> {
        val key = CouponKeyGenerator.getIssueRequestQueueKey(couponId)
        val results = mutableListOf<CouponIssueRequest>()
        
        val range = redisTemplate.opsForZSet().popMin(key, batchSize)
        
        range?.forEach { tuple ->
            val serializedRequest = tuple.value
            if (serializedRequest is String) {
                val request = objectMapper.readValue<CouponIssueRequest>(serializedRequest)
                results.add(request)
            }
        }

        return results
    }

    override fun pushToFailedIssueRequestQueue(issueRequest: CouponIssueRequest) {
        val key = CouponKeyGenerator.getFailedIssueRequestQueueKey(issueRequest.couponId)
        val serializedRequest = objectMapper.writeValueAsString(issueRequest)
        redisTemplate.opsForList().rightPush(key, serializedRequest)
    }

    override fun pushAllToFailedIssueRequestQueue(failedRequests: List<CouponIssueRequest>) {
        failedRequests.forEach { request ->
            pushToFailedIssueRequestQueue(request)
        }
    }

    override fun popBatchFromFailedIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest> {
        val key = CouponKeyGenerator.getFailedIssueRequestQueueKey(couponId)
        val results = mutableListOf<CouponIssueRequest>()
        
        for (i in 0 until batchSize) {
            val serializedRequest = redisTemplate.opsForList().leftPop(key) ?: break
            val request = objectMapper.readValue<CouponIssueRequest>(serializedRequest)
            results.add(request)
        }
        
        return results
    }

    override fun peekBatchFromFailedIssueRequestQueue(
        couponId: Long,
        batchSize: Long
    ): List<CouponIssueRequest> {
        val key = CouponKeyGenerator.getFailedIssueRequestQueueKey(couponId)
        val results = mutableListOf<CouponIssueRequest>()

        for (i in 0 until batchSize) {
            val serializedRequest = redisTemplate.opsForList().index(key, i) ?: break
            val request = objectMapper.readValue<CouponIssueRequest>(serializedRequest)
            results.add(request)
        }

        return results
    }

    override fun getStock(couponId: Long): CouponStock {
        val key = CouponKeyGenerator.getStockKey(couponId)
        val stockStr = redisTemplate.opsForValue().get(key)
        
        return if (stockStr != null) {
            objectMapper.readValue(stockStr)
        } else {
            CouponStock(couponId, 0)
        }
    }

    override fun getStocks(couponIds: List<Long>): CouponStock {
        return if (couponIds.isNotEmpty()) {
            getStock(couponIds[0])
        } else {
            CouponStock(-1, 0)
        }
    }

    override fun setStock(couponStock: CouponStock) {
        val key = CouponKeyGenerator.getStockKey(couponStock.couponId)
        val serializedStock = objectMapper.writeValueAsString(couponStock)
        redisTemplate.opsForValue().set(key, serializedStock)
    }

    override fun setIssuedStatus(userId: Long, couponId: Long, status: IssuedStatus) {
        val key = CouponKeyGenerator.getIssuedStatusKey(userId, couponId)
        redisTemplate.opsForValue().set(key, status.name)
    }

    override fun getIssuedStatus(userId: Long, couponId: Long): IssuedStatus {
        val key = CouponKeyGenerator.getIssuedStatusKey(userId, couponId)
        val statusStr = redisTemplate.opsForValue().get(key)
        
        return if (statusStr != null) {
            IssuedStatus.valueOf(statusStr)
        } else {
            IssuedStatus.FAILED
        }
    }
    
    override fun pushToIssueRequestedCouponIdList(couponId: Long) {
        val listKey = CouponKeyGenerator.getIssueRequestedCouponIdListKey()
        redisTemplate.opsForList().rightPush(listKey, couponId.toString())
    }
    
    override fun popFromIssueRequestedCouponIdList(): Long? {
        val listKey = CouponKeyGenerator.getIssueRequestedCouponIdListKey()
        val value = redisTemplate.opsForList().leftPop(listKey)
        
        return value?.toInt()?.toLong()
    }

    override fun peekFromIssueRequestedCouponIdList(): Long? {
        val listKey = CouponKeyGenerator.getIssueRequestedCouponIdListKey()
        val value = redisTemplate.opsForList().index(listKey, 0)

        return value?.toInt()?.toLong()
    }

    override fun pushToFailedIssueRequestedCouponIdList(couponId: Long) {
        val listKey = CouponKeyGenerator.getFailedIssueRequestedCouponIdListKey()
        redisTemplate.opsForList().rightPush(listKey, couponId.toString())
    }
    
    override fun popFromFailedIssueRequestedCouponIdList(): Long? {
        val listKey = CouponKeyGenerator.getFailedIssueRequestedCouponIdListKey()
        val value = redisTemplate.opsForList().leftPop(listKey)
        
        return value?.toInt()?.toLong()
    }

    override fun peekFromFailedIssueRequestedCouponIdList(): Long? {
        val listKey = CouponKeyGenerator.getFailedIssueRequestedCouponIdListKey()
        val value = redisTemplate.opsForList().index(listKey, 0)

        return value?.toInt()?.toLong()
    }

    override fun pushToOutOfStockCouponIdList(couponId: Long) {
        val listKey = CouponKeyGenerator.getOutOfStockCouponIdListKey()
        redisTemplate.opsForList().rightPush(listKey, couponId.toString())
    }
    
    override fun popFromOutOfStockCouponIdList(): Long? {
        val listKey = CouponKeyGenerator.getOutOfStockCouponIdListKey()
        val value = redisTemplate.opsForList().leftPop(listKey)
        
        return value?.toInt()?.toLong()
    }

    override fun markAsIssued(userId: Long, couponId: Long): Boolean {
        val statusKey = CouponKeyGenerator.getIssuedStatusKey(userId, couponId)
        val issuedSetKey = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        
        val keys = listOf(statusKey, issuedSetKey)
        val args = listOf(userId.toString())
        
        val result = redisTemplate.execute(
            RedisScript.of(markAsIssuedScript, Long::class.java),
            keys, 
            *args.toTypedArray()
        )
        
        return result == 1L
    }
    
    override fun rollbackIssuedMark(userId: Long, couponId: Long): Boolean {
        val statusKey = CouponKeyGenerator.getIssuedStatusKey(userId, couponId)
        val issuedSetKey = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        
        val keys = listOf(statusKey, issuedSetKey)
        val args = listOf(userId.toString())
        
        val result = redisTemplate.execute(
            RedisScript.of(rollbackIssuedMarkScript, Long::class.java),
            keys, 
            *args.toTypedArray()
        )
        
        return result == 1L
    }

    override fun peekAllFromIssueRequestQueue(couponId: Long): List<CouponIssueRequest> {
        val key = CouponKeyGenerator.getIssueRequestQueueKey(couponId)
        val results = mutableListOf<CouponIssueRequest>()
        
        val range = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1)
        
        range?.forEach { tuple ->
            val serializedRequest = tuple.value
            if (serializedRequest is String) {
                val request = objectMapper.readValue<CouponIssueRequest>(serializedRequest)
                results.add(request)
            }
        }

        return results
    }
    
    override fun peekBatchFromIssueRequestQueue(couponId: Long, batchSize: Long): List<CouponIssueRequest> {
        val key = CouponKeyGenerator.getIssueRequestQueueKey(couponId)
        val results = mutableListOf<CouponIssueRequest>()
        
        val range = redisTemplate.opsForZSet().rangeWithScores(key, 0, batchSize - 1)
        
        range?.forEach { tuple ->
            val serializedRequest = tuple.value
            if (serializedRequest is String) {
                val request = objectMapper.readValue<CouponIssueRequest>(serializedRequest)
                results.add(request)
            }
        }

        return results
    }

    override fun countIssueRequestQueue(couponId: Long): Long {
        val key = CouponKeyGenerator.getIssueRequestQueueKey(couponId)
        return redisTemplate.opsForZSet().size(key) ?: 0
    }
    
    override fun countFailedIssueRequestQueue(couponId: Long): Long {
        val key = CouponKeyGenerator.getFailedIssueRequestQueueKey(couponId)
        return redisTemplate.opsForList().size(key) ?: 0
    }

    // Redis + Kafka 개선된 발급을 위한 메서드들
    
    /**
     * Lua Script를 활용한 원자적 쿠폰 발급 사전 검증 및 처리
     */
    override fun validateAndMarkCouponIssue(userId: Long, couponId: Long): CouponIssueValidationResult {
        val validateAndMarkScript = """
            local stockKey = KEYS[1]
            local issuedSetKey = KEYS[2] 
            local userId = ARGV[1]
            
            -- 1. 재고 정보 확인
            local stockData = redis.call('GET', stockKey)
            if not stockData then
                return { 'INVALID', 'COUPON_NOT_FOUND', '쿠폰을 찾을 수 없습니다.' }
            end
            
            -- 2. 중복 발급 체크
            if redis.call('SISMEMBER', issuedSetKey, userId) == 1 then
                return { 'INVALID', 'DUPLICATE_ISSUE', '이미 발급받은 쿠폰입니다.' }
            end
            
            -- 3. 재고 파싱 및 검증 (stock은 최대 발급 수량)
            local stock = cjson.decode(stockData)
            local currentIssued = redis.call('SCARD', issuedSetKey)
            local remainingStock = stock.stock - currentIssued
            
            if remainingStock < 1 then
                return { 'INVALID', 'OUT_OF_STOCK', '쿠폰 재고가 부족합니다.' }
            end
            
            -- 4. 검증 성공 시 발급 처리
            redis.call('SADD', issuedSetKey, userId)
            
            return { 'VALID', nil, nil }
        """.trimIndent()
        
        val stockKey = CouponKeyGenerator.getStockKey(couponId)
        val issuedSetKey = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        
        val keys = listOf(stockKey, issuedSetKey)
        val args = listOf(userId.toString())
        
        try {
            val result = redisTemplate.execute(
                RedisScript.of(validateAndMarkScript, List::class.java),
                keys, 
                *args.toTypedArray()
            ) as? List<String>
            
            return when (result?.get(0)) {
                "VALID" -> CouponIssueValidationResult.success()
                "INVALID" -> {
                    val errorCode = result.getOrNull(1) ?: "UNKNOWN_ERROR"
                    val errorMessage = result.getOrNull(2) ?: "알 수 없는 오류가 발생했습니다."
                    CouponIssueValidationResult.failure(errorCode, errorMessage)
                }
                else -> CouponIssueValidationResult.failure("SCRIPT_ERROR", "Redis 스크립트 실행 중 오류가 발생했습니다.")
            }
        } catch (e: Exception) {
            return CouponIssueValidationResult.failure("REDIS_ERROR", "Redis 처리 중 오류가 발생했습니다: ${e.message}")
        }
    }
    
    /**
     * 쿠폰 발급 실패 시 롤백 처리 (보상 로직)
     * 단순한 연산이므로 Lua Script 없이 직접 처리
     */
    override fun rollbackCouponIssue(userId: Long, couponId: Long): Boolean {
        return try {
            val issuedSetKey = CouponKeyGenerator.getIssuedUserSetKey(couponId)
            val result = redisTemplate.opsForSet().remove(issuedSetKey, userId.toString())
            result != null && result > 0
        } catch (e: Exception) {
            false
        }
    }
} 