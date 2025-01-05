import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mil0dv.shalhoubie.adapters.VideoAdapter
import com.mil0dv.shalhoubie.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchButton()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter { video ->
            // Handle download click
            // Start your download task here
        }

        binding.recyclerViewVideos.apply {
            adapter = videoAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }
} 