# syntax=docker/dockerfile:1

# ---------- Tahap 1: build ----------
FROM node:24-alpine AS build
WORKDIR /app

# Salin manifest dulu → layer npm ci di-cache selama dependency tidak berubah
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Nilai VITE_* di-bake ke bundle saat build (publik — bukan rahasia)
ARG VITE_API_BASE_URL=""
ARG VITE_MIDTRANS_CLIENT_KEY
ARG VITE_MIDTRANS_SNAP_URL=https://app.sandbox.midtrans.com/snap/snap.js
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_MIDTRANS_CLIENT_KEY=$VITE_MIDTRANS_CLIENT_KEY \
    VITE_MIDTRANS_SNAP_URL=$VITE_MIDTRANS_SNAP_URL

RUN test -n "$VITE_MIDTRANS_CLIENT_KEY" || (echo "VITE_MIDTRANS_CLIENT_KEY wajib diisi (--build-arg)" && exit 1)
RUN npm run build

# ---------- Tahap 2: runtime ----------
FROM nginxinc/nginx-unprivileged:1.29-alpine

# Template → diproses envsubst oleh entrypoint image menjadi /etc/nginx/conf.d/default.conf
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

ENV BACKEND_URL=http://backend:8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1