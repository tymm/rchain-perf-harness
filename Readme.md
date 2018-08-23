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

- `ConinousRunner`
- `DeployProposeSimulation`

### Automation
#### drone
Configuration and tools for a CI/CD tool that is used to setup an rchain network.

How to setup?

*TBD*

##### etc
Holds scripts that are used by drone to scrape results from `runner`.

#### hubot
Configuration for a bot attached to discord.
A `hubot test performance of master using contract contracts/dupe.rho` will invoke a test using `master` image and `contracts/dupe.rho` contract.

#### tools
Holds a small set of tools that help build a perf runner docker image.
To get a current version:

- `setup.sh $$version$$`

will result in an `rchain-perf-runner:$$version$$` docker image.

This image can be then transported with `docker save` and `docker load` commands.


### Metrics
#### prometheus & grafana
Configuration for docker-compose.
The usual `docker-compose up` should suffice to get monitoring.

### Performance testing
#### setup

Assumption: an environment that has the `boot-p2p` script running:

- `./scripts/boot-p2p.py -b -p 3 -c 2 -m 2048m -i rchain/rnode:dev`
- `docker run -d -v /root/perf-runner/test-contracts:/contracts -e "loops=1" -e "sessions=3" -e "ratio=1" -e "hosts=peer1.rchain.coop" --name perf-runner --network rchain.coop rchain-perf-runner:0.1.0`


#### rchain-perf-runner:0.1.0
This is the image that was built using Automation/tools script

#### perf-runner parameters
- `-v /root/perf-runner/test-contracts:/contracts` directory with contracts to run
- `loops` times to repeat the above directory
- `sessions` amount of concurrent clients
- `ratio` deploy : propose ratio
- `hosts` space separated list of hosts to test (assumed port is 40401)