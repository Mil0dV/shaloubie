package com.mil0dv.shalhoubie.models

data class YouTubeVideo(
    val id: VideoId,
    val snippet: VideoSnippet
)

data class VideoId(
    val videoId: String
)

data class VideoSnippet(
    val title: String,
    val thumbnails: VideoThumbnails
)

data class VideoThumbnails(
    val default: Thumbnail
)

data class Thumbnail(
    val url: String
) 