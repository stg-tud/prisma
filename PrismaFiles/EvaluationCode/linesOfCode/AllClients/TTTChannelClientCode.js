//Imports
const web3 = require('web3');
const fs = require('fs');

// !!! LoC Counted !!!

//Input
const readline = require('readline');
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

//Web3js + Contract
web3js = new web3(new web3.providers.WebsocketProvider("ws://127.0.0.1:8545"));
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualTTTChannel.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0"];
let privateKeys = ["0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d", "0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1"];

let printBoard = function (moves, board, parties){
  console.log(`
    moves: ${moves}
    parties: ${parties}
    ${board[0][0]}|${board[1][0]}|${board[2][0]}
    - - -
    ${board[0][1]}|${board[1][1]}|${board[2][1]}
    - - -
    ${board[0][2]}|${board[1][2]}|${board[2][2]}`);
}

let toSigTuple = function(sig){
  return {
    v: sig.v,
    r: sig.r,
    s: sig.s
  }
}

//Parties
let party = async function(id, incoming, outgoing, instance){

  let offchain = {
    version: 0,
    moves: 0,
    board: [[0,0,0],[0,0,0],[0,0,0]]
  }

  let lastCommitment = {
    state: offchain,
    sig1: {},
    sig2: {}
  }

  let lastSignature = {}

  let signAndSend = async function(msgType){
    let hash = web3js.utils.keccak256(web3js.eth.abi.encodeParameter(
        {
            "State": {
              "version"  : 'uint',
              "moves"  : 'uint8',
              "board"  : 'uint8[][]'
            }
        },
        {
          "version"  : offchain.version,
          "moves"  : offchain.moves,
          "board"  : offchain.board
        }
    ));

    let sig = await web3js.eth.accounts.sign(hash, privateKeys[id - 2]);
    lastSignature = toSigTuple(sig)
    outgoing.push({
      msgType: msgType,
      state: JSON.parse(JSON.stringify(offchain)),
      sig: lastSignature})
  }

  //Init
  if(id == 2){

    console.log(`Party-${id}: Initial move`)
    console.log(`Party-${id}: ${JSON.stringify(offchain)}`)

    let x = await new Promise(resolve => rl.question("Off-chain x? ", ans => {
        resolve(ans);
    }))
    let y = await new Promise(resolve => rl.question("Off-chain y? ", ans => {
        resolve(ans);
    }))

    offchain.version = offchain.version + 1
    offchain.moves = offchain.moves + 1
    offchain.board[parseInt(x)][parseInt(y)] = id

    await signAndSend(1);

  }

  let breakIt = false
  while(!breakIt){
    while(incoming.length == 0){
      await new Promise(resolve => setTimeout(resolve, 2000))
    }

    let nextMessage = incoming.pop()

    //As we do not want to implement the full offline protocol in a secure way, we do not validate messages

    //console.log(`Party-${id} - Incoming: (${JSON.stringify(nextMessage)})`)
    console.log(`Party-${id} - Incoming: (${nextMessage.msgType})`)

    if(nextMessage.msgType == 1){
      offchain = nextMessage.state;
      await signAndSend(2);

      lastCommitment = {
        state: offchain,
        sig1: (id == 2 ? lastSignature : nextMessage.sig),
        sig2: (id == 2 ? nextMessage.sig : lastSignature)
      }

      if(offchain.moves == 4){
        outgoing.push({
          msgType: 3
        })
        incoming.push({
          msgType: 3
        })
      } else {
        printBoard(offchain.moves, offchain.board, accounts)
        let x = await new Promise(resolve => rl.question("Off-chain x? ", ans => {
            resolve(ans);
        }))
        let y = await new Promise(resolve => rl.question("Off-chain y? ", ans => {
            resolve(ans);
        }))

        offchain.version = offchain.version + 1
        offchain.moves = offchain.moves + 1
        offchain.board[parseInt(x)][parseInt(y)] = id

        await signAndSend(1);
      }
    } else if (nextMessage.msgType == 2) {

      lastCommitment = {
        state: offchain,
        sig1: (id == 2 ? lastSignature : nextMessage.sig),
        sig2: (id == 2 ? nextMessage.sig : lastSignature)
      }

      console.log(`Party-${id}: Received commitment: ${JSON.stringify(lastCommitment)}`)

    } else {
      breakIt = true
    }
  }

  console.log(`Party-${id}: Moves onchain`)
  console.log(`Party-${id}: With commitment`)
  console.log(`Party-${id}: ${JSON.stringify(lastCommitment.state)}`)

    let index = id - 2;

  //Party 1 starts with dispute
  if(id == 2){
    console.log(`Party-${id}: Starts dispute`)
    await instance.methods.dispute(lastCommitment.state, lastCommitment.sig1, lastCommitment.sig2).send({from: accounts[index], gas: 6000000});
  } else {
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds before continue such that dispute is indeed submitted
  }

  while(true){
    let timeout = await instance.methods.timeout().call({from: accounts[index]})
    let version = (await instance.methods.getOnchain().call({from: accounts[index]})).version
    if(timeout < Math.floor(Date.now() / 1000)){
      break;
    }
    if(id == 3 && version < lastCommitment.state.version){
      console.log(`Party-${id}: Submits more recent`)
      await instance.methods.dispute(lastCommitment.state, lastCommitment.sig1, lastCommitment.sig2).send({from: accounts[index], gas: 6000000});
    }
  }

  console.log("Disputing done");

  while(true){
    let moves = await (await instance.methods.getOnchain().call({from: accounts[index]})).moves
    let parties = await instance.methods.getParties().call({from: accounts[index]})
    let board = (await instance.methods.getOnchain().call({from: accounts[index]})).board
    if(moves >= 9){
      printBoard(moves, board, parties);
      console.log("Finished");
      process.exit(1);
    }
    if(moves % 2 == index){
      printBoard(moves, board, parties);
      let x = await new Promise(resolve => rl.question(`X-Position? `, ans => {
          resolve(ans);
      }))
      let y = await new Promise(resolve => rl.question(`Y-Position? `, ans => {
          resolve(ans);
      }))
      await instance.methods.move(x,y).send({from: accounts[index], gas: 6000000})
    }
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds to check again
  }

}


let asyncDeploy = async function(){

  let Contract = new web3js.eth.Contract(abi);
  let instance = await Contract.deploy({data: code, arguments: [accounts]}).send({from: accounts[0], gas: 6000000})

  let messagesToA = []
  let messagesToB = []

  //Start "Threads"
  party(2, messagesToA, messagesToB, instance)
  party(3, messagesToB, messagesToA, instance)
}



asyncDeploy()
