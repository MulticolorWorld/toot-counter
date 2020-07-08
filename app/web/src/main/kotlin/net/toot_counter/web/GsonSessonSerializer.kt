package net.toot_counter.web

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.ktor.sessions.SessionSerializer
import java.time.LocalDateTime

class GsonSessionSerializer : SessionSerializer<SessionEntity> {

    val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()

    override fun serialize(session: SessionEntity): String = gson.toJson(session) + "\n"
    override fun deserialize(text: String): SessionEntity = gson.fromJson(text, SessionEntity::class.java)
}

class LocalDateTimeAdapter : TypeAdapter<LocalDateTime>() {

    override fun write(writer: JsonWriter, value: LocalDateTime) {
        writer.value(value.toString())
    }

    override fun read(reader: JsonReader): LocalDateTime {
        return LocalDateTime.parse(reader.nextString())
    }

}