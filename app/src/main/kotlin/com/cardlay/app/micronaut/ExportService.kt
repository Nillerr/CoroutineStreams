package com.cardlay.app.micronaut

import io.micronaut.core.io.buffer.ByteBufferFactory
import io.micronaut.http.MediaType
import io.micronaut.http.server.types.files.StreamedFile
import jakarta.inject.Singleton
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Singleton
class ExportService(
    private val aws: AWSS3Client,
    private val byteBufferFactory: ByteBufferFactory<*, *>,
    private val expenses: ExpenseRepository,
    private val attachments: ExpenseAttachmentRepository,
) {
    fun export(request: ExportRequest): StreamedFile {
        val exportId = "dd26276ad2884937a0d02fd91961b434"

        // This stuff is placeholder stuff. Replace it with efficient queries k thx plz.
        val expenses = request.expenseIds.map { expenses.get(it) }
        val attachments = expenses.flatMap { expense -> attachments.list(expense.id) }

        // Not only do we use a `BlockingTaskQueue` to limit the parallelism of fetching the attachment files from
        // AWS S3...
        val downloadedFilesQueue = BlockingTaskQueue<DownloadedAWSS3File>(5)

        // Doing so means we would block the calling thread while waiting for enqueued work to be complete, so to
        // prevent that we create a new single-thread executor for every call to export.
        // (This could also use a custom configured thread-pool by injecting another `@Named` `ExecutorService`) if we
        // want to limit the total number of concurrent calls to the export endpoint for a given service instance.
        val enqueueExecutor = Executors.newSingleThreadExecutor()
        enqueueExecutor.execute {
            // ... but we also wrap that in a `PermittingBlockingTaskQueue` to limit enqueuing the work itself, thus
            // lowering memory usage by not allocating heap memory associated with each call to `execute` until we know
            // there's a thread available to actually perform the associated work.
            downloadedFilesQueue.asPermittingBlockingTaskQueue().use { downloadedWriterQueue ->
                attachments.forEach { attachment ->
                    downloadedWriterQueue.submit {
                        // By using a `PermittingBlockingTaskQueue` this block is only entered once the underlying
                        // `BlockingTaskQueue` is ready to perform more work.
                        downloadedAWSS3File(attachment)
                    }
                }
            }

            enqueueExecutor.shutdown()
        }

        // Now when we want to write the files downloaded from AWS S3 to an `InputStream` we pass on to the client in
        // order to stream the ZIP file itself, we first need to create a `PipedInputStream` which we can write to
        // while Micronaut eventually takes care of streaming it to the client.
        val pipedInputStream = PipedInputStream(DEFAULT_BUFFER_SIZE)

        // We wrap that `PipedInputStream` in a `PipedOutputStream` to be able to write to it.
        val pipedOutputStream = PipedOutputStream(pipedInputStream)

        // Then we wrap that `PipedOutputStream` in a `ZipOutputStream` in order to write a ZIP file.
        val zipOutputStream = ZipOutputStream(pipedOutputStream)

        // Now for another important part... When writing to the `PipedOutputStream` we must do so from another thread
        // than what is used to read from its associated `PipedInputStream`. Now I'm not sure whether Micronaut uses
        // the current thread to read the `InputStream`, so to make sure the implementation is solid, we use another
        // single-thread executor, that is separate from the one used to enqueue the work to download files.
        val zipWriterExecutor = Executors.newSingleThreadExecutor()
        zipWriterExecutor.execute {
            zipOutputStream.writeFiles(downloadedFilesQueue)
            zipWriterExecutor.shutdown()
        }

        // Now here's the confusing thing... Once we reach this line, all the stuff we started up above is still
        // running! The magic is really in the `PipedInputStream` which blocks on calls to `read` until it has new
        // content to read, which was written to its connected `PipedOutputStream`.
        val file = StreamedFile(pipedInputStream, MediaType.ZIP_TYPE)
            .attach("$exportId.zip")

        // Notes for further potential optimizations
        //  1. We can fetch the expense attachment records as-needed within the call to `downloadExecutor.execute`. This
        //  will limit memory consumption caused by a large number of `ExpenseAttachment` instances unnecessarily being
        //  in memory at the same time.
        //  2. We can apply that same principal to the `Expense` records, but this may get tricky when considering the
        //  need to feed those into an XLSX generator, although the case could be made for just fetching them twice, if
        //  we want to favor memory consumption over database queries.

        return file
    }

    private fun downloadedAWSS3File(attachment: ExpenseAttachment): DownloadedAWSS3File {
        val awsFile = aws.getFile(attachment.token)

        val byteBuffer = byteBufferFactory.buffer(awsFile.size)
        awsFile.inputStream.use { inputStream ->
            inputStream.copyTo(byteBuffer)
        }

        return DownloadedAWSS3File(name = awsFile.name, content = byteBuffer)
    }
}

private fun ZipOutputStream.writeFiles(files: BlockingTaskQueue<DownloadedAWSS3File>) {
    var attachmentFile = files.dequeue()
    while (attachmentFile != null) {
        val attachmentEntry = ZipEntry(attachmentFile.name)
        putNextEntry(attachmentEntry)
        writeFile(attachmentFile)
        closeEntry()
    }

    close()
}

private fun ZipOutputStream.writeFile(file: DownloadedAWSS3File) {
    file.content.toInputStream().use { contentInputStream ->
        contentInputStream.copyTo(this)
    }
}
