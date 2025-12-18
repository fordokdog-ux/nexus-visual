import base64
import json
import os
import secrets
import sqlite3
import time
from dataclasses import dataclass

from fastapi import FastAPI
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

APP = FastAPI()

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
KEYS_DIR = os.path.join(BASE_DIR, "keys")
DB_PATH = os.path.join(BASE_DIR, "licensing.db")

PRIVATE_KEY_PATH = os.path.join(KEYS_DIR, "private_key.pem")
PUBLIC_KEY_PATH = os.path.join(KEYS_DIR, "public_key.pem")


def db() -> sqlite3.Connection:
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    return con


def init_db() -> None:
    con = db()
    try:
        con.execute(
            """
            CREATE TABLE IF NOT EXISTS codes (
                code TEXT PRIMARY KEY,
                used INTEGER NOT NULL DEFAULT 0,
                bound_uuid TEXT,
                bound_hwid TEXT,
                created_at INTEGER NOT NULL,
                used_at INTEGER
            )
            """
        )
        con.commit()
    finally:
        con.close()


def ensure_keys() -> None:
    os.makedirs(KEYS_DIR, exist_ok=True)
    if os.path.exists(PRIVATE_KEY_PATH) and os.path.exists(PUBLIC_KEY_PATH):
        return

    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    public_key = private_key.public_key()

    with open(PRIVATE_KEY_PATH, "wb") as f:
        f.write(
            private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption(),
            )
        )

    with open(PUBLIC_KEY_PATH, "wb") as f:
        f.write(
            public_key.public_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PublicFormat.SubjectPublicKeyInfo,
            )
        )


def load_private_key():
    with open(PRIVATE_KEY_PATH, "rb") as f:
        return serialization.load_pem_private_key(f.read(), password=None)


def public_key_x509_base64() -> str:
    with open(PUBLIC_KEY_PATH, "rb") as f:
        pub = serialization.load_pem_public_key(f.read())
    der = pub.public_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    return base64.b64encode(der).decode("ascii")


def sign_payload(payload_json: str) -> str:
    key = load_private_key()
    sig = key.sign(
        payload_json.encode("utf-8"),
        padding.PKCS1v15(),
        hashes.SHA256(),
    )
    return base64.b64encode(sig).decode("ascii")


def create_code() -> str:
    # readable code: NV-XXXX-XXXX-XXXX
    raw = secrets.token_hex(6).upper()
    return f"NV-{raw[0:4]}-{raw[4:8]}-{raw[8:12]}"


class RedeemRequest(BaseModel):
    code: str
    uuid: str
    hwid: str


@APP.on_event("startup")
def _startup():
    ensure_keys()
    init_db()


@APP.get("/health")
def health():
    return {"ok": True}


@APP.get("/public_key")
def get_public_key():
    return {"public_key_x509_base64": public_key_x509_base64()}


@APP.post("/redeem")
def redeem(req: RedeemRequest):
    code = (req.code or "").strip()
    uuid = (req.uuid or "").strip()
    hwid = (req.hwid or "").strip()

    if not code or not uuid or not hwid:
        return JSONResponse(status_code=400, content={"error": "missing_fields"})

    con = db()
    try:
        row = con.execute("SELECT * FROM codes WHERE code = ?", (code,)).fetchone()
        if row is None:
            return JSONResponse(status_code=404, content={"error": "code_not_found"})

        used = int(row["used"]) == 1
        bound_uuid = row["bound_uuid"]
        bound_hwid = row["bound_hwid"]

        if not used:
            con.execute(
                "UPDATE codes SET used = 1, bound_uuid = ?, bound_hwid = ?, used_at = ? WHERE code = ?",
                (uuid, hwid, int(time.time()), code),
            )
            con.commit()
        else:
            if (bound_uuid or "") != uuid or (bound_hwid or "") != hwid:
                return JSONResponse(status_code=403, content={"error": "code_already_used"})

        payload = {"uuid": uuid, "hwid": hwid}
        payload_json = json.dumps(payload, separators=(",", ":"), ensure_ascii=False)
        signature_b64 = sign_payload(payload_json)

        return {
            "license": {
                "payload": payload_json,
                "signature": signature_b64,
            }
        }
    finally:
        con.close()


# Admin helpers (optional)

def admin_create_one_code() -> str:
    init_db()
    code = create_code()
    con = db()
    try:
        con.execute(
            "INSERT INTO codes(code, used, bound_uuid, bound_hwid, created_at, used_at) VALUES(?, 0, NULL, NULL, ?, NULL)",
            (code, int(time.time())),
        )
        con.commit()
    finally:
        con.close()
    return code


if __name__ == "__main__":
    # Quick-start: create one code and print public key to paste into the mod.
    ensure_keys()
    init_db()
    print("Public key (X509 Base64) to paste into mod:")
    print(public_key_x509_base64())
    print()
    print("Test activation code:")
    print(admin_create_one_code())
    print()
    print("Run server:")
    print("  python -m uvicorn lic_server:APP --host 127.0.0.1 --port 8787")
