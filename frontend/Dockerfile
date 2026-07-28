# web 이미지(Phase 18): 프론트를 빌드해 Caddy로 정적 서빙 + /api·/actuator를 앱으로 프록시.
# 빌드 컨텍스트 = frontend/ (compose: build.context=frontend, dockerfile=Dockerfile).
# → 루트 .dockerignore(frontend 제외)와 무관하게 frontend/.dockerignore가 적용된다.
FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . ./
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /app/dist /srv
COPY Caddyfile /etc/caddy/Caddyfile
