# Stage 1: Build
FROM node:20 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve
FROM nginxinc/nginx-unprivileged:stable-alpine
USER root
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Set apiUrl to relative path, as Nginx will proxy it
RUN echo '{"apiUrl": "/api"}' > /usr/share/nginx/html/config.json && \
    chown 101:101 /usr/share/nginx/html/config.json

USER 101
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
