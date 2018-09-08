package com.perkelle.dev.mcpingercli

import org.json.JSONObject
import java.util.*

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    print("Enter the server IP: ")
    val input = scanner.next()

    val host by lazy {
        if(input.contains(":")) input.split(":")[0]
        else input
    }

    val port by lazy {
        if(input.contains(":")) input.split(":")[1].toIntOrNull() ?: 25565
        else 25565
    }

    val pinger = ServerPinger(host, port, true)
    val response = pinger.getServerInfo()

    response.keySet().forEach { key ->
        val value = response[key]

        if(value is JSONObject) {
            value.keySet().forEach { key ->
                println("$key: ${value[key]}")
            }
        }

        else println("$key: ${response[key]}")
    }
}