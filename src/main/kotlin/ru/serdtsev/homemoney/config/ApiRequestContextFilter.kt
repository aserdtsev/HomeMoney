package ru.serdtsev.homemoney.config

import org.slf4j.MDC
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import java.util.*
import javax.servlet.*
import javax.servlet.annotation.WebFilter
import javax.servlet.http.HttpServletRequestWrapper

@WebFilter("/api/*")
class ApiRequestContextFilter : Filter {
    override fun init(filterConfig: FilterConfig) {
    }

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

    override fun destroy() {}
}