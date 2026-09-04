@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.domain.util.PHOTO_MAX_BYTES
import cz.b2brental.domain.util.computeInSampleSize
import cz.b2brental.domain.util.jpegQualityForBytes
import cz.b2brental.domain.util.scaledDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Čisté (JVM) testy nad pomocnými funkcemi [cz.b2brental.domain.util.PhotoEncoder].
 * Skutečné zpracování Bitmapy / ContentResolveru nelze na JVM testovat
 * (Android třídy nejsou zamokovány) — proto je zde pokryta pouze čistá logika.
 */
public class PhotoEncoderLogicTest {

    /**
     * Oversize obrázek (4000x3000) při limitu 1280 px → sample=2
     * (4000/2=2000 >= 1280 → sample=2; 4000/4=1000 < 1280 → stop).
     */
    @Test
    public fun computeInSampleSize_oversize_returns2(): Unit {
        assertEquals(2, computeInSampleSize(4000, 3000, 1280))
    }

    /**
     * Přesně na limitu (1280x960) → sample=1 (bez downscale).
     */
    @Test
    public fun computeInSampleSize_atLimit_returns1(): Unit {
        assertEquals(1, computeInSampleSize(1280, 960, 1280))
    }

    /**
     * Menší než limit → sample=1 (bez downscale).
     */
    @Test
    public fun computeInSampleSize_small_returns1(): Unit {
        assertEquals(1, computeInSampleSize(640, 480, 1280))
    }

    /**
     * Oversize 2000x1500 → cíl 1280x960 (poměr stran zachován).
     */
    @Test
    public fun scaledDimensions_oversize_returnsScaledPair(): Unit {
        assertEquals(Pair(1280, 960), scaledDimensions(2000, 1500, 1280))
    }

    /**
     * Přesně na limitu → zůstává původní rozměr.
     */
    @Test
    public fun scaledDimensions_atLimit_returnsSame(): Unit {
        assertEquals(Pair(1280, 1280), scaledDimensions(1280, 1280, 1280))
    }

    /**
     * Menší než limit → žádný upscale (zachová rozměr).
     */
    @Test
    public fun scaledDimensions_small_returnsSameNoUpscale(): Unit {
        assertEquals(Pair(600, 800), scaledDimensions(600, 800, 1280))
    }

    /**
     * Oversize 3000x2000 → cíl 1280x853 (zaokrouhleno dolů).
     */
    @Test
    public fun scaledDimensions_3000x2000_returns1280x853(): Unit {
        assertEquals(Pair(1280, 853), scaledDimensions(3000, 2000, 1280))
    }

    /**
     * 1 MB výstup při limitu 4 MB → kvalita 70.
     */
    @Test
    public fun jpegQualityForBytes_belowLimit_returns70(): Unit {
        assertEquals(70, jpegQualityForBytes(1 * 1024 * 1024, PHOTO_MAX_BYTES))
    }

    /**
     * Přesně na limitu → kvalita 70 (limit je přípustný).
     */
    @Test
    public fun jpegQualityForBytes_atLimit_returns70(): Unit {
        assertEquals(70, jpegQualityForBytes(PHOTO_MAX_BYTES, PHOTO_MAX_BYTES))
    }

    /**
     * O 1 bajt přes limit → kvalita 50 (druhý pokus).
     */
    @Test
    public fun jpegQualityForBytes_overLimit_returns50(): Unit {
        assertEquals(50, jpegQualityForBytes(PHOTO_MAX_BYTES + 1, PHOTO_MAX_BYTES))
    }
}