#!/usr/bin/env python3
import os
import sys
import time
import shlex
import subprocess
from pathlib import Path

PGHOST = os.getenv("PGHOST", "mintrade-order")
PGUSER = os.getenv("PGUSER", "postgres")
PGDATABASE = os.getenv("PGDATABASE", "mintrade-order")
PGPASSWORD = os.getenv("PGPASSWORD", "password")
SCRIPTS_DIR = os.getenv("SCRIPTS_DIR", "/scripts")

env = os.environ.copy()
env["PGPASSWORD"] = PGPASSWORD

print(f"Waiting for Postgres at {PGHOST}...")

def can_connect_with_psql():
    try:
        # Try a lightweight query to confirm server is accepting connections
        return subprocess.run(
            ["psql", "-h", PGHOST, "-U", PGUSER, "-d", PGDATABASE, "-c", "SELECT 1;"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            env=env,
        ).returncode == 0
    except FileNotFoundError:
        print("psql not found in container PATH; ensure postgresql client is installed", file=sys.stderr)
        sys.exit(1)

while not can_connect_with_psql():
    time.sleep(1)

print(f"Postgres is up running scripts in {SCRIPTS_DIR}")

scripts_path = Path(SCRIPTS_DIR)

if not scripts_path.exists():
    print(f"No scripts directory found at {SCRIPTS_DIR}")
    sys.exit(0)

files = sorted([p for p in scripts_path.iterdir() if p.is_file()])
if not files:
    print(f"No scripts found in {SCRIPTS_DIR}")
    sys.exit(0)

for filepath in files:
    filename = filepath.name
    suffix = filename.split('.')[-1] if '.' in filename else ''
    if suffix == 'sql':
        print(f"-> psql -f {filename}")
        subprocess.run(["psql", "-v", "ON_ERROR_STOP=1", "-h", PGHOST, "-U", PGUSER, "-d", PGDATABASE, "-f", str(filepath)], check=True, env=env)
    else:
        print(f"-> skipping {filename} (unknown extension)")

print("Runner finished.")
sys.exit(0)

