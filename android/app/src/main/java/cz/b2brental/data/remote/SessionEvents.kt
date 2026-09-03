package cz.b2brental.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Globální události relace — singleton pro přenos událostí mezi síťovou vrstvou a UI.
 * Používá se k oznamování vypršení session (401) napříč vrstvami aplikace.
 */
public object SessionEvents {

    /**
     * Událost vypršení session (odpověď 401 z API).
     * UI se přihlásí a přepne na přihlašovací obrazovku.
     */
    public val unauthorized: MutableSharedFlow<Unit> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1,
    )
}
