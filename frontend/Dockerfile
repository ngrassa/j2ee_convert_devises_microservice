FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
COPY index.html styles.css script.js ./
EXPOSE 80
