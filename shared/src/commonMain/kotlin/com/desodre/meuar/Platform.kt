package com.desodre.meuar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform