#!/usr/bin/env python3
"""Build and run HTTP integration tests against an isolated real MongoDB database."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def wait_ready(url, process):
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError(f"Service exited early. See {ROOT / '.artifacts'} for logs.")
        try:
            with urllib.request.urlopen(url, timeout=1) as response:
                if response.status == 200:
                    return
        except (OSError, urllib.error.URLError):
            time.sleep(0.25)
    raise RuntimeError(f"Service did not start: {url}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--no-build", action="store_true")
    parser.add_argument("--configuration", default="Debug", choices=["Debug", "Release"])
    parser.add_argument("--test", help="Optional unittest name, e.g. test_00_stations.StationTests")
    args = parser.parse_args()
    if not args.no_build:
        subprocess.run(["dotnet", "build", "SolarMicrogrid.sln", "-c", args.configuration, "-m:1", "/nr:false"], cwd=ROOT, check=True)
    mongod = shutil.which("mongod")
    external_mongo = os.environ.get("TEST_MONGO_URL")
    if not mongod and not external_mongo:
        raise RuntimeError("Install MongoDB locally, or set TEST_MONGO_URL to a disposable test MongoDB server.")
    artifacts = ROOT / ".artifacts"
    artifacts.mkdir(exist_ok=True)
    processes, streams = [], []
    with tempfile.TemporaryDirectory(prefix="solar-component1-") as temporary:
        try:
            mongo_port, api_port, web_port = free_port(), free_port(), free_port()
            if not external_mongo:
                log = open(artifacts / "verify-mongo.log", "w")
                streams.append(log)
                mongo = subprocess.Popen([mongod, "--dbpath", temporary, "--bind_ip", "127.0.0.1", "--port", str(mongo_port)], stdout=log, stderr=subprocess.STDOUT)
                processes.append(mongo)
            env = os.environ.copy()
            env.update({
                "ASPNETCORE_ENVIRONMENT": "Development",
                "Mongo__ConnectionString": external_mongo or f"mongodb://127.0.0.1:{mongo_port}",
                "Mongo__DatabaseName": "solar_test_" + secrets.token_hex(6),
                "Jwt__SigningKey": secrets.token_urlsafe(48),
                "Bootstrap__Username": "test_backoffice",
                "Bootstrap__Password": secrets.token_urlsafe(24),
                "Bootstrap__Email": "backoffice@example.test",
                "RateLimiting__AuthPermitLimit": "200",
            })
            for app, port in [("Api", api_port), ("Web", web_port)]:
                app_dir = ROOT / "src" / f"SolarMicrogrid.{app}"
                dll = app_dir / "bin" / args.configuration / "net10.0" / f"SolarMicrogrid.{app}.dll"
                log = open(artifacts / f"verify-{app.lower()}.log", "w")
                streams.append(log)
                service_env = {**env, "ASPNETCORE_URLS": f"http://127.0.0.1:{port}", "Api__BaseUrl": f"http://127.0.0.1:{api_port}/api/v1/"}
                process = subprocess.Popen(["dotnet", str(dll)], cwd=app_dir, env=service_env, stdout=log, stderr=subprocess.STDOUT)
                processes.append(process)
                wait_ready(f"http://127.0.0.1:{port}/" + ("health" if app == "Api" else "Account/Login"), process)
            env.update({"TEST_API_URL": f"http://127.0.0.1:{api_port}/api/v1/", "TEST_WEB_URL": f"http://127.0.0.1:{web_port}"})
            with urllib.request.urlopen(f"http://127.0.0.1:{api_port}/openapi/v1.json") as response:
                schema = json.load(response)
            (ROOT / "docs" / "openapi.json").write_text(json.dumps(schema, indent=2) + "\n")
            command = [sys.executable, "-m", "unittest"]
            command += [args.test, "-v"] if args.test else ["discover", "-s", "tests", "-v"]
            result = subprocess.run(command, cwd=ROOT / "tests" if args.test else ROOT, env=env)
            return result.returncode
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    if os.name == "nt":
                        process.terminate()
                    else:
                        process.send_signal(signal.SIGINT)
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
            for stream in streams:
                stream.close()


if __name__ == "__main__":
    sys.exit(main())
