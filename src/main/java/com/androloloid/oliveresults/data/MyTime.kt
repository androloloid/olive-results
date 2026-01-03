/*
This file is part of O'Live Results.

O'Live Results is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

O'Live Results is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with O'Live Results. If
not, see <https://www.gnu.org/licenses/>

@Author: androloloid@gmail.com
@Date: 2026-01
 */

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

@Serializable(with = MyIntSerializer::class)
@JvmInline
value class MyTime(val value: Int) {
    // operator to cast MyTime to an Int
    operator fun invoke() = value
}

class MyTimeSerializer : KSerializer<MyTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MyTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MyTime) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): MyTime {
        val jsonInput = decoder as? JsonDecoder
            ?: throw IllegalStateException("This serializer can only be used with JSON")
        val element = jsonInput.decodeJsonElement()

        if (element !is JsonPrimitive) {
            return MyTime(0) // Default value for non-primitive JSON elements
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
        return MyTime(intValue)
    }
}