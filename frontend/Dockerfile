# ---- Stage 1: Build ----
FROM node:20-alpine AS build
WORKDIR /build

# Cache dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Build Quasar SPA
COPY . .
RUN npm run build

# ---- Stage 2: Serve ----
FROM nginx:alpine
COPY --from=build /build/dist/spa /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
