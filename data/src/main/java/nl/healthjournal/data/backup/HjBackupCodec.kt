package nl.healthjournal.data.backup

import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.port.secondary.BackupCodecPort

/** [BackupCodecPort] for the `.hjbackup` format: JSON document inside the encrypted container. */
class HjBackupCodec(private val container: BackupContainer) : BackupCodecPort {

    override fun encode(document: BackupDocument, passphrase: CharArray): ByteArray {
        val json = BackupJson.write(document)
        try {
            return container.seal(json, passphrase)
        } finally {
            json.fill(0)
        }
    }

    override fun decode(bytes: ByteArray, passphrase: CharArray): BackupDocument {
        val json = container.open(bytes, passphrase)
        try {
            return BackupJson.read(json)
        } finally {
            json.fill(0)
        }
    }
}
