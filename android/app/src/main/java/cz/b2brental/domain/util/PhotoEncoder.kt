@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Rozhraní pro kompresi a kódování fotografií poruchy do formátu Base64.
 * Implementace zajišťuje: downscale na max 1280 px, EXIF rotaci, JPEG kompresi
 * a kódování bez prefixu "data:" (čisté Base64, NO_WRAP).
 */
public interface PhotoEncoder {
    /**
     * Načte obrázek z Uri, zmenší jej na max 1280 px delší stranu, aplikuje EXIF rotaci,
     * zkomprimuje do JPEG (kvalita 70, při překročení 4 MB opakovaně 50) a zakóduje do Base64.
     * @param context aplikacni kontext pro cteni pres ContentResolver
     * @param imageUri Uri fotografie (z kamery i z galerie)
     * @return Base64 retez bez zalomeni radku a bez prefixu "data:"
     * @throws IllegalStateException pokud obrazek nelze dekodovat
     */
    public suspend fun encodeToBase64(context: Context, imageUri: Uri): String
}

/** Maximální povolená délka delší strany obrázku v pixelech. */
public const val PHOTO_MAX_SIDE_PX: Int = 1280

/** Maximální povolená velikost zakódovaného JPEG v bajtech (limit backendu). */
public const val PHOTO_MAX_BYTES: Int = 4 * 1024 * 1024

/**
 * Spočítá hodnotu inSampleSize pro BitmapFactory tak, aby po dělení 2^sample
 * delší strana nebyla výrazně větší než [maxSide].
 * @param width původní šířka obrázku
 * @param height původní výška obrázku
 * @param maxSide limit delší strany
 * @return hodnota inSampleSize (mocnina 2, minimum 1)
 */
public fun computeInSampleSize(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    val maxDim: Int = maxOf(width, height)
    while (maxDim / (sample * 2) >= maxSide) {
        sample *= 2
    }
    return sample
}

/**
 * Spočítá cílové rozměry po přesném škálování na max [maxSide] px delší stranu
 * se zachováním poměru stran. Zvětšování (upscale) se neprovádí.
 * @return pár (cílová šířka, cílová výška)
 */
@Suppress("KDocMissingDocumentation")
public fun scaledDimensions(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    val maxDim: Int = maxOf(width, height)
    if (maxDim <= maxSide) return Pair(width, height)
    val scale: Float = maxSide.toFloat() / maxDim.toFloat()
    val newWidth: Int = (width * scale).toInt().coerceAtLeast(1)
    val newHeight: Int = (height * scale).toInt().coerceAtLeast(1)
    return Pair(newWidth, newHeight)
}

/**
 * Vrátí kvalitu JPEG komprese podle velikosti výstupu v bajtech.
 * @param byteCount velikost komprimovaného JPEG v bajtech
 * @param maxBytes limit backendu (PHOTO_MAX_BYTES)
 * @return 70 pokud [byteCount] <= [maxBytes], jinak 50 (druhý pokus o kompresi)
 */
@Suppress("KDocMissingDocumentation")
public fun jpegQualityForBytes(byteCount: Int, maxBytes: Int): Int {
    return if (byteCount > maxBytes) 50 else 70
}

/**
 * Výchozí implementace [PhotoEncoder] nad android.graphics a ContentResolverem.
 */
public class DefaultPhotoEncoder : PhotoEncoder {

    @Suppress("KDocMissingDocumentation")
    override suspend fun encodeToBase64(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
        // 1) Čtení rozměrů bez načtení celého bitmapy do paměti
        val bounds: BitmapFactory.Options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IllegalStateException("Obrazek nelze otevrit: $imageUri")

        // 2) Downscale inSampleSize (hrubé zmenšení, úspora paměti)
        val options: BitmapFactory.Options = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, PHOTO_MAX_SIDE_PX)
        }
        val decoded: Bitmap = context.contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IllegalStateException("Obrazek nelze dekodovat: $imageUri")

        // 3) EXIF rotace dle orientace snímku (kaméra ukládá orientaci do metadat)
        val rotated: Bitmap = rotateByExif(context, imageUri, decoded)

        // 4) Přesné škálování na <= 1280 px delší stranu (inSampleSize samo nestačí)
        val (targetW: Int, targetH: Int) = scaledDimensions(rotated.width, rotated.height, PHOTO_MAX_SIDE_PX)
        val scaled: Bitmap = if (targetW != rotated.width || targetH != rotated.height) {
            rotated.scale(targetW, targetH, true)
        } else {
            rotated
        }

        // 5) JPEG komprese: kvalita 70; při překročení 4 MB znovu s kvalitou 50 (limit backendu)
        val firstBytes: ByteArray = compressToJpeg(scaled, 70)
        val finalBytes: ByteArray = if (firstBytes.size > PHOTO_MAX_BYTES) compressToJpeg(scaled, 50) else firstBytes

        // 6) Base64 NO_WRAP, bez prefixu "data:" (backend dekóduje čisté Base64)
        Base64.encodeToString(finalBytes, Base64.NO_WRAP)
    }

    /**
     * Aplikuje rotaci z EXIF metadat obrázku.
     * @return otočený bitmap nebo původní bitmap, pokud rotace není potřeba
     */
    @Suppress("KDocMissingDocumentation")
    private fun rotateByExif(context: Context, imageUri: Uri, source: Bitmap): Bitmap {
        val orientation: Int = context.contentResolver.openInputStream(imageUri)?.use { input ->
            try {
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees: Float = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return source
        val matrix: Matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Zkomprimuje bitmap do JPEG se zadanou kvalitou.
     * @return pole bajtů JPEG
     */
    @Suppress("KDocMissingDocumentation")
    private fun compressToJpeg(source: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        source.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }
}