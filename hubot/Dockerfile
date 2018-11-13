FROM node:8
COPY --from=drone/cli:0.8 /bin/drone /usr/local/bin/
RUN useradd -r -u 6666 hubot -d /hubot && mkdir /hubot && chown hubot. /hubot
USER hubot
WORKDIR /hubot
ENV PATH="/hubot/node_modules/.bin:$PATH"
COPY package.json hubot-scripts.json external-scripts.json /hubot/
RUN yarn
USER root
COPY rchainperfharness /usr/local/bin/
USER hubot
COPY scripts /hubot/scripts
CMD hubot -a discord
