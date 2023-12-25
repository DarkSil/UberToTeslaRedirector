package com.waze

data class FetchData(
    private val status: String,
    val date: Long,
    val period: Int,
    val updateRequired: Boolean = false
) {

    fun getStatus() : STATUS {
        return STATUS.entries.filter { it.status == status }[0]
    }
    enum class STATUS(val status: String) {
        TEST("test"),
        PAID("paid"),
        UNPAID("unpaid")
    }
}
