package com.maxrave.domain.utils

/** A filesystem path must never become part of an external artwork request. */
fun youtubeArtworkOrEmpty(videoId: String?): String =
    if (videoId != null && Regex("[A-Za-z0-9_-]{11}").matches(videoId)) {
        "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
    } else ""
