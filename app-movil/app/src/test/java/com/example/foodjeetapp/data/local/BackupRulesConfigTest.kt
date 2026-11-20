package com.example.foodjeetapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Valida la configuración declarativa de protección de respaldos (Corrección 02 - Semana 12):
 * - AndroidManifest.xml deshabilita allowBackup y referencia ambas reglas XML.
 * - backup_rules.xml (API 29-30) excluye datastore, database y sharedpref.
 * - data_extraction_rules.xml (API 31+) excluye datastore, database y sharedpref tanto en
 *   cloud-backup como en device-transfer.
 */
class BackupRulesConfigTest {

    private fun appMainDir(): File {
        val candidates = listOf(
            File("src/main"),
            File("app/src/main"),
            File("app-movil/app/src/main")
        )
        return candidates.first { File(it, "AndroidManifest.xml").exists() }
    }

    private fun parseXml(file: File): Element {
        assertTrue("Debe existir ${file.path}", file.exists())
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        return factory.newDocumentBuilder().parse(file).documentElement
    }

    private fun collectExcludes(parent: Element): Set<Pair<String, String>> {
        val nodes = parent.getElementsByTagName("exclude")
        val result = mutableSetOf<Pair<String, String>>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            result += el.getAttribute("domain") to el.getAttribute("path")
        }
        return result
    }

    @Test
    fun `AndroidManifest deshabilita allowBackup y declara reglas para API 29-30 y 31+`() {
        val manifest = parseXml(File(appMainDir(), "AndroidManifest.xml"))
        val appEl = manifest.getElementsByTagName("application").item(0) as Element

        assertEquals("false", appEl.getAttribute("android:allowBackup"))
        assertEquals("@xml/backup_rules", appEl.getAttribute("android:fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", appEl.getAttribute("android:dataExtractionRules"))
    }

    @Test
    fun `backup_rules excluye DataStore de sesion, Room y SharedPreferences`() {
        val root = parseXml(File(appMainDir(), "res/xml/backup_rules.xml"))
        assertEquals("full-backup-content", root.tagName)
        val excludes = collectExcludes(root)

        assertTrue("Debe excluir files/datastore", ("file" to "datastore") in excludes)
        assertTrue("Debe excluir bases de datos Room", ("database" to ".") in excludes)
        assertTrue("Debe excluir SharedPreferences", ("sharedpref" to ".") in excludes)
    }

    @Test
    fun `data_extraction_rules excluye sesion en cloud-backup y device-transfer`() {
        val root = parseXml(File(appMainDir(), "res/xml/data_extraction_rules.xml"))
        assertEquals("data-extraction-rules", root.tagName)

        val cloudBackup = root.getElementsByTagName("cloud-backup").item(0) as? Element
        val deviceTransfer = root.getElementsByTagName("device-transfer").item(0) as? Element

        assertNotNull("Debe declarar <cloud-backup>", cloudBackup)
        assertNotNull("Debe declarar <device-transfer> para bloquear D2D en Android 12+", deviceTransfer)
        assertEquals("true", cloudBackup!!.getAttribute("disableIfNoEncryptionCapabilities"))

        val cloudExcludes = collectExcludes(cloudBackup)
        val d2dExcludes = collectExcludes(deviceTransfer!!)

        listOf("file" to "datastore", "database" to ".", "sharedpref" to ".").forEach { expected ->
            assertTrue("cloud-backup debe excluir $expected", expected in cloudExcludes)
            assertTrue("device-transfer debe excluir $expected", expected in d2dExcludes)
        }
    }
}
