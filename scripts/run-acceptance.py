"""Create a new test database and launch its own backend before HTTP acceptance.

Never resets a database. Requires MySQL client, built executable jar and Java.
The chosen HTTP port must be unoccupied, preventing tests against another server.
"""
import argparse
from datetime import datetime, timezone
import importlib.util
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import time
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, default=ROOT / "backEnd/sky-server/target/sky-server-1.0-SNAPSHOT.jar")
    parser.add_argument("--port", type=int, default=18080)
    parser.add_argument("--keep-running", action="store_true")
    parser.add_argument("--report", type=Path, default=ROOT / ".local/acceptance/report.json")
    args = parser.parse_args()
    if not args.jar.is_file():
        parser.error("Build executable jar with mvn package spring-boot:repackage before acceptance")
    if not os.environ.get("ACCEPTANCE_ADMIN_PASSWORD"):
        parser.error("Set ACCEPTANCE_ADMIN_PASSWORD to the explicitly seeded demo administrator password")
    with socket.socket() as listener:
        try:
            listener.bind(("127.0.0.1", args.port))
        except OSError:
            parser.error("HTTP port is occupied; refusing to write to an existing server")
    database = "cfd_acceptance_" + datetime.now().strftime("%Y%m%d_%H%M%S_") + uuid.uuid4().hex[:6]
    env = dict(os.environ, DB_NAME=database, SERVER_PORT=str(args.port), SERVER_ADDRESS="127.0.0.1",
               AUTH_MOCK_LOGIN="true", WECHAT_MOCK_LOGIN="true", SPRING_PROFILES_ACTIVE="dev")
    args.report.parent.mkdir(parents=True, exist_ok=True)
    for action in ("init", "migrate", "seed"):
        subprocess.run([sys.executable, str(ROOT / "scripts/database.py"), action,
                        "--confirm-database", database], env=env, check=True)
    # Repeat migrations on the populated test database to check preservation/idempotence.
    subprocess.run([sys.executable, str(ROOT / "scripts/database.py"), "migrate",
                    "--confirm-database", database], env=env, check=True)
    java = os.environ.get("JAVA_BIN", "java")
    command = [java, "-Duser.timezone=Asia/Shanghai", "-jar", str(args.jar.resolve()), "--spring.profiles.active=dev"]
    base_url = "http://127.0.0.1:" + str(args.port)
    log = args.report.with_suffix(".server.log").open("w", encoding="utf-8")
    process = None

    def start():
        child = subprocess.Popen(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            if child.poll() is not None:
                raise RuntimeError("Backend exited before readiness; inspect " + str(args.report.with_suffix(".server.log")))
            try:
                with urllib.request.urlopen(base_url + "/api/dishes", timeout=2) as response:
                    if response.status == 200:
                        return child
            except Exception:
                pass
            time.sleep(0.5)
        child.terminate()
        raise RuntimeError("Backend readiness timed out")

    def stop(child):
        child.terminate()
        try:
            child.wait(timeout=15)
        except subprocess.TimeoutExpired:
            child.kill()
            child.wait(timeout=5)

    report = {"createdAt": datetime.now(timezone.utc).isoformat(), "database": database, "baseUrl": base_url,
              "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
              "jarSha256": hashlib.sha256(args.jar.read_bytes()).hexdigest(),
              "workingTreeChanges": subprocess.check_output(["git", "status", "--short"], cwd=ROOT, text=True).strip(),
              "results": []}
    try:
        process = start()
        spec = importlib.util.spec_from_file_location("http_acceptance", ROOT / "tests/acceptance/http_acceptance.py")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        suite = module.Acceptance(base_url)
        report["results"] = suite.run()
        if getattr(suite, "restart_order", None):
            stop(process)
            process = start()
            suite.case("A14", "程序重启后本人已完成订单仍保留", lambda: suite.verify_restart())
        report["processId"] = process.pid if args.keep_running else None
    except Exception as error:
        report["results"].append({"id": "ENVIRONMENT", "status": "FAIL", "detail": str(error)})
    finally:
        if process and not args.keep_running:
            stop(process)
        log.close()
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    passed = report["results"] and all(item["status"] == "PASS" for item in report["results"])
    print("Report:", args.report)
    print("Database retained:", database)
    if args.keep_running and process:
        print("Backend retained:", base_url, "PID", process.pid)
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
