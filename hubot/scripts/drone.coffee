# Description:
#   hubot integration with drone
#
# Commands:
#   hubot test performance of <tag> using <contract(s)> - Tests performance of an rchain/rnode docker image labeled with <tag>. <contract(s)> can point to a single file or a directory with contracts - in the latter case all contracts will be deployed in alphabetical order.

{spawn,execFileSync} = require('child_process')

module.exports = (robot) ->

  robot.respond /test performance of (.*) using (.*)/i, (msg) ->
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
      msg.send "I shall see right to it!"
