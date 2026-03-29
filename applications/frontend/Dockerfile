# Dockerfile for the Vite Vue + TypeScript frontend application.
#
# Build example:
#   cd applications/frontend
#   npm install
#   npm run build
#   docker build -t frontend:latest .

FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:stable-alpine
WORKDIR /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist ./
