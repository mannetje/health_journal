package nl.healthjournal.data.backup

import nl.healthjournal.domain.port.secondary.BackupKeyDerivationPort
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** Argon2id 1.3 (RFC 9106) from Bouncy Castle, see ADR 0024. No secret value and no associated data. */
class Argon2KeyDerivation : BackupKeyDerivationPort {
    override fun derive(
        passphrase: ByteArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
        length: Int
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(memoryKiB)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val out = ByteArray(length)
        generator.generateBytes(passphrase, out)
        return out
    }
}
