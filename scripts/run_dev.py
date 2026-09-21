#!/usr/bin/env python3
"""Run Component 1 locally; use Ctrl+C to stop the processes started by this script."""
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import subprocess
import sys
import time
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]


def listening(port):
    with socket.socket() as sock:
        return sock.connect_ex(("127.0.0.1", port)) == 0


def mongo_connection_string():
    """Match the API's Development JSON settings and environment override."""
    connection = "mongodb://127.0.0.1:27017"
    directory = ROOT / "src/SolarMicrogrid.Api"
    for name in ("appsettings.json", "appsettings.Development.json", "appsettings.Local.json"):
        path = directory / name
        if path.exists():
            settings = json.loads(path.read_text())
            connection = settings.get("Mongo", {}).get("ConnectionString", connection)
    return os.environ.get("Mongo__ConnectionString", connection)


def main():
    subprocess.run([sys.executable, str(ROOT / "scripts/init_dev.py")], check=True)
    for port in (5080, 5081):
        if listening(port):
            raise RuntimeError(f"Port {port} is already in use. Stop the existing API/web app in its terminal or IDE before starting this script.")
    subprocess.run(["dotnet", "build", "SolarMicrogrid.sln", "-m:1", "/nr:false"], cwd=ROOT, check=True)
    local = ROOT / ".local"
    local.mkdir(exist_ok=True)
    processes, streams = [], []
    try:
        mongo_url = urlsplit(mongo_connection_string())
        default_local_mongo = (mongo_url.scheme == "mongodb" and
                               mongo_url.hostname in ("localhost", "127.0.0.1") and
                               mongo_url.port in (None, 27017) and not mongo_url.username)
        if mongo_url.scheme == "mongodb+srv":
            print("Using MongoDB Atlas from API configuration; local MongoDB is not required.", flush=True)
        if default_local_mongo and not listening(27017):
            mongod = shutil.which("mongod")
            if not mongod:
                raise RuntimeError("Start MongoDB with 'docker compose up -d mongo' or install mongod locally.")
            database = local / "mongodb"
            database.mkdir(exist_ok=True)
            log = open(local / "mongo.log", "a")
            streams.append(log)
            processes.append(subprocess.Popen([mongod, "--dbpath", str(database), "--bind_ip", "127.0.0.1", "--port", "27017"], stdout=log, stderr=subprocess.STDOUT))
        for app, port in [("Api", 5080), ("Web", 5081)]:
            if listening(port):
                raise RuntimeError(f"Port {port} is already in use. Stop that service before starting this script.")
            directory = ROOT / "src" / f"SolarMicrogrid.{app}"
            log = open(local / f"{app.lower()}.log", "a")
            streams.append(log)
            env = {**os.environ, "ASPNETCORE_ENVIRONMENT": "Development", "ASPNETCORE_URLS": f"http://localhost:{port}"}
            processes.append(subprocess.Popen(["dotnet", str(directory / "bin/Debug/net10.0" / f"SolarMicrogrid.{app}.dll")], cwd=directory, env=env, stdout=log, stderr=subprocess.STDOUT))
        print("Starting API at http://localhost:5080 and staff portal at http://localhost:5081", flush=True)
        print("Login: .local/development-credentials.txt | Logs: .local/ | Ctrl+C to stop", flush=True)
        while all(process.poll() is None for process in processes):
            time.sleep(1)
        raise RuntimeError("A service stopped. Check .local/ logs.")
    except KeyboardInterrupt:
        print("Stopping local services.")
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.send_signal(signal.SIGINT)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
        for stream in streams:
            stream.close()


if __name__ == "__main__":
    main()
