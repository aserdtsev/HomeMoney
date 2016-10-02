package ru.serdtsev.homemoney

import org.apache.log4j.NDC
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

@Provider
class RemoveLogNdcFilter : ContainerResponseFilter {
  override fun filter(requestCtx: ContainerRequestContext?, p1: ContainerResponseContext?) {
    NDC.pop()
    NDC.remove()
  }
}