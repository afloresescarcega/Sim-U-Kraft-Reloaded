.PHONY: run run-server build test clean-natives stop help

help:
	@echo "Targets:"
	@echo "  make run            Launch the dev MC client (auto-heals stale LWJGL natives)"
	@echo "  make run-server     Launch the dev dedicated server"
	@echo "  make build          Build the mod jar"
	@echo "  make test           Run the JUnit suite"
	@echo "  make clean-natives  Stop daemon + wipe run/natives (fixes arm64/x86_64 native mismatch)"
	@echo "  make stop           Stop the Gradle daemon"

run:
	@if [ -f run/natives/lwjgl2/liblwjgl.dylib ]; then \
		HOST_ARCH=$$(uname -m); \
		NATIVE_ARCH=$$(file run/natives/lwjgl2/liblwjgl.dylib | grep -oE 'arm64|x86_64' | head -1); \
		if [ "$$HOST_ARCH" = "arm64" ] && [ "$$NATIVE_ARCH" != "arm64" ]; then \
			echo ">> Stale $$NATIVE_ARCH natives on arm64 host — clearing"; \
			./gradlew --stop; \
			rm -rf run/natives build/tmp/extractNatives2; \
		fi; \
	fi
	./gradlew runClient

run-server:
	./gradlew runServer

build:
	./gradlew build

test:
	./gradlew test

clean-natives:
	./gradlew --stop
	rm -rf run/natives build/tmp/extractNatives2

stop:
	./gradlew --stop
