package com.jacobrozell.dartbuddy.support.test

import kotlinx.serialization.json.Json

object TestJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
}
