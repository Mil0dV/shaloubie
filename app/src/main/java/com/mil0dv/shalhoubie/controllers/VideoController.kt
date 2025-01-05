@Singleton
class VideoController @Inject constructor() {
    suspend fun getChunks(url: String): Chunks {
        // Implementation
    }
    
    fun adjustSample(sample: ByteArray): ByteArray {
        // Implementation
        return sample
    }
} 