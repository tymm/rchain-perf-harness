FROM node:8
COPY --from=drone/cli:0.8 /bin/drone /usr/local/bin/
COPY rchainperfharness /usr/local/bin/
RUN useradd -r -u 6666 hubot -d /hubot && mkdir /hubot && chown hubot. /hubot
USER hubot
WORKDIR /hubot
COPY package.json /hubot/
RUN yarn
COPY scripts /hubot/scripts
ENV PATH="/hubot/node_modules/.bin:$PATH"
CMD hubot -a discord
