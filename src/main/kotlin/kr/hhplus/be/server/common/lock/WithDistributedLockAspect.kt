package kr.hhplus.be.server.common.lock

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
class WithDistributedLockAspect(
    private val lockExecutor: DistributedLockExecutor,
    private val parser: SpelExpressionParser = SpelExpressionParser()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(withDistributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, withDistributedLock: WithDistributedLock): Any? {
        val key = resolveKey(joinPoint, withDistributedLock.key)
        logger.info("resolve key: $key")

        return lockExecutor.execute(
            key = key,
            lockType = withDistributedLock.type,
            waitTimeMillis = withDistributedLock.waitTimeMillis,
            leaseTimeMillis = withDistributedLock.leaseTimeMillis
        ) {
            joinPoint.proceed()
        }
    }

    private fun resolveKey(joinPoint: JoinPoint, keySpel: String): String {
        // SpEL 사용 안 하면 그대로 반환
        if (!keySpel.contains("#")) return keySpel

        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val args = joinPoint.args
        val paramNames = methodSignature.parameterNames

        val context = StandardEvaluationContext()
        paramNames.forEachIndexed { i, name ->
            context.setVariable(name, args[i])
        }

        return parser
            .parseExpression(keySpel)
            .getValue(context, String::class.java)
            ?: throw IllegalArgumentException("SpEL 표현식 평가 실패: $keySpel")
    }

}
