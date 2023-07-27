package ru.serdtsev.homemoney.infra.config

import kotlinx.coroutines.ThreadContextElement
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import ru.serdtsev.homemoney.infra.ApiRequestContext
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import kotlin.coroutines.CoroutineContext


@Configuration
class SpringAsyncConfig {
    @Bean
    fun getAsyncExecutor() = ThreadPoolTaskExecutor().apply {
        setTaskDecorator(ContextCopyingDecorator())
        initialize()
    }
}

class ContextCopyingDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        val context = RequestContextHolder.currentRequestAttributes()
        val contextMap = MDC.getCopyOfContextMap()
        val apiRequestContext = ApiRequestContextHolder.apiRequestContext
        return Runnable {
            try {
                RequestContextHolder.setRequestAttributes(context)
                MDC.setContextMap(contextMap)
                ApiRequestContextHolder.apiRequestContext = apiRequestContext
                runnable.run()
            } finally {
                MDC.clear()
                RequestContextHolder.resetRequestAttributes()
                ApiRequestContextHolder.clear()
            }
        }
    }
}

/**
 * Supports passing ApiRequestContext and MDC-context to Kotlin Coroutines.
 *
 * Usage example: launch(Dispatchers.Default + CoroutineApiRequestContext()) {...
 */
class CoroutineApiRequestContext(
    private val apiRequestContext: ApiRequestContext = ApiRequestContextHolder.apiRequestContext,
    private val requestAttributes: RequestAttributes? = getCurrentRequestAttributes(),
    private val contextMap: Map<String, String> = MDC.getCopyOfContextMap() ?: emptyMap()
) : ThreadContextElement<ApiRequestContext> {
    // declare companion object for a key of this element in coroutine context
    companion object Key : CoroutineContext.Key<CoroutineApiRequestContext>

    // provide the key of the corresponding context element
    override val key: CoroutineContext.Key<CoroutineApiRequestContext>
        get() = Key

    // this is invoked before coroutine is resumed on current thread
    override fun updateThreadContext(context: CoroutineContext): ApiRequestContext {
        val previousApiRequestContext = ApiRequestContextHolder.apiRequestContext

        RequestContextHolder.setRequestAttributes(requestAttributes)
        MDC.setContextMap(contextMap)
        ApiRequestContextHolder.apiRequestContext = apiRequestContext

        return previousApiRequestContext
    }

    // this is invoked after coroutine has suspended on current thread
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun restoreThreadContext(context: CoroutineContext, oldApiRequestContext: ApiRequestContext) {
        MDC.clear()
        RequestContextHolder.resetRequestAttributes()
        ApiRequestContextHolder.clear()
    }
}

// for module tests
fun getCurrentRequestAttributes() = try {
    RequestContextHolder.currentRequestAttributes()
} catch (e: IllegalStateException) {
    null
}