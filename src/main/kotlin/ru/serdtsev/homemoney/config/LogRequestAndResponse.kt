package ru.serdtsev.homemoney.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.Serializable

/**
 * Данной аннотацией должны быть помечены точки входа в контроллерах, если требуется логировать запрос и ответ.
 */
annotation class LogRequestAndResponse

/**
 * Аспект для логирования запроса и ответа.
 */
@Aspect
@Component
class LogRequestAndResponseAspect {
    @Around(pointcutExpr)
    fun logRequestAndResponse(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        val startAt = System.currentTimeMillis()
        val mapper = ObjectMapper()
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        val requestUri = request.requestURI
        val args = proceedingJoinPoint.args
        val requestBody = if (request.method in listOf(HttpMethod.POST.name(), HttpMethod.PUT.name())
                && args.isNotEmpty() && request.contentLength > 0 && args[args.size - 1] is Serializable) {
            try {
                val lastArg = args[args.size - 1] as Serializable
                mapper.writeValueAsString(lastArg)
            } catch (e: JsonProcessingException) {
                null
            }
        } else null
        val requestStr = "${request.method} $requestUri?${request.queryString}"
        log.debug { "REQUEST $requestStr ${if (requestBody.isNullOrEmpty()) "" else requestBody}" }
        val response: Any? = proceedingJoinPoint.proceed(args)
        val responseBody = try {
            mapper.writeValueAsString(response)
        } catch (e: JsonProcessingException) {
            null
        }
        val duration = System.currentTimeMillis() - startAt
        log.debug { "RESPONSE $requestUri $duration ms $responseBody" }
        return response
    }

    companion object {
        private val log = KotlinLogging.logger {}
        private const val pointcutExpr = "execution(@LogRequestAndResponse * *.*(..))"
    }
}