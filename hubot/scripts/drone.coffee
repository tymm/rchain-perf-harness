# Description:
#   hubot integration with drone
#
# Commands:
#   hubot test performance of image <tag> using <contract(s)> - Tests performance of an rchain/rnode docker image labeled with <tag>. <contract(s)> can point to a single file or a directory with contracts - in the latter case all contracts will be deployed in alphabetical order.
#   hubot test performance of commit <hash> using <contract(s)> - Tests performance of an image built from rchain/rchain:<hash>. <contract(s)> can point to a single file or a directory with contracts - in the latter case all contracts will be deployed in alphabetical order.

{spawn,execFileSync} = require('child_process')

module.exports = (robot) ->

  robot.respond /test performance of image (.*) using (.*)/i, (msg) ->
    tag = msg.match[1]
    contract = msg.match[2]

    lastSuccessfulBuildNo = execFileSync('./drone-cli.sh', ['build', 'ls' ,'--status', 'success', '--format', '{{.Number}}', '--limit', '1', 'rchain/rchain-perf-harness'],{
      cwd: '../drone'
    }).toString()

    child = spawn("bash", ["./drone-custom-contract.sh", lastSuccessfulBuildNo, "#{contract}", "#{tag}"], {
      cwd: '../drone'
    })

    child.stdout.on 'data', (data) ->
      #console.log('stdout: ' + data)
      msg.send ("Scheduled build http://stress-docker.pyr8.io:8080/rchain/rchain-perf-harness/" + data)

  robot.respond /test performance of commit (.*) using (.*)/i, (msg) ->
    hash = msg.match[1]
    contract = msg.match[2]

    lastSuccessfulBuildNo = execFileSync('./drone-cli.sh', ['build', 'ls' ,'--status', 'success', '--format', '{{.Number}}', '--limit', '1', 'rchain/rchain-perf-harness'],{
      cwd: '../drone'
    }).toString()

    child = spawn("bash", ["./drone-custom-commit.sh", lastSuccessfulBuildNo, "#{contract}", "#{hash}"], {
      cwd: '../drone'
    })

    child.stdout.on 'data', (data) ->
      #console.log('stdout: ' + data)
      msg.send ("Scheduled build http://stress-docker.pyr8.io:8080/rchain/rchain-perf-harness/" + data)
