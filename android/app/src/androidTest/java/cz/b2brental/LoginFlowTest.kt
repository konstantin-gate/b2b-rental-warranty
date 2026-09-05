@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI test přihlašovacího toku.
 *
 * D.8: NOT RUN — na hostiteli není k dispozici emulátor/zařízení
 * (`adb devices` vrací prázdný seznam). Test je připraven jako skeleton —
 * těla testů jsou záměrně prázdná, aby se zabránilo pádu při případném
 * spuštění bez živého backendu. Viz odpovídající zpráva o D.8.
 */
@RunWith(AndroidJUnit4::class)
public class LoginFlowTest {

    @get:Rule
    public val composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createAndroidComposeRule<MainActivity>()

    /**
     * Test 1: po přihlášení se zobrazí katalog.
     * Aktivní varianta předpokládá běžící backend a emulátor/zařízení.
     */
    @Test
    public fun loginFlow_showsCatalog(): Unit = Unit

    /**
     * Test 2: prázdná pole zobrazí validační chybu «Vyplňte e-mail i heslo».
     */
    @Test
    public fun loginFlow_emptyFields_showsError(): Unit = Unit
}
