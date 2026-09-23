FROM node:25-alpine AS dependencies
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

FROM node:25-alpine AS build
WORKDIR /app
RUN corepack enable
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
ARG NEXT_PUBLIC_DEMO_MODE=false
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_DEMO_MODE=$NEXT_PUBLIC_DEMO_MODE
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .
RUN pnpm build

FROM node:25-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S -g 10001 pantry && adduser -S -u 10001 -G pantry pantry
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
USER 10001:10001
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
