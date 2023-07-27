package ru.serdtsev.homemoney.infra.exception

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class HmExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(HmException::class)
    fun handleBadRequest(e: HmException, request: WebRequest): ResponseEntity<Any>? {
        return handleExceptionInternal(e, "", HttpHeaders(), HttpStatus.UNAUTHORIZED, request)
    }
}