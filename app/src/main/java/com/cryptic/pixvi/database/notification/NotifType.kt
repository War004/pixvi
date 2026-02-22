package com.cryptic.pixvi.database.notification

enum class NotifType(val id: Int, val displayName: String) {
    DOWNLOAD(0, displayName = "Download"),
    SYSTEM(1, "System"),
    AUTHOR(2, "Author"),
    PIXIV(id=3, "Pixiv")
}

enum class MediaType(val id: Int, val displayName: String){
    IMAGE(0,"Images"),
    PDF(1,"Pdf"),
    GIF(2,"Gif"),
    HTML(3,"Html")
}