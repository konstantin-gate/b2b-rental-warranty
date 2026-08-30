package cz.b2brental.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.javamoney.moneta.Money
import java.math.BigDecimal

/** Serializace peněz do JSON: řetězec s maximálně 2 desetinnými místy, měna CZK se nepřenáší */
public object MoneySerializer : KSerializer<Money> {
    @Suppress("HardCodedStringLiteral")
    public override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Money", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Money,
    ) {
        encoder.encodeString(value.toDbBigDecimal().toPlainString())
    }

    override fun deserialize(decoder: Decoder): Money {
        val raw = decoder.decodeString()
        val amount =
            try {
                BigDecimal(raw)
            } catch (_: NumberFormatException) {
                throw SerializationException("Neplatný formát částky: $raw")
            }
        if (amount.scale() > 2) {
            throw SerializationException("Částka smí mít maximálně 2 desetinná místa: $raw")
        }
        return Money.of(amount, CZK)
    }
}
