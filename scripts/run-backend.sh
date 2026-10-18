#!/usr/bin/env bash
#
# Assumes config (e.g., backend/.env) is already setup, starts docker compose.

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

VERCEL_TOKEN="$(grep -E '^VERCEL_TOKEN=' backend/.env | head -1 | cut -d= -f2- | tr -d ' \"' || true)"
[[ -n "$VERCEL_TOKEN" ]] || die "Missing VERCEL_TOKEN in backend/.env."

# ---------------------------------------------------------------------------
# Backend
# ---------------------------------------------------------------------------

info "Deploying backend to Vercel production..."
(cd backend && vercel deploy --prod --token "$VERCEL_TOKEN" --name exploreubc --yes)

echo
info "Backend is up."
