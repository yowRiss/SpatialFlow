package com.codetrio.spatialflow.shared.account

import com.codetrio.spatialflow.shared.data.innertube.InnerTubeClient
import java.util.prefs.Preferences
import org.cef.callback.CefCookieVisitor
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager

/** Desktop counterpart of Android AccountManager: owns the authenticated
 * YouTube Music cookie and immediately hydrates the shared InnerTube client. */
class DesktopYouTubeSession(private val client: InnerTubeClient) {
    private val preferences = Preferences.userRoot().node("com/codetrio/spatialflow/account")
    init { client.updateCookie(preferences.get("yt_cookies", null)) }
    fun saveCookie(cookie: String) { preferences.put("yt_cookies", cookie); client.updateCookie(cookie) }
    fun clear() { preferences.remove("yt_cookies"); client.updateCookie(null) }
    fun isLoggedIn(): Boolean = !preferences.get("yt_cookies", "").isBlank()
    fun captureFromEmbeddedBrowser(onCaptured: (Boolean) -> Unit) {
        val cookies = mutableListOf<CefCookie>()
        CefCookieManager.getGlobalManager().visitUrlCookies("https://music.youtube.com", true, object : CefCookieVisitor {
            override fun visit(cookie: CefCookie, count: Int, total: Int, deleteCookie: BoolRef): Boolean {
                cookies += cookie
                if (count + 1 == total) {
                    val header = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    val authenticated = header.contains("SAPISID=") && header.contains("HSID=")
                    if (authenticated) saveCookie(header)
                    onCaptured(authenticated)
                }
                return true
            }
        })
    }
}
