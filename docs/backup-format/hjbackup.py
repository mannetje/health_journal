#!/usr/bin/env python3
"""Reference encoder and decoder for Health Journal backup files (.hjbackup), format version 1.

Standalone on purpose: it needs only Python 3.9+ and the `cryptography` package (version 44 or newer,
which provides Argon2id). It is the executable form of docs/backup-format/README.md.

  python hjbackup.py decode backup.hjbackup            -> prints the JSON document
  python hjbackup.py decode backup.hjbackup --csv out/ -> also writes one CSV per metric and profile
  python hjbackup.py encode data.json out.hjbackup     -> encrypts a JSON document (used for test vectors)

The passphrase is asked on the terminal, or read from the HJ_BACKUP_PASSPHRASE environment variable.
"""
import argparse
import csv
import getpass
import gzip
import json
import os
import struct
import sys
import unicodedata

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.argon2 import Argon2id

MAGIC = b"HJBACKUP"
FORMAT_VERSION = 1
KDF_ARGON2ID = 1
CIPHER_AES_256_GCM = 1
COMPRESSION_NONE = 0
COMPRESSION_GZIP = 1
FIXED_HEADER = struct.Struct(">8sHBBBBBBII4s")  # 28 bytes, big-endian
TAG_LENGTH = 16
MAX_MEMORY_KIB = 1024 * 1024  # refuse more than 1 GiB because of a hostile header
MAX_ITERATIONS = 100
MAX_PARALLELISM = 16


class BackupError(Exception):
    pass


def derive_key(passphrase, salt, memory_kib, iterations, parallelism):
    secret = unicodedata.normalize("NFKC", passphrase).encode("utf-8")
    kdf = Argon2id(salt=salt, length=32, iterations=iterations, lanes=parallelism, memory_cost=memory_kib)
    return kdf.derive(secret)


def decode(data, passphrase):
    """Returns the parsed JSON document of a backup file."""
    if len(data) < FIXED_HEADER.size:
        raise BackupError("File too short to be a Health Journal backup.")
    (magic, version, kdf_id, cipher_id, compression_id, parallelism, salt_len, nonce_len,
     memory_kib, iterations, reserved) = FIXED_HEADER.unpack_from(data)
    if magic != MAGIC:
        raise BackupError("Not a Health Journal backup (wrong magic bytes).")
    if version != FORMAT_VERSION:
        raise BackupError(f"Unsupported container version {version}.")
    if kdf_id != KDF_ARGON2ID or cipher_id != CIPHER_AES_256_GCM:
        raise BackupError("Unsupported key derivation or cipher.")
    if compression_id not in (COMPRESSION_NONE, COMPRESSION_GZIP):
        raise BackupError("Unsupported compression.")
    if reserved != b"\0\0\0\0":
        raise BackupError("Reserved header bytes are not zero.")
    if salt_len != 16 or nonce_len != 12:
        raise BackupError("Unexpected salt or nonce length.")
    if not (1 <= parallelism <= MAX_PARALLELISM and 8 * parallelism <= memory_kib <= MAX_MEMORY_KIB
            and 1 <= iterations <= MAX_ITERATIONS):
        raise BackupError("Key derivation parameters are out of range.")
    header_end = FIXED_HEADER.size + salt_len + nonce_len
    if len(data) < header_end + TAG_LENGTH:
        raise BackupError("File is truncated.")
    salt = data[FIXED_HEADER.size:FIXED_HEADER.size + salt_len]
    nonce = data[FIXED_HEADER.size + salt_len:header_end]
    key = derive_key(passphrase, salt, memory_kib, iterations, parallelism)
    try:
        plain = AESGCM(key).decrypt(nonce, data[header_end:], data[:header_end])
    except InvalidTag:
        raise BackupError("Wrong passphrase, or the file is damaged.") from None
    if compression_id == COMPRESSION_GZIP:
        plain = gzip.decompress(plain)
    document = json.loads(plain.decode("utf-8"))
    if document.get("format") != "healthjournal-backup":
        raise BackupError("Decrypted data is not a Health Journal document.")
    if document.get("schemaVersion", 0) > 1:
        print("Warning: this file has a newer schemaVersion; unknown fields are ignored.", file=sys.stderr)
    return document


def encode(document, passphrase, memory_kib=65536, iterations=3, parallelism=1,
           compression_id=COMPRESSION_GZIP, salt=None, nonce=None):
    salt = salt if salt is not None else os.urandom(16)
    nonce = nonce if nonce is not None else os.urandom(12)
    plain = json.dumps(document, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    if compression_id == COMPRESSION_GZIP:
        plain = gzip.compress(plain, mtime=0)
    header = FIXED_HEADER.pack(MAGIC, FORMAT_VERSION, KDF_ARGON2ID, CIPHER_AES_256_GCM, compression_id,
                               parallelism, len(salt), len(nonce), memory_kib, iterations, b"\0\0\0\0")
    header += salt + nonce
    key = derive_key(passphrase, salt, memory_kib, iterations, parallelism)
    return header + AESGCM(key).encrypt(nonce, plain, header)


def _duration_seconds(entry):
    """The backup stores start and end; the app's CSV also has the duration in whole seconds."""
    from datetime import datetime
    start = datetime.fromisoformat(entry["startTime"].replace("Z", "+00:00"))
    end = datetime.fromisoformat(entry["endTime"].replace("Z", "+00:00"))
    return int((end - start).total_seconds())


CSV_TABLES = {
    "weights": ("weight", ["timestamp", "weight_kg", "bmi", "comment"],
                lambda e: [e["timestamp"], e["weightKg"], e.get("bmi"), e.get("comment")]),
    "bloodPressures": ("blood_pressure",
                       ["timestamp", "systolic_mmhg", "diastolic_mmhg", "pulse_bpm", "classification", "comment"],
                       lambda e: [e["timestamp"], e["systolic"], e["diastolic"], e.get("pulse"), e.get("category"),
                                  e.get("comment")]),
    "glucoses": ("glucose", ["timestamp", "glucose_mmol_l", "context", "classification", "comment"],
                 lambda e: [e["timestamp"], e["glucoseMmolL"], e["context"], e.get("category"), e.get("comment")]),
    "waistCircumferences": ("waist", ["timestamp", "waist_cm", "classification", "comment"],
                            lambda e: [e["timestamp"], e["waistCm"], e.get("category"), e.get("comment")]),
    "activities": ("activity", ["start_timestamp", "end_timestamp", "distance_m", "duration_s", "comment"],
                   lambda e: [e["startTime"], e["endTime"], e["distanceMeters"], _duration_seconds(e),
                              e.get("comment")]),
}


MIGRAINE_HEADER = ["start_timestamp", "woke_up_with_it", "end_timestamp", "intensity", "areas", "pain_character",
                   "type", "symptoms", "triggers", "took_something", "medication_text", "second_dose",
                   "intensity_after_2h", "relief_methods", "custom_triggers", "custom_relief_methods",
                   "medication_helped_me", "relief_helped_me", "activities", "comment"]


def _migraine_row(e, labels):
    """Same columns as the app's migraine CSV: codes joined with | and tag ids replaced by their labels."""
    def codes(key):
        return "|".join(sorted(e.get(key) or []))

    def tags(key):
        return "|".join(sorted((labels[i] for i in e.get(key) or [] if i in labels), key=str.lower))

    def flag(key):
        return "true" if e.get(key) else "false"

    return [e["start"], flag("wokeUpWithIt"), e.get("end"), e.get("intensity"), codes("areas"),
            codes("painCharacters"), e.get("type"), codes("symptoms"), codes("triggers"), flag("tookSomething"),
            e.get("medicationText"), flag("secondDose"), e.get("intensityAfter2Hours"), codes("reliefMethods"),
            tags("customTriggers"), tags("customReliefMethods"), flag("medicationHelpedMe"), flag("reliefHelpedMe"),
            e.get("activities"), e.get("comment")]


def write_csv(document, directory):
    """Writes one CSV per metric and profile, oldest first. Medication is only in the JSON."""
    os.makedirs(directory, exist_ok=True)
    written = []
    for index, profile in enumerate(document["profiles"], start=1):
        for key, (name, header, row) in CSV_TABLES.items():
            entries = sorted(profile.get(key, []), key=lambda e: e.get("timestamp") or e.get("startTime"))
            path = os.path.join(directory, f"profile{index}_{name}.csv")
            with open(path, "w", encoding="utf-8", newline="") as handle:
                writer = csv.writer(handle, lineterminator="\n")
                writer.writerow(header)
                for entry in entries:
                    writer.writerow(["" if cell is None else cell for cell in row(entry)])
            written.append(path)
        labels = {t["id"]: t["label"] for t in profile.get("migraineTags", [])}
        path = os.path.join(directory, f"profile{index}_migraine.csv")
        with open(path, "w", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle, lineterminator="\n")
            writer.writerow(MIGRAINE_HEADER)
            for entry in sorted(profile.get("migraines", []), key=lambda e: e["start"]):
                writer.writerow(["" if cell is None else cell for cell in _migraine_row(entry, labels)])
        written.append(path)
    return written


def ask_passphrase(confirm=False):
    value = os.environ.get("HJ_BACKUP_PASSPHRASE")
    if value is None:
        value = getpass.getpass("Backup passphrase: ")
        if confirm and getpass.getpass("Repeat passphrase: ") != value:
            raise BackupError("The passphrases differ.")
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    dec = sub.add_parser("decode")
    dec.add_argument("file")
    dec.add_argument("--out", help="write the JSON to this file instead of standard output")
    dec.add_argument("--csv", metavar="DIR", help="also write CSV files (same columns as the app export)")
    enc = sub.add_parser("encode")
    enc.add_argument("json_file")
    enc.add_argument("out")
    enc.add_argument("--memory-kib", type=int, default=65536)
    enc.add_argument("--iterations", type=int, default=3)
    enc.add_argument("--parallelism", type=int, default=1)
    enc.add_argument("--no-gzip", action="store_true")
    enc.add_argument("--salt-hex", help="fixed salt, for test vectors only")
    enc.add_argument("--nonce-hex", help="fixed nonce, for test vectors only")
    args = parser.parse_args()
    try:
        if args.command == "decode":
            with open(args.file, "rb") as handle:
                document = decode(handle.read(), ask_passphrase())
            text = json.dumps(document, ensure_ascii=False, indent=2)
            if args.out:
                with open(args.out, "w", encoding="utf-8") as handle:
                    handle.write(text + "\n")
            else:
                sys.stdout.reconfigure(encoding="utf-8")
                print(text)
            if args.csv:
                for path in write_csv(document, args.csv):
                    print("wrote", path, file=sys.stderr)
        else:
            with open(args.json_file, encoding="utf-8") as handle:
                document = json.load(handle)
            blob = encode(document, ask_passphrase(confirm=not os.environ.get("HJ_BACKUP_PASSPHRASE")),
                          args.memory_kib, args.iterations, args.parallelism,
                          COMPRESSION_NONE if args.no_gzip else COMPRESSION_GZIP,
                          bytes.fromhex(args.salt_hex) if args.salt_hex else None,
                          bytes.fromhex(args.nonce_hex) if args.nonce_hex else None)
            with open(args.out, "wb") as handle:
                handle.write(blob)
    except BackupError as error:
        sys.exit(f"Error: {error}")


if __name__ == "__main__":
    main()
