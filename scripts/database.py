"""Explicit MySQL schema operations. Never drops or resets a database."""
import argparse
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("init", "migrate", "seed"))
    parser.add_argument("--confirm-database", required=True)
    args = parser.parse_args()
    database = os.environ.get("DB_NAME", "")
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,63}", database):
        parser.error("DB_NAME must be an explicit SQL identifier")
    if args.confirm_database != database:
        parser.error("--confirm-database must exactly match DB_NAME")
    command = [os.environ.get("MYSQL_BIN", "mysql"), "--no-defaults", "--protocol=TCP",
               "--host=" + os.environ.get("DB_HOST", "127.0.0.1"),
               "--port=" + os.environ.get("DB_PORT", "3306"),
               "--user=" + os.environ.get("DB_USER", "root"), "--default-character-set=utf8mb4", "--batch", "--skip-column-names"]
    env = dict(os.environ, MYSQL_PWD=os.environ.get("DB_PASSWORD", ""))

    def sql(statement, selected=False):
        result = subprocess.run(command + ([database] if selected else []), input=statement,
                                text=True, encoding="utf-8", capture_output=True, env=env)
        if result.returncode:
            raise SystemExit(result.stderr)
        return result.stdout.strip()

    if args.action == "init":
        count = sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='" + database + "';")
        if count != "0":
            raise SystemExit("Refusing init: database contains tables. Use migrate after backup.")
        sql("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
        files = [ROOT / "backEnd/sql/001-core-schema.sql"]
    elif args.action == "seed":
        if not database.startswith(("cfd_demo", "cfd_acceptance_", "cfd_ci")):
            raise SystemExit("Seed restricted to cfd_demo*, cfd_acceptance_* or cfd_ci*.")
        files = [ROOT / "backEnd/sql/002-demo-seed.sql"]
    else:
        files = sorted((ROOT / "backEnd/sky-server/src/main/resources/db").glob("m*-*.sql"))
    for path in files:
        statement = path.read_text(encoding="utf-8-sig")
        # Legacy migrations contain a fixed USE. Select only the confirmed database.
        statement = re.sub(r"(?im)^\s*USE\s+`?sky_take_out`?\s*;", "", statement)
        if re.search(r"(?im)^\s*(USE\s|DROP\s+DATABASE|DROP\s+TABLE)", statement):
            raise SystemExit("Refusing destructive or cross-database SQL in " + str(path))
        sql(statement, selected=True)
        print("Applied " + path.relative_to(ROOT).as_posix() + " to " + database)


if __name__ == "__main__":
    main()
