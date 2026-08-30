package cz.b2brental.domain

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.javamoney.moneta.Money

// Serializace peněz do JSON: řetězec s maximálně 2 desetinnými místy, měna CZK se nepřenáší
object MoneySerializer : KSerializer<Money> {
    override val descriptor = PrimitiveSerialDescriptor("Money", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Money) {
        encoder.encodeString(value.toDbBigDecimal().toPlainString())
    }

    override fun deserialize(decoder: Decoder): Money {
        val raw = decoder.decodeString()
        val amount = try {
            BigDecimal(raw)
        } catch (e: NumberFormatException) {
            throw SerializationException("Neplatný formát částky: $raw")
        }
        if (amount.scale() > 2) {
            throw SerializationException("Částka smí mít maximálně 2 desetinná místa: $raw")
        }
        return Money.of(amount, CZK)
    }
}
