abstract class BaseTask {
    protected lateinit var currentThread: Thread
    protected lateinit var mBuilder: NotificationCompat.Builder
    protected lateinit var notificationManager: NotificationManager
    protected lateinit var context: Context
    protected val dlGroup = "download_group"
    protected val progress = AtomicInteger(0)
    
    abstract suspend fun run()
    
    protected fun complete() {
        // Implementation
    }
    
    protected fun cancelled() {
        // Implementation
    }
    
    protected fun failed() {
        // Implementation
    }
} 