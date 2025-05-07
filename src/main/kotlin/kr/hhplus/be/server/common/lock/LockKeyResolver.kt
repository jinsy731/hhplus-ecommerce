package kr.hhplus.be.server.common.lock

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Component
class LockKeyResolver {

    private val parser = SpelExpressionParser()
    private val paramDiscoverer = DefaultParameterNameDiscoverer()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun resolveKey(joinPoint: JoinPoint, expression: String): List<String> {
        if (!expression.contains("#")) return listOf(expression)
        val context = createContext(joinPoint)
        val parsed = parser.parseExpression(expression).getValue(context)
        val resolvedKeys = when (parsed) {
            is Collection<*> -> parsed.map {
                it?.toString() ?: throw IllegalArgumentException("null 키는 허용되지 않습니다: $expression")
            }
            is String -> listOf(parsed)
            else -> throw IllegalArgumentException("SpEL 표현식이 List 또는 String을 반환해야 합니다: $expression")
        }

        return resolvedKeys
            .distinct()
            .sorted()
    }

    private fun createContext(joinPoint: JoinPoint): StandardEvaluationContext {
        val signature = joinPoint.signature as MethodSignature
        val args = joinPoint.args
        val names = signature.parameterNames
        return StandardEvaluationContext().apply {
            names.forEachIndexed { i, name -> setVariable(name, args[i]) }
        }
    }
}
