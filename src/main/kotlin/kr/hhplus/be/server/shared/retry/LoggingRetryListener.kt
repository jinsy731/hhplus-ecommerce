package kr.hhplus.be.server.shared.retry

import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.stereotype.Component

@Component
class LoggingRetryListener : RetryListener {

    override fun <T, E : Throwable> open(context: RetryContext, callback: RetryCallback<T, E>): Boolean {
        return true  // 항상 retry 열기
    }

    override fun <T, E : Throwable> onError(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable
    ) {
        val method = context.getAttribute(RetryContext.NAME)
        val count = context.retryCount
        println("🔁 Retry attempt #$count for $method due to ${throwable::class.simpleName}")
    }

    override fun <T, E : Throwable> close(context: RetryContext, callback: RetryCallback<T, E>, throwable: Throwable?) {
        // 필요 시 클린업 로직
    }
}