package com.perkelle.dev.mcpingercli

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.experimental.*

/**
 * @author DotRar (https://github.com/Dot-Rar) & zh32 (https://github.com/zh32)
 */
class ServerPinger(val host: String, val port: Int = 25565, val stripColour: Boolean = false) {

    val jsonColour = mapOf(
            "black" to '0',
            "dark_blue" to '1',
            "dark_green" to '2',
            "dark_aqua" to '3',
            "dark_red" to '4',
            "dark_purple" to '5',
            "gold" to '6',
            "gray" to '7',
            "dark_gray" to '8',
            "blue" to '9',
            "green" to 'a',
            "aqua" to 'b',
            "red" to 'c',
            "light_purple" to 'd',
            "yellow" to 'e',
            "white" to 'f'
    )

    fun readVarInt(inStream: DataInputStream): Int {
        try {
            var i = 0
            var j = 0
            while (true) {
                val byte = inStream.readByte()
                i = i or (byte and 0x7F shl j++ * 7)
                if (j > 5) return -1
                if ((byte and 0x80.toByte()) != 128.toByte()) break
            }
            return i
        } catch(_: Exception) {
            return -1
        }
    }

    fun writeVarInt(out: DataOutputStream, param: Int): Int {
        try {
            while(true) {
                if((param.toByte() and 0xFFFFFF80.toByte()) == 0.toByte()) {
                    out.writeByte(param)
                    return -1
                }

                out.writeByte(param and 0x7F or 0x80)
                param ushr 7

                return 0
            }
        } catch(_: Exception) {
            return -1
        }
    }

    fun getServerInfo(): JSONObject {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 7000)
            val out = socket.getOutputStream()
            val dataOutput = DataOutputStream(out)
            val inStream = socket.getInputStream()
            val dataIn = DataInputStream(inStream)

            val address = InetSocketAddress(this.host, this.port)
            if(address.isUnresolved) {
                return json {
                    put("httpCode", 200)
                    put("success", false)
                    put("error", "The target cannot be resolved")
                }
            }

            val b = ByteArrayOutputStream()
            val handshake = DataOutputStream(b)
            handshake.writeByte(0x00)
            writeVarInt(handshake, 4)
            writeVarInt(handshake, address.hostString.length)
            handshake.writeBytes(address.hostString)
            handshake.writeShort(port)
            writeVarInt(handshake, 1)

            writeVarInt(dataOutput, b.size())
            dataOutput.write(b.toByteArray())

            dataOutput.writeByte(0x01)
            dataOutput.writeByte(0x00)

            readVarInt(dataIn)
            var id = readVarInt(dataIn)

            if(id == -1) return invalidPacket()
            if(id != 0x00) return invalidPacket()

            val length = readVarInt(dataIn)

            if(length == -1) return invalidPacket()
            if(length == 0) return invalidPacket()

            val bytesIn = ByteArray(length)
            dataIn.readFully(bytesIn)
            val jsonIn = JSONObject(String(bytesIn))

            val now = System.currentTimeMillis()
            dataOutput.writeByte(0x09) //handshake
            dataOutput.writeByte(0x01) //ping
            dataOutput.writeLong(now)

            readVarInt(dataIn)
            id = readVarInt(dataIn)
            if(id == -1) return invalidPacket()
            if(id != 0x01) return invalidPacket()

            val motd by lazy {
                if(!jsonIn.has("description")) ""
                val description = jsonIn["description"]
                if(description is String) {
                    if(stripColour) description.removeColour()
                    else description
                }
                else if(description is JSONObject && description.has("extra") && description["extra"] is JSONArray) {
                    var desc = ""
                    val array = description.getJSONArray("extra")
                    for(i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)

                        if(!stripColour) {
                            if(obj.has("color")) {
                                val colour = obj.getString("color")
                                if(jsonColour.containsKey(colour)) {
                                    desc+="§"+jsonColour[colour]
                                }
                            }
                            if(obj.has("bold") && obj.getBoolean("bold")) desc+="§l"
                            if(obj.has("italic") && obj.getBoolean("italic")) desc+="§o"
                            if(obj.has("underlined") && obj.getBoolean("underlined")) desc+="§n"
                            if(obj.has("strikethrough") && obj.getBoolean("strikethrough")) desc+="§m"
                            if(obj.has("obfuscated") && obj.getBoolean("obfuscated")) desc+="§k"
                        }

                        desc+=obj.getString("text")
                    }
                    desc
                }
                else if(description is JSONObject && description.has("text") && description["text"] is String) {
                    val desc = description.getString("text")
                    if(stripColour) desc.removeColour()
                    else desc
                }
                else ""
            }

            val response = Response(
                    this.host,
                    this.port,
                    true,
                    motd,
                    jsonIn.getJSONObject("players").getInt("online"),
                    jsonIn.getJSONObject("players").getInt("max"),
                    jsonIn.getString("favicon"),
                    jsonIn.getJSONObject("version").getInt("protocol"),
                    jsonIn.getJSONObject("version").getString("name"),
                    (System.currentTimeMillis()-dataIn.readLong()).toInt()
            )

            socket.close()
            dataIn.close()
            dataOutput.close()
            inStream.close()
            out.close()

            return json {
                put("httpCode", 200)
                put("success", true)
                put("response", response.toJson())
            }
        } catch(ex: Exception) {
            ex.printStackTrace()
            return json {
                put("httpCode", 200)
                put("success", false)
                put("error", "The requested server is offline")
            }
        }
    }

    fun invalidPacket(): JSONObject {
        return json {
            put("httpCode", 200)
            put("success", false)
            put("error", "Invalid response from the server ")
        }
    }

    fun String.removeColour(): String {
        var previousSection = false
        var newString = ""
        for(i in 0..this.length-1) {
            if(previousSection) {
                previousSection = false
                continue
            }
            val char = this[i]
            if(char == '§') {
                previousSection = true
                continue
            }
            else newString += char
        }
        return newString
    }

    data class Response(
            val hostname: String,
            val port: Int,
            val online: Boolean,
            val motd: String,
            val playerCount: Int,
            val maxPlayers: Int,
            val favicon: String,
            val protocol: Int,
            val versionName: String,
            val ping: Int
    )

    fun Response.toJson() = json {
        put("hostname", hostname)
        put("port", port)
        put("online", online)
        put("motd", motd)
        put("playerCount", playerCount)
        put("maxPlayers", maxPlayers)
        put("favicon", favicon)
        put("protocol", protocol)
        put("versionName", versionName)
        put("ping", ping)
    }
}

infix fun Byte.shl(shift: Int): Int = this.toInt() shl shift