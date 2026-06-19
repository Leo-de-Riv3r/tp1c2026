FROM node:22-alpine
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig.json ./
COPY src ./src
COPY scripts ./scripts
COPY seed ./seed
COPY config.json ./
COPY docker-entrypoint.sh ./
RUN sed -i 's/\r$//' docker-entrypoint.sh && chmod +x docker-entrypoint.sh

ENV API_BASE_URL=http://backend:8080
ENV LOAD_TEST_USER_COUNT=50

RUN npm run seed:users

ENTRYPOINT ["sh", "docker-entrypoint.sh"]
