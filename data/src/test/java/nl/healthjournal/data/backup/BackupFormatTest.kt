package nl.healthjournal.data.backup

import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupException
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer

/**
 * Decodes the published test vectors in `docs/backup-format/test-vectors` with the app's own code, so the
 * published format and the implementation cannot drift apart. Runs under Robolectric for `org.json`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupFormatTest {

    private val passphrase = "correct horse battery staple".toCharArray()
    private val kdf = Argon2KeyDerivation()
    private val fast = BackupContainer(kdf, memoryKiB = 1024, iterations = 1)

    private val vectors = File("../docs/backup-format/test-vectors")
    private fun vector(name: String) = File(vectors, name).readBytes()
    private fun sample() = File(vectors, "sample.json").readText()

    /** Parsed JSON as plain Kotlin values, without the derived fields the reader recomputes. */
    private fun raw(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence()
            .filter { it != "bmi" && it != "category" }
            .associateWith { raw(value.get(it)) }
        is JSONArray -> List(value.length()) { raw(value.get(it)) }
        JSONObject.NULL -> null
        else -> value
    }

    @Test
    fun `default vector (64 MiB, gzip) decodes to the sample document`() {
        val doc = HjBackupCodec(BackupContainer(kdf)).decode(vector("sample-default.hjbackup"), passphrase)
        assertEquals(raw(JSONObject(sample())), raw(JSONObject(String(BackupJson.write(doc), Charsets.UTF_8))))
    }

    @Test
    fun `fast vector decodes to the same document as the default vector`() {
        val codec = HjBackupCodec(fast)
        val a = codec.decode(vector("sample-default.hjbackup"), passphrase)
        val b = codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase)
        assertEquals(a, b)
    }

    @Test
    fun `decoded vector carries the expected content`() {
        val doc = HjBackupCodec(fast).decode(vector("sample-fast-nogzip.hjbackup"), passphrase)
        assertEquals("1.6.0", doc.appVersion)
        assertEquals("nl", doc.preferences?.language)
        assertEquals(30, doc.preferences?.reminderSnoozeMinutes)
        val profile = doc.profiles.single()
        assertTrue(profile.isActive)
        assertEquals("Sample Person", profile.profile.name)
        assertEquals(2, profile.weights.size)
        assertEquals("after a walk, \"light\" breakfast", profile.weights[1].comment?.text)
        assertEquals("café ☕", profile.waistCircumferences.single().comment?.text)
        // Derived values are recomputed, not read: 82.4 kg at 178 cm
        assertEquals("26.0", profile.weights[0].bmi?.toPlainString())
        val med = profile.medications.single()
        assertEquals(2, med.intakes.size)
        assertEquals(2, med.medication.schedules.single().let { (it.schedule as nl.healthjournal.domain.model.medication.Schedule.Recurring).times.size })
    }

    @Test
    fun `writing the decoded document gives the sample back apart from derived fields`() {
        val doc = HjBackupCodec(fast).decode(vector("sample-fast-nogzip.hjbackup"), passphrase)
        val written = raw(JSONObject(String(BackupJson.write(doc), Charsets.UTF_8)))
        assertEquals(raw(JSONObject(sample())), written)
    }

    @Test
    fun `round trip through the container keeps the document`() {
        val codec = HjBackupCodec(fast)
        val doc = codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase)
        val file = codec.encode(doc, passphrase)
        assertEquals(doc, codec.decode(file, passphrase))
        assertArrayEquals(BackupContainer.MAGIC, file.copyOfRange(0, 8))
    }

    @Test
    fun `two files of the same document differ because salt and nonce are random`() {
        val codec = HjBackupCodec(fast)
        val doc = codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase)
        assertFalse(codec.encode(doc, passphrase).contentEquals(codec.encode(doc, passphrase)))
    }

    @Test
    fun `wrong passphrase is refused`() {
        assertThrows(BackupException.WrongPassphraseOrCorrupt::class.java) {
            HjBackupCodec(fast).decode(vector("sample-fast-nogzip.hjbackup"), "wrong passphrase".toCharArray())
        }
    }

    @Test
    fun `passphrase is normalised so composed and decomposed forms open the same file`() {
        val codec = HjBackupCodec(fast)
        val doc = codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase)
        val file = codec.encode(doc, "café café".toCharArray())
        assertNotNull(codec.decode(file, "café café".toCharArray()))
    }

    @Test
    fun `changing any single byte makes the file fail`() {
        val original = vector("sample-fast-nogzip.hjbackup")
        // Sampled positions across header, salt, nonce, ciphertext and tag keep the test fast (each try runs Argon2).
        val positions = listOf(0, 7, 8, 10, 11, 12, 13, 14, 15, 16, 20, 24, 28, 44, 56, 100, original.size - 17, original.size - 1)
        for (pos in positions) {
            val tampered = original.copyOf().also { it[pos] = (it[pos].toInt() xor 0x01).toByte() }
            assertThrows("byte $pos", BackupException::class.java) { HjBackupCodec(fast).decode(tampered, passphrase) }
        }
    }

    @Test
    fun `truncated file fails`() {
        val original = vector("sample-fast-nogzip.hjbackup")
        for (len in listOf(0, 5, 8, 30, 55, 56, 60, original.size - 1)) {
            assertThrows("length $len", BackupException::class.java) {
                HjBackupCodec(fast).decode(original.copyOf(len), passphrase)
            }
        }
    }

    @Test
    fun `not a backup is reported as such`() {
        assertThrows(BackupException.NotABackup::class.java) {
            HjBackupCodec(fast).decode("name,date\n1,2\n".toByteArray(), passphrase)
        }
    }

    /** Hostile headers must be refused before any key is derived: the KDF port is never called. */
    @Test
    fun `hostile headers are refused before deriving a key`() {
        val original = vector("sample-fast-nogzip.hjbackup")
        val neverCalled = BackupContainer(object : nl.healthjournal.domain.port.secondary.BackupKeyDerivationPort {
            override fun derive(passphrase: ByteArray, salt: ByteArray, memoryKiB: Int, iterations: Int, parallelism: Int, length: Int): ByteArray =
                throw AssertionError("key derivation must not run")
        })
        fun mutate(block: (ByteBuffer) -> Unit): ByteArray {
            val copy = original.copyOf()
            block(ByteBuffer.wrap(copy))
            return copy
        }
        val cases = mapOf(
            "version" to mutate { it.putShort(8, 2) },
            "kdf" to mutate { it.put(10, 2) },
            "cipher" to mutate { it.put(11, 2) },
            "compression" to mutate { it.put(12, 2) },
            "lanes zero" to mutate { it.put(13, 0) },
            "lanes 17" to mutate { it.put(13, 17) },
            "salt length" to mutate { it.put(14, 15) },
            "nonce length" to mutate { it.put(15, 13) },
            "memory above 1 GiB" to mutate { it.putInt(16, 1_048_577) },
            "memory below 8 x lanes" to mutate { it.putInt(16, 7) },
            "huge memory" to mutate { it.putInt(16, -1) },
            "iterations zero" to mutate { it.putInt(20, 0) },
            "iterations 101" to mutate { it.putInt(20, 101) },
            "reserved" to mutate { it.putInt(24, 1) }
        )
        for ((name, file) in cases) {
            assertThrows(name, BackupException.Unsupported::class.java) { HjBackupCodec(neverCalled).decode(file, passphrase) }
        }
    }

    @Test
    fun `a newer schema version is refused with a clear reason`() {
        val newer = sample().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        val file = fast.seal(newer.toByteArray(), passphrase)
        assertThrows(BackupException.Unsupported::class.java) { HjBackupCodec(fast).decode(file, passphrase) }
    }

    @Test
    fun `an invalid entry makes the file malformed instead of importing part of it`() {
        val bad = sample().replace("\"weightKg\": \"82.4\"", "\"weightKg\": \"0.1\"")
        val file = fast.seal(bad.toByteArray(), passphrase)
        val e = assertThrows(BackupException.Malformed::class.java) { HjBackupCodec(fast).decode(file, passphrase) }
        assertFalse(e.message!!.contains("0.1"))
    }

    @Test
    fun `unknown properties and unknown preference values are tolerated`() {
        val root = JSONObject(sample())
        root.put("somethingNew", "x")
        root.getJSONObject("preferences").put("theme", "sepia").put("reminderSnoozeMinutes", 7)
        val doc: BackupDocument = BackupJson.read(root.toString().toByteArray())
        assertEquals("system", doc.preferences?.theme)
        assertEquals(30, doc.preferences?.reminderSnoozeMinutes)
    }

    @Test
    fun `document without preferences reads as null`() {
        val root = JSONObject(sample())
        root.remove("preferences")
        assertEquals(null, BackupJson.read(root.toString().toByteArray()).preferences)
    }

    private fun withMigraine(doc: BackupDocument): BackupDocument {
        val p = doc.profiles.single()
        val trigger = nl.healthjournal.domain.model.migraine.MigraineTag(
            nl.healthjournal.domain.model.migraine.MigraineTagId.generate(), p.profile.id,
            nl.healthjournal.domain.model.migraine.MigraineTagKind.TRIGGER, "skipped lunch"
        )
        val entry = nl.healthjournal.domain.model.migraine.MigraineEntry(
            id = nl.healthjournal.domain.model.common.MeasurementId.generate(), profileId = p.profile.id,
            start = java.time.Instant.parse("2026-10-20T06:30:00Z"), wokeUpWithIt = true,
            end = java.time.Instant.parse("2026-10-20T10:00:00Z"), intensity = 7,
            areas = setOf(nl.healthjournal.domain.model.migraine.HeadArea.TEMPLES),
            symptoms = setOf(nl.healthjournal.domain.model.migraine.MigraineSymptom.NAUSEA),
            customTriggerIds = setOf(trigger.id), tookSomething = true, medicationText = "my usual one",
            intensityAfter2Hours = 3, medicationHelpedMe = true
        )
        return doc.copy(profiles = listOf(p.copy(migraines = listOf(entry), migraineTags = listOf(trigger))))
    }

    @Test
    fun `migraines and tags survive the container round trip`() {
        val codec = HjBackupCodec(fast)
        val doc = withMigraine(codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase))
        assertEquals(doc, codec.decode(codec.encode(doc, passphrase), passphrase))
    }

    @Test
    fun `a file from a newer version with unknown migraine codes and tag kinds still opens`() {
        val codec = HjBackupCodec(fast)
        val doc = withMigraine(codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase))
        val root = JSONObject(String(BackupJson.write(doc), Charsets.UTF_8))
        val profile = root.getJSONArray("profiles").getJSONObject(0)
        profile.getJSONArray("migraines").getJSONObject(0).getJSONArray("symptoms").put("FUTURE_SYMPTOM")
        profile.getJSONArray("migraineTags").put(
            JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("kind", "FUTURE_KIND").put("label", "x")
        )
        val read = BackupJson.read(root.toString().toByteArray()).profiles.single()
        assertEquals(doc.profiles.single().migraines, read.migraines)
        assertEquals(doc.profiles.single().migraineTags, read.migraineTags)
    }

    @Test
    fun `what a newer version wrote and this one leaves out is counted`() {
        val codec = HjBackupCodec(fast)
        val doc = withMigraine(codec.decode(vector("sample-fast-nogzip.hjbackup"), passphrase))
        val root = JSONObject(String(BackupJson.write(doc), Charsets.UTF_8))
        assertEquals(0, BackupJson.read(root.toString().toByteArray()).unreadableItems)

        val profile = root.getJSONArray("profiles").getJSONObject(0)
        val migraine = profile.getJSONArray("migraines").getJSONObject(0)
        migraine.getJSONArray("symptoms").put("FUTURE_SYMPTOM")
        migraine.put("type", "FUTURE_TYPE")
        profile.getJSONArray("migraineTags").put(
            JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("kind", "FUTURE_KIND").put("label", "x")
        )
        assertEquals(3, BackupJson.read(root.toString().toByteArray()).unreadableItems)
    }

    @Test
    fun `an invalid migraine makes the file malformed`() {
        val doc = withMigraine(HjBackupCodec(fast).decode(vector("sample-fast-nogzip.hjbackup"), passphrase))
        val root = JSONObject(String(BackupJson.write(doc), Charsets.UTF_8))
        root.getJSONArray("profiles").getJSONObject(0).getJSONArray("migraines").getJSONObject(0).put("intensity", 11)
        assertThrows(BackupException.Malformed::class.java) { BackupJson.read(root.toString().toByteArray()) }
    }
}
