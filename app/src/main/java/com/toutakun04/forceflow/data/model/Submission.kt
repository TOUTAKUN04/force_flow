package com.toutakun04.forceflow.data.model

data class Submission(
    val id: Long,
    val creationTimeSeconds: Long,
    val problem: Problem,
    val verdict: String?
)

data class Problem(
    val contestId: Int?,
    val index: String,
    val name: String,
    val rating: Int?,
    val tags: List<String>?
)
