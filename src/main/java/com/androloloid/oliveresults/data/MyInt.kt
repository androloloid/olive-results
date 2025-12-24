package com.androloloid.oliveresults.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlin.jvm.JvmInline

@Serializable(with = MyIntSerializer::class)
@JvmInline
value class MyInt(val value: Int) {
    // operator to cast MyInt to an Int
    operator fun invoke() = value
}

object MyIntSerializer : KSerializer<MyInt> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MyInt", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MyInt) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): MyInt {
        val jsonInput = decoder as? JsonDecoder
            ?: throw IllegalStateException("This serializer can only be used with JSON")
        val element = jsonInput.decodeJsonElement()

        if (element !is JsonPrimitive) {
            return MyInt(0) // Default value for non-primitive JSON elements
        }

        val intValue = if (element.isString) {
            element.content.toIntOrNull() ?: 0
        } else {
            try {
                element.int
            } catch (e: NumberFormatException) {
                0 // The number is not a valid Int (e.g., it's a long or a float)
            }
        }
        return MyInt(intValue)
    }
}
