{spawn,execFileSync} = require('child_process')

module.exports = (robot) ->

  robot.respond /test performance of (.*) using contract (.*)/i, (msg) ->
    tag = msg.match[1]
    contract = msg.match[2]

    lastSuccessfulBuildNo = execFileSync('./drone-cli.sh', ['build', 'ls' ,'--status', 'success', '--format', '{{.Number}}', '--limit', '1', 'lukasz-golebiewski-org/rchain-perf-harness'],{
      cwd: '../drone'
    }).toString()

    child = spawn("bash", ["./drone-custom-contract.sh", lastSuccessfulBuildNo, "#{contract}", "#{tag}"], {
      cwd: '../drone'
    })

    child.stdout.on 'data', (data) ->
      #console.log('stdout: ' + data)
      msg.send "I shall see right to it!"
