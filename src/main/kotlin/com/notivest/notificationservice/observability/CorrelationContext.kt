package com.notivest.notificationservice.observability

import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.util.StringUtils

object CorrelationContext {
    const val HEADER_CORRELATION_ID = "X-Correlation-Id"
    const val HEADER_REQUEST_ID = "X-Request-Id"
    private const val MDC_CORRELATION_ID = "correlationId"
    private const val MDC_USER_ID = "userId"

    fun setCorrelationId(correlationId: String) {
        MDC.put(MDC_CORRELATION_ID, correlationId)
    }

    fun setUserId(userId: String) {
        MDC.put(MDC_USER_ID, userId)
    }

    fun currentCorrelationId(): String? =
        MDC.get(MDC_CORRELATION_ID)?.takeIf(StringUtils::hasText)

    fun currentTraceId(): String? =
        MDC.get("traceId")?.takeIf(StringUtils::hasText)

    fun copyTo(headers: HttpHeaders) {
        val correlationId = currentCorrelationId() ?: return
        if (!headers.containsKey(HEADER_CORRELATION_ID)) {
            headers.set(HEADER_CORRELATION_ID, correlationId)
        }
    }

    fun clear() {
        MDC.remove(MDC_CORRELATION_ID)
        MDC.remove(MDC_USER_ID)
    }
}
