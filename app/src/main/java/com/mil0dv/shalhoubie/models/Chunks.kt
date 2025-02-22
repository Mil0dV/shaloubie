package com.mil0dv.shalhoubie.models

import com.mil0dv.shalhoubie.models.SampleSizeBox

data class Chunks(
    val chunks: List<Chunk>,
    val sampleSizeBox: SampleSizeBox
)

data class Chunk(
    val number: Int,
    val start: Long,
    val end: Long
) 