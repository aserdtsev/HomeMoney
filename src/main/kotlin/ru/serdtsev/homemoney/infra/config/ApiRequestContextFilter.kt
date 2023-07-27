package ru.serdtsev.homemoney.infra.config

import org.slf4j.MDC
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.exception.HmException
import java.util.*
import jakarta.servlet.*
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequestWrapper

@WebFilter("/api/*")
class ApiRequestContextFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        ApiRequestContextHolder.requestId = (request as HttpServletRequestWrapper)
            .getHeader("X-Request-Id") ?: UUID.randomUUID().toString().substring(0, 8)
        MDC.put("ri", ApiRequestContextHolder.requestId)
        if (!request.requestURI.contains("api/user/login")) {
            ApiRequestContextHolder.bsId = request.getHeader("X-Balance-Sheet-Id")
                ?.let { UUID.fromString(it) } ?: throw HmException(HmException.Code.BalanceSheetIdNotFound)
        }
        chain.doFilter(request, response)
    }
}