"""Verify the public database CLI on a new legacy-schema fixture database.

DDL and fixture records are observed here only to verify schema migration and
data preservation. Business acceptance remains entirely at the HTTP boundary.
"""
from datetime import datetime
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]


def main():
    database = "cfd_acceptance_migration_" + datetime.now().strftime("%Y%m%d_%H%M%S_") + uuid.uuid4().hex[:5]
    command = [os.environ.get("MYSQL_BIN", "mysql"), "--no-defaults", "--protocol=TCP",
               "--host=" + os.environ.get("DB_HOST", "127.0.0.1"),
               "--port=" + os.environ.get("DB_PORT", "3306"),
               "--user=" + os.environ.get("DB_USER", "root"),
               "--batch", "--skip-column-names", "--default-character-set=utf8mb4"]
    env = dict(os.environ, MYSQL_PWD=os.environ.get("DB_PASSWORD", ""), DB_NAME=database)

    def sql(statement, selected=True):
        result = subprocess.run(command + ([database] if selected else []), input=statement,
                                text=True, encoding="utf-8", capture_output=True, env=env, check=True)
        return result.stdout.strip()

    sql("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4;", selected=False)
    legacy = (ROOT / "backEnd/sky_take_out.sql").read_text(encoding="utf-8-sig")
    # Only CREATE TABLE statements, never the legacy DROP/USE or personal sample data.
    tables = re.findall(r"CREATE TABLE `\w+`.*?ROW_FORMAT = Dynamic;", legacy, re.S)
    assert len(tables) == 14, "Legacy schema changed; review the migration fixture"
    sql("\n".join(tables))
    seed = (ROOT / "backEnd/sql/002-demo-seed.sql").read_text(encoding="utf-8-sig")
    seed = re.sub(r"INSERT IGNORE INTO order_stock.*?;", "", seed, flags=re.S)
    sql(seed)
    probe = "SELECT id,number,status,pay_status,amount FROM orders ORDER BY id;"
    before = sql(probe)
    for _ in range(2):
        subprocess.run([sys.executable, str(ROOT / "scripts/database.py"), "migrate",
                        "--confirm-database", database], env=env, cwd=ROOT, check=True)
    after = sql(probe)
    assert before == after, (before, after)
    refused = subprocess.run([sys.executable, str(ROOT / "scripts/database.py"), "init",
                             "--confirm-database", database], env=env, cwd=ROOT, text=True, capture_output=True)
    assert refused.returncode != 0 and "contains tables" in refused.stderr, refused.stderr
    indices = sql("SELECT DISTINCT index_name,non_unique FROM information_schema.statistics "
                  "WHERE table_schema=DATABASE() AND table_name='orders' AND index_name LIKE 'uq_%' ORDER BY index_name;")
    assert indices.splitlines() == ["uq_orders_number\t0", "uq_orders_pickup_code\t0", "uq_orders_user_request\t0"], indices
    report = {"database": database, "mysql": sql("SELECT VERSION();"), "legacyTableCount": 14,
              "migrationRepeats": 2, "before": before, "after": after, "orderUniqueIndexes": indices,
              "nonemptyInit": "REFUSED", "status": "PASS"}
    output = ROOT / ".local/acceptance/migration.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS legacy migration and preserved fixture orders:", database)
    print("Report:", output)


if __name__ == "__main__":
    main()
