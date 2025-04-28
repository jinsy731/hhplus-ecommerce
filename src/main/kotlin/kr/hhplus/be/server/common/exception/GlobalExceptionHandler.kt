package kr.hhplus.be.server.common.exception

import kr.hhplus.be.server.common.ErrorResponse
import kr.hhplus.be.server.common.FieldError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        log.info("business exception: ${ex.message}")
        val errorResponse = ErrorResponse(
            code = ex.errorCode.code,
            message = ex.message ?: ex.errorCode.message
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }


    /**
     * @Valid, @RequestBody 등에서 발생하는 validation 에러 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldError(
                field = it.field,
                value = it.rejectedValue?.toString() ?: "",
                errorMessage = it.defaultMessage ?: "유효하지 않은 값입니다."
            )
        }

        val errorResponse = ErrorResponse(
            code = "INVALID_REQUEST",
            message = "요청값이 유효하지 않습니다.",
            fieldErrors = fieldErrors
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * IllegalArgumentException 같은 일반적인 클라이언트 오류 처리
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            code = "BAD_REQUEST",
            message = ex.message ?: "잘못된 요청입니다."
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 시스템 예외 혹은 알 수 없는 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error occurred", ex)

        val errorResponse = ErrorResponse(
            code = "INTERNAL_ERROR",
            message = "서버 오류가 발생했습니다. 관리자에게 문의해주세요."
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
