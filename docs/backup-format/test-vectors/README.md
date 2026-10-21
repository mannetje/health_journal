# Test vectors

Known backup files for checking an implementation of the [backup format](../README.md). Every file decrypts to the same document, [sample.json](sample.json) (compare as parsed JSON, not byte for byte: the decoder's output formatting is not part of the format).

| File | Passphrase | Argon2id (memory, iterations, lanes) | Compression | Salt (hex) | Nonce (hex) |
|------|------------|--------------------------------------|-------------|------------|-------------|
| [sample-default.hjbackup](sample-default.hjbackup) | `correct horse battery staple` | 65536 KiB, 3, 1 (the app defaults) | gzip | `000102030405060708090a0b0c0d0e0f` | `101112131415161718191a1b` |
| [sample-fast-nogzip.hjbackup](sample-fast-nogzip.hjbackup) | `correct horse battery staple` | 1024 KiB, 1, 1 (quick to run in tests) | none | `f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff` | `202122232425262728292a2b` |

The salts and nonces are fixed only so that these files can be reproduced. A real backup always has a random salt and nonce.

## How to use them

1. Decode both files with your implementation and compare the result with `sample.json`.
2. Check that a wrong passphrase (for example `wrong`) fails with an authentication error and returns no data.
3. Check that changing any single byte of a file makes decoding fail.
4. Check that your writer's output decodes with the reference decoder: `python ../hjbackup.py decode yourfile.hjbackup`.

The sample document contains a comment with a quote and a comma, and a comment with non-ASCII characters (`café ☕`), so that quoting and UTF-8 handling are tested.

## Regenerating

```bash
HJ_BACKUP_PASSPHRASE="correct horse battery staple" python ../hjbackup.py encode sample.json sample-default.hjbackup \
  --salt-hex 000102030405060708090a0b0c0d0e0f --nonce-hex 101112131415161718191a1b
HJ_BACKUP_PASSPHRASE="correct horse battery staple" python ../hjbackup.py encode sample.json sample-fast-nogzip.hjbackup \
  --memory-kib 1024 --iterations 1 --no-gzip --salt-hex f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff --nonce-hex 202122232425262728292a2b
```

If the format changes, regenerate these files in the same commit and update the app's tests that read them.
