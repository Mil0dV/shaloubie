import android.util.Log
import com.coremedia.iso.boxes.Container
import com.coremedia.iso.boxes.SampleSizeBox
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

abstract class VideoTask(
    private val taskID: Int,
    private val download: Download
) : BaseTask() {

    @Inject
    protected lateinit var videoController: VideoController

    override suspend fun run() = withContext(Dispatchers.IO) {
        try {
            EventBus.getDefault().post(StartEvent(download))
            
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            
            val startTime = System.currentTimeMillis()
            currentThread = Thread.currentThread()
            
            val chunksData = videoController.getChunks(download.downloadUrl)
            val chunkFileMap = download(chunksData)
            assembleFile(chunkFileMap)
            finalizeAudio(chunksData)
            complete()

            Log.d(TAG, "Total execution time: ${System.currentTimeMillis() - startTime}ms")

        } catch (ex: InterruptedException) {
            Log.d(TAG, ex.toString())
            cancelled()
            cleanup(true)
        } catch (ex: Exception) {
            Log.d(TAG, ex.toString())
            failed()
            cleanup(true)
        } finally {
            Ion.getDefault(context).cancelAll(dlGroup)
            cleanup(false)
        }
    }

    private fun cleanup(removeOutputFile: Boolean) {
        if (removeOutputFile && download.dst != null) {
            FileUtils.deleteQuietly(download.dst)
        }
        download.tmpDst?.let { FileUtils.deleteQuietly(it) }
    }

    private suspend fun assembleFile(chunks: Map<Int, File>) = withContext(Dispatchers.IO) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }

        mBuilder.setContentText("Finalizing the audio")
            .setProgress(0, 0, true)
        notificationManager.notify(taskID, mBuilder.build())
        
        Log.d(TAG, "Map Size: ${chunks.size}")
        Log.d(TAG, "Files: $chunks")

        BufferedOutputStream(FileOutputStream(download.tmpDst)).use { output ->
            chunks.forEach { (_, sourceFile) ->
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }
                BufferedInputStream(FileInputStream(sourceFile)).use { input ->
                    IOUtils.copy(input, output)
                }
            }
        }
    }

    private suspend fun finalizeAudio(chunksData: Chunks) = withContext(Dispatchers.IO) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }

        val sampleSizeBox = chunksData.sampleSizeBox
        
        FileInputStream(download.tmpDst).use { aacFis ->
            FileOutputStream(download.tmpDst2, true).use { newAac ->
                sampleSizeBox.sampleSizes.forEachIndexed { index, sampleSize ->
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    val originalSample = ByteArray(sampleSize.toInt())
                    aacFis.read(originalSample)
                    val modifiedSample = videoController.adjustSample(originalSample)
                    newAac.write(modifiedSample)
                    
                    Log.d(TAG, "Processing sample #${index + 1}, " +
                        "Original size: ${originalSample.size}, " +
                        "Modified size: ${modifiedSample.size}")
                }
            }
        }

        FileDataSourceImpl(download.tmpDst2).use { aacSourceFile ->
            val movie = Movie().apply {
                addTrack(AACTrackImpl(aacSourceFile))
            }
            
            val out = DefaultMp4Builder().build(movie)
            FileOutputStream(download.dst).use { output ->
                out.writeContainer(output.channel)
            }
        }
    }

    private suspend fun download(chunksData: Chunks): Map<Int, File> = withContext(Dispatchers.IO) {
        val chunks = chunksData.chunks
        Log.d(TAG, "Num Chunks: ${chunks.size}")
        
        if (chunks.isEmpty()) {
            throw InvalidSourceException("Something went wrong :(")
        }

        val chunkFileMap = sortedMapOf<Int, File>()
        Log.d(TAG, "Folder to write: ${download.tmpFolder.absolutePath}")
        
        val partitions = chunks.chunked(75)
        val total = partitions.size
        Log.d(TAG, "Number of partitions: $total")

        partitions.forEachIndexed { index, partition ->
            if (Thread.interrupted()) {
                throw InterruptedException()
            }

            EventBus.getDefault().post(ProgressEvent(taskID, progress.get().toLong(), total.toLong()))
            mBuilder.setProgress(100, Utils.normalizePercent(progress.get(), total), false)
            notificationManager.notify(taskID, mBuilder.build())
            
            Log.d(TAG, "Processing partition size: ${partition.size}")
            val doneSignal = CountDownLatch(partition.size)

            partition.forEach { chunk ->
                Ion.with(context)
                    .load(download.downloadUrl)
                    .addHeader("Accept-Ranges", "bytes")
                    .addHeader("Range", "bytes=${chunk.start}-${chunk.end}")
                    .group(dlGroup)
                    .write(File(download.tmpFolder.absolutePath, "${chunk.number}.part"))
                    .setCallback(FutureCallback { e, file ->
                        if (e == null && file != null) {
                            chunkFileMap[chunk.number] = file
                        } else {
                            Log.e(TAG, "Error downloading audio", e)
                        }
                        doneSignal.countDown()
                    })
            }
            doneSignal.await()
            progress.incrementAndGet()
        }

        if (chunkFileMap.size != chunks.size) {
            throw Exception("Some requests have failed")
        }

        return@withContext chunkFileMap
    }

    companion object {
        private const val TAG = "VideoTask"
    }
} 