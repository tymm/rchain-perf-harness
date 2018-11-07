NAME = rchain/rchain-hubot
REV = $(shell git rev-parse --short HEAD)
VER = 0.3

.PHONY: all
all: build

.PHONY: build
build:
	docker build -t $(NAME):$(REV) .
	docker tag $(NAME):$(REV) $(NAME):$(VER)
	docker tag $(NAME):$(REV) $(NAME):latest

.PHONY: push
push:
	docker push $(NAME):$(VER)
	docker push $(NAME):latest
