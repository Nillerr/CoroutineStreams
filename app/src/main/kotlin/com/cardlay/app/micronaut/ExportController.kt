package com.cardlay.app.micronaut

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.server.types.files.StreamedFile

@Controller
class ExportController(private val service: ExportService) {
    @Post
    fun export(@Body request: ExportRequest): StreamedFile {
        return service.export(request)
    }
}
