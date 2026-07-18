# syntax=docker/dockerfile:1

# ---- Build stage: produce the Angular static bundle ----
FROM node:22.22.3-alpine AS build
WORKDIR /build

# Pin npm to the lockfile's version via Corepack (matches package.json
# packageManager) so the image build matches CI.
COPY package.json package-lock.json ./
RUN corepack enable && corepack prepare npm@10.9.8 --activate && npm ci

COPY . .
RUN npm run build

# ---- Runtime stage: nginx serving the SPA ----
FROM nginx:alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/corporate-ui/browser /usr/share/nginx/html
EXPOSE 80
