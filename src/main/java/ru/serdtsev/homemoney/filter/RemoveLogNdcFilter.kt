package ru.serdtsev.homemoney.filter

import org.apache.log4j.NDC
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

@Provider
class RemoveLogNdcFilter : ContainerResponseFilter {
  private val log = LoggerFactory.getLogger(javaClass)
  override fun filter(requestCtx: ContainerRequestContext?, p1: ContainerResponseContext?) {
    NDC.pop()
    NDC.remove()
  }
}