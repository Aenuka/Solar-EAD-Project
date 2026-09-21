#!/usr/bin/env python3
"""Run Component 1 locally; use Ctrl+C to stop the processes started by this script."""
import os
from pathlib import Path
import shutil
import signal
import socket
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]


def listening(port):
    with socket.socket() as sock:
        return sock.connect_ex(("127.0.0.1", port)) == 0


def main():
    subprocess.run([sys.executable, str(ROOT / "scripts/init_dev.py")], check=True)
    subprocess.run(["dotnet", "build", "SolarMicrogrid.sln", "-m:1", "/nr:false"], cwd=ROOT, check=True)
    local = ROOT / ".local"
    processes, streams = [], []
    try:
        if not listening(27017) and not os.environ.get("Mongo__ConnectionString"):
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
