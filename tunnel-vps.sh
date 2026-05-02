#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  tunnel-vps.sh — Abre SSH tunnel para o PostgreSQL da VPS
#
#  Uso:
#    ./tunnel-vps.sh          → abre tunnel em background
#    ./tunnel-vps.sh stop     → fecha o tunnel
#    ./tunnel-vps.sh status   → verifica se está ativo
# ─────────────────────────────────────────────────────────────

VPS_HOST="76.13.163.235"
VPS_USER="root"
VPS_SSH_PORT="22"
LOCAL_PORT="5433"
REMOTE_PORT="5433"
PID_FILE="/tmp/sindico-tunnel.pid"

start() {
  if [ -f "$PID_FILE" ] && kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
    echo "✅ Tunnel já está ativo (PID $(cat $PID_FILE))"
    return
  fi

  echo "🔌 Abrindo SSH tunnel: localhost:${LOCAL_PORT} → ${VPS_HOST}:${REMOTE_PORT} ..."
  ssh -f -N \
    -L "${LOCAL_PORT}:127.0.0.1:${REMOTE_PORT}" \
    -p "${VPS_SSH_PORT}" \
    "${VPS_USER}@${VPS_HOST}" \
    -o StrictHostKeyChecking=no \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3

  # Captura o PID do processo ssh em background
  sleep 1
  SSH_PID=$(pgrep -f "ssh.*-L ${LOCAL_PORT}:127.0.0.1:${REMOTE_PORT}" | head -1)

  if [ -n "$SSH_PID" ]; then
    echo "$SSH_PID" > "$PID_FILE"
    echo "✅ Tunnel ativo! PID: ${SSH_PID}"
    echo "   Local  → localhost:${LOCAL_PORT}"
    echo "   Remoto → ${VPS_HOST}:${REMOTE_PORT}"
  else
    echo "❌ Falha ao abrir o tunnel. Verifique o acesso SSH."
    exit 1
  fi
}

stop() {
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
      kill "$PID"
      echo "🔴 Tunnel encerrado (PID $PID)"
    else
      echo "⚠️  Processo $PID não estava ativo"
    fi
    rm -f "$PID_FILE"
  else
    echo "⚠️  Nenhum tunnel registrado"
  fi
}

status() {
  if [ -f "$PID_FILE" ] && kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
    echo "✅ Tunnel ATIVO — PID $(cat $PID_FILE) | localhost:${LOCAL_PORT} → ${VPS_HOST}:${REMOTE_PORT}"
  else
    echo "🔴 Tunnel INATIVO"
  fi
}

case "${1:-start}" in
  start)  start ;;
  stop)   stop ;;
  status) status ;;
  *)      echo "Uso: $0 {start|stop|status}" ; exit 1 ;;
esac
