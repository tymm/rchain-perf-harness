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
