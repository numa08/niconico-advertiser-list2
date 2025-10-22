package net.numa08.niconico_advertiser_list2.datasource.response

import kotlinx.serialization.Serializable

@Serializable
data class NicoadResponse(
    val meta: Meta,
    val data: Data,
) {
    @Serializable
    data class Meta(
        val status: Int,
    )

    @Serializable
    data class Data(
        val count: Int,
        val serverTime: Long,
        val histories: List<NicoadHistoryResponse>,
    )
}
