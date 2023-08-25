package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import okhttp3.Headers

abstract class UserAuthStateEndpoint: YoutubeApi.Endpoint() {
    abstract fun byChannelAndHeaders(own_channel: Artist?, headers: Headers): Result<YoutubeApi.UserAuthState>
    abstract suspend fun byHeaders(headers: Headers): Result<YoutubeApi.UserAuthState>

    fun bySetData(set: Set<String>) {
        val data = YoutubeApi.UserAuthState.unpackSetData(set)
        byChannelAndHeaders(data.first, data.second)
    }
}