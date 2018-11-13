Performance Harness
===

## Modules:
### builder
A simple approach to generating environments. Entry point is `init.sh`. Takes in a few parameters and tries to setup an enviromnemnts using assembly jars.


#### templater/templater
A tool that is used by `builder` to generate configs.

### templater/runner
Gatling based rnode client.
Classes of note:

- [`ContinuousRunner`](templater/runner/src/main/scala/coop/rchain/perf/ContinuousRunner.scala)
- [`DeployProposeSimulation`](templater/runner/src/test/scala/coop/rchain/perf/DeployProposeSimulation.scala)

### Automation
#### [drone](.drone.yml)
Configuration and tools for a CI/CD tool that is used to setup an rchain network.

How to setup?

*TBD*

##### etc
Holds scripts that are used by drone to scrape results from `runner`.

#### hubot
Configuration for a bot attached to discord.
A `hubot test performance of master using contract contracts/dupe.rho` will invoke a test using `master` image and `contracts/dupe.rho` contract.

#### tools
Holds a small set of [tools](tools/setup.sh) that help build a perf runner docker image.
To get a current version:

- `setup.sh $$version$$`

will result in an `rchain-perf-runner:$$version$$` docker image.

This image can be then transported with `docker save` and `docker load` commands.


### Metrics
#### prometheus & grafana
Configuration for [docker-compose](docker-compose.yml)
The usual `docker-compose up` should suffice to get monitoring.

### Performance testing
#### setup

Assumption: an environment that has the `boot-p2p` script running:

- `./scripts/boot-p2p.py -b -p 3 -c 2 -m 2048m -i rchain/rnode:dev`
- `docker run -d -v /root/perf-runner/test-contracts:/contracts -e "loops=1" -e "sessions=3" -e "ratio=1" -e "hosts=peer1.rchain.coop" --name perf-runner --network rchain.coop rchain-perf-runner:0.1.0`


#### rchain-perf-runner:0.1.0
This is the image that was built using [Automation/tools script](tools/setup.sh)

#### perf-runner parameters
- `-v /root/perf-runner/test-contracts:/contracts` directory with contracts to run
- `loops` times to repeat the above directory
- `sessions` amount of concurrent clients
- `ratio` deploy : propose ratio
- `hosts` space separated list of hosts to test (assumed port is 40401)

## Installation

Docker, docker-compose and systemd are required for installation and run time.
Bash, rsync and Git are required only for installation.

### Installation steps

1. Clone the repository on your server and `cd` into it.
2. `cd` into `systemd` directory and run `./install`. This will install copy of
   the repository files into `/opt/rchain-perf-harness` and configuration files
   into `/etc/rchain-perf-harness`.
3. Edit the `/etc/rchain-perf-harness/drone.env` configuration file:

       DRONE_HOST

   Set this to HTTP(S) URL that'll point to this Drone instance from outside.

       DRONE_GITHUB_CLIENT
       DRONE_GITHUB_SECRET

   Set this to values of an GitHub OAuth application created at
   https://github.com/organizations/rchain/settings/applications

       DRONE_SECRET

   Set this to a random string. It's used to authenticate Drone agent with
   server. Both server and agent source this file.

       DRONE_ORGS

   Set this to comma separated list of GitHub organizations whose users can use
   this Drone instance.

       DRONE_ADMIN

   Set this to comma separated list of GitHub users who can manage this Drone
   instance.

4. Start the metrics and Drone services:

       systemctl start metrics drone

5. Edit the `/etc/rchain-perf-harness/hubot.env` configuration file:

       DRONE_SERVER
       DRONE_TOKEN

   Get these values from `${DRONE_HOST}/account/token` page.

       DRONE_BUILD_REPO

   _GitHub path_ of this repository, i.e. most probably
   `rchain/rchain-perf-harness` unless this is a fork.

       HUBOT_DISCORD_TOKEN

   Set this to token of a Discord bot you've created.

6. Start the Hubot:

       systemctl start hubot

Services will be started on every boot. Check with

    systemctl status rchain-perf-harness.target
    systemctl status metrics
    systemctl status drone
    systemctl status hubot
