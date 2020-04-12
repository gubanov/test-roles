package org.gubanov.app

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value= HttpStatus.FORBIDDEN)
class AccessDeniedException: Exception()

@ResponseStatus(value= HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : Exception(message)

fun denyAccess() : Nothing {
    throw AccessDeniedException()
}

fun abortWithNotFound(message: String): Nothing {
    throw ResourceNotFoundException(message)
}