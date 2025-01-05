object Utils {
    fun normalizePercent(progress: Int, total: Int): Int {
        return ((progress.toFloat() / total.toFloat()) * 100).toInt()
    }
} 