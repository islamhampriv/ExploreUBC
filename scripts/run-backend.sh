#!/usr/bin/env bash
#
# Deploys backend to Vercel using application settings from backend/.env.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

info()  { printf '\033[34m==>\033[0m %s\n' "$*"; }
die()   { printf '\033[31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
command -v vercel >/dev/null 2>&1  || die "Vercel CLI not found. Install it with: npm install -g vercel"

[[ -f backend/.env ]] || die "Missing backend/.env — follow the student setup guide first."

# ---------------------------------------------------------------------------
# Backend
# ---------------------------------------------------------------------------

info "Deploying backend to Vercel production..."
node <<'NODE'
const { spawnSync } = require("node:child_process");
const { readFileSync } = require("node:fs");
const { parseEnv } = require("node:util");

const environment = parseEnv(readFileSync("backend/.env", "utf8"));

const token = environment.VERCEL_TOKEN?.trim();
if (!token) {
	console.error("Missing VERCEL_TOKEN in backend/.env.");
	process.exit(1);
}

const args = ["deploy", "--prod", "--token", token, "--name", "exploreubc", "--yes"];
for (const [name, value] of Object.entries(environment)) {
	if (name === "VERCEL_TOKEN") continue;
	args.push("--env", `${name}=${value}`, "--build-env", `${name}=${value}`);
}

const result = spawnSync("vercel", args, { cwd: "backend", stdio: "inherit" });
if (result.error) {
	console.error(`Could not start Vercel CLI: ${result.error.message}`);
	process.exit(1);
}
process.exit(result.status ?? 1);
NODE

echo
info "Backend is up."
