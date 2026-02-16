package com.toutakun04.forceflow.data.model

data class ApiResponse<T>(
    val status: String,
    val result: T?,
    val comment: String? = null
)
