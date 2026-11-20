package com.example.foodjeetapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Valida la política declarativa de seguridad de red (Corrección 03 - Semana 12):
 * - AndroidManifest no habilita usesCleartextTraffic globalmente y apunta a network_security_config.
 * - La variante release (src/main) deniega todo tráfico HTTP y confía únicamente en CAs del sistema.
 * - Las variantes de desarrollo local (debug y localRelease) deniegan HTTP por defecto y solo
 *   habilitan 10.0.2.2 y localhost con includeSubdomains="false".
 */
class NetworkSecurityConfigTest {

    private fun appSrcDir(): File {
        val candidates = listOf(
            File("src"),
            File("app/src"),
            File("app-movil/app/src")
        )
        return candidates.first { File(it, "main/AndroidManifest.xml").exists() }
    }

    private fun parseXml(relativePath: String): Element {
        val file = File(appSrcDir(), relativePath)
        assertTrue("Debe existir ${file.path}", file.exists())
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(file).documentElement
    }

    @Test
    fun `AndroidManifest bloquea cleartext global y declara networkSecurityConfig`() {
        val manifest = parseXml("main/AndroidManifest.xml")
        val appEl = manifest.getElementsByTagName("application").item(0) as Element

        assertEquals("false", appEl.getAttribute("android:usesCleartextTraffic"))
        assertEquals("@xml/network_security_config", appEl.getAttribute("android:networkSecurityConfig"))
    }

    @Test
    fun `politica release en main bloquea todo HTTP y confia solo en certificados del sistema`() {
        val root = parseXml("main/res/xml/network_security_config.xml")
        val baseConfig = root.getElementsByTagName("base-config").item(0) as? Element
        assertNotNull(baseConfig)
        assertEquals("false", baseConfig!!.getAttribute("cleartextTrafficPermitted"))

        val domainConfigs = root.getElementsByTagName("domain-config")
        for (i in 0 until domainConfigs.length) {
            val dc = domainConfigs.item(i) as Element
            assertNotEqualsTrue(dc.getAttribute("cleartextTrafficPermitted"))
        }

        val certs = root.getElementsByTagName("certificates")
        assertTrue("Debe declarar certificados del sistema", certs.length > 0)
        for (i in 0 until certs.length) {
            val cert = certs.item(i) as Element
            assertEquals("system", cert.getAttribute("src"))
        }
    }

    @Test
    fun `politicas debug y localRelease solo permiten HTTP hacia 10_0_2_2 y localhost`() {
        listOf(
            "debug/res/xml/network_security_config.xml",
            "localRelease/res/xml/network_security_config.xml"
        ).forEach { path ->
            val root = parseXml(path)
            val baseConfig = root.getElementsByTagName("base-config").item(0) as Element
            assertEquals("base-config debe denegar HTTP en $path", "false", baseConfig.getAttribute("cleartextTrafficPermitted"))

            val domainConfigs = root.getElementsByTagName("domain-config")
            assertEquals("Solo debe existir un bloque domain-config en $path", 1, domainConfigs.length)
            val dc = domainConfigs.item(0) as Element
            assertEquals("true", dc.getAttribute("cleartextTrafficPermitted"))

            val domainNodes = dc.getElementsByTagName("domain")
            val hosts = mutableSetOf<String>()
            for (i in 0 until domainNodes.length) {
                val d = domainNodes.item(i) as Element
                assertEquals("includeSubdomains debe ser false en $path", "false", d.getAttribute("includeSubdomains"))
                hosts += d.textContent.trim()
            }
            assertEquals("Hosts HTTP autorizados en $path", setOf("10.0.2.2", "localhost"), hosts)
        }
    }

    private fun assertNotEqualsTrue(value: String?) {
        assertFalse("Release nunca debe permitir cleartextTrafficPermitted=true", value.equals("true", ignoreCase = true))
    }
}
