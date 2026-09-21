#!/usr/bin/env python3
"""Create ignored, local development credentials without embedding secrets in source."""
import json
from pathlib import Path
import secrets

ROOT = Path(__file__).resolve().parents[1]
config_path = ROOT / "src/SolarMicrogrid.Api/appsettings.Local.json"
credentials_path = ROOT / ".local/development-credentials.txt"
if config_path.exists():
    print("Local API settings already exist; no credentials were changed.")
else:
    password = secrets.token_urlsafe(24)
    config = {
        "Jwt": {"SigningKey": secrets.token_urlsafe(48)},
        "Bootstrap": {"Username": "backoffice", "Password": password, "FullName": "Grid Administrator", "Email": "backoffice@example.test"},
    }
    credentials_path.parent.mkdir(exist_ok=True)
    config_path.touch(mode=0o600)
    config_path.write_text(json.dumps(config, indent=2) + "\n")
    credentials_path.touch(mode=0o600)
    credentials_path.write_text(f"Development only\nWeb: http://localhost:5081\nUsername: backoffice\nPassword: {password}\n")
    print(f"Local settings created. Your generated login is in {credentials_path.relative_to(ROOT)}")
    print("These files are ignored by Git. Existing MongoDB accounts are never reset on startup.")
