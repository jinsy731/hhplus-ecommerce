package kr.hhplus.be.server.coupon.infrastructure

import kr.hhplus.be.server.coupon.application.CouponKeyGenerator
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant

@Component
class RedisCouponKVStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : CouponKVStore {
    
    override fun existsIssuedUser(userId: Long, couponId: Long) {
        val key = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        redisTemplate.opsForSet().isMember(key, userId.toString())
    }

    override fun setIssuedUser(userId: Long, couponId: Long) {
        val key = CouponKeyGenerator.getIssuedUserSetKey(couponId)
        redisTemplate.opsForSet().add(key, userId.toString())
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
    
    override fun addToIssueRequestedCouponIdSet(couponId: Long) {
        val setKey = CouponKeyGenerator.getIssueRequestedCouponIdSetKey()
        redisTemplate.opsForSet().add(setKey, couponId.toString())
    }
    
    override fun popFromIssueRequestedCouponIdSet(): Long? {
        val setKey = CouponKeyGenerator.getIssueRequestedCouponIdSetKey()
        val popResult = redisTemplate.opsForSet().pop(setKey)
        
        return popResult?.toInt()?.toLong()
    }
    
    override fun addToFailedIssueRequestedCouponIdSet(couponId: Long) {
        val setKey = CouponKeyGenerator.getFailedIssueRequestedCouponIdSetKey()
        redisTemplate.opsForSet().add(setKey, couponId.toString())
    }
    
    override fun popFromFailedIssueRequestedCouponIdSet(): Long? {
        val setKey = CouponKeyGenerator.getFailedIssueRequestedCouponIdSetKey()
        val popResult = redisTemplate.opsForSet().pop(setKey)
        
        return popResult?.toInt()?.toLong()
    }
} 