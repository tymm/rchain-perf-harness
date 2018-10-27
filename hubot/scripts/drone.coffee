# Description:
#   hubot integration with drone
#
# Commands:
#   hubot test performance of image <tag> using <contract(s)> - Tests performance of an rchain/rnode docker image labeled with <tag>. <contract(s)> can point to a single file or a directory with contracts - in the latter case all contracts will be deployed in alphabetical order.
#   hubot test performance of commit <hash> using <contract(s)> - Tests performance of an image built from rchain/rchain:<hash>. <contract(s)> can point to a single file or a directory with contracts - in the latter case all contracts will be deployed in alphabetical order.

{spawn,execFileSync} = require('child_process')

baseUrl = process.env.DRONE_SERVER + '/' + process.env.DRONE_BUILD_REPO + '/'

module.exports = (robot) ->

  robot.respond /test performance of image (.*) using (.*)/i, (msg) ->
    tag = msg.match[1]
    contract = msg.match[2]

    child = spawn('rchainperfharness', ['dockerimg', contract, tag])

    child.stdout.on 'data', (data) ->
      #console.log('stdout: ' + data)
      msg.send ("Scheduled build #{baseUrl}" + data)

  robot.respond /test performance of commit (.*) using (.*)/i, (msg) ->
    hash = msg.match[1]
    contract = msg.match[2]

    child = spawn('rchainperfharness', ['gitrev', contract, hash])

    child.stdout.on 'data', (data) ->
      #console.log('stdout: ' + data)
      msg.send ("Scheduled build #{baseUrl}" + data)
