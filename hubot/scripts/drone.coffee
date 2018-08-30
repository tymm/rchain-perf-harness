# Description:
#   hubot integration with drone
#
# Commands:
#   hubot test performance of <docker-tag> using <cmd> on <path> - Tests performance of an rchain/rchain docker image labeled with <docker-tag>. <cmd> can be either 'contract' or 'path'. The first expects a path to a contract, the latter a path to a directory containing contracts which will be deployed in alphabetical order.

{spawn,execFileSync} = require('child_process')

module.exports = (robot) ->

  robot.respond /test performance of (.*) using (.*) on (.*)/i, (msg) ->
    tag = msg.match[1]
    cmd = msg.match[2]
    contract = msg.match[3]

    lastSuccessfulBuildNo = execFileSync('./drone-cli.sh', ['build', 'ls' ,'--status', 'success', '--format', '{{.Number}}', '--limit', '1', 'rchain/rchain-perf-harness'],{
      cwd: '../drone'
    }).toString()

    child = spawn("bash", ["./drone-custom-contract.sh", lastSuccessfulBuildNo, "#{cmd}", "#{contract}", "#{tag}"], {
      cwd: '../drone'
    })

    child.stdout.on 'data', (data) ->
      #console.log('stdout: ' + data)
      msg.send "I shall see right to it!"
