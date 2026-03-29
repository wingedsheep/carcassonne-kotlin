# Start the Ktor backend server on :8080
server:
    ./gradlew :app:run

# Start the Vite dev server on :5173 (proxies API to :8080)
client:
    cd frontend && npm run dev

# Build the frontend to app/src/main/resources/static/
build-client:
    cd frontend && npm run build

# Build everything (backend + frontend)
build: build-client
    ./gradlew build

# Run engine tests
test:
    ./gradlew :utils:test

# Install frontend dependencies
install:
    cd frontend && npm install

# Run both client and server in parallel (dev mode)
dev:
    just server &
    sleep 5
    just client
