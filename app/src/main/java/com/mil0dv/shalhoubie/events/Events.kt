data class StartEvent(val download: Download)
data class ProgressEvent(val taskId: Int, val progress: Long, val total: Long) 