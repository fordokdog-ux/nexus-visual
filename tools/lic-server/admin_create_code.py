import sqlite3
import time
import os

from lic_server import BASE_DIR, DB_PATH, init_db, create_code


def main():
    init_db()
    code = create_code()

    con = sqlite3.connect(DB_PATH)
    try:
        con.execute(
            "INSERT INTO codes(code, used, bound_uuid, bound_hwid, created_at, used_at) VALUES(?, 0, NULL, NULL, ?, NULL)",
            (code, int(time.time())),
        )
        con.commit()
    finally:
        con.close()

    print(code)


if __name__ == "__main__":
    main()
