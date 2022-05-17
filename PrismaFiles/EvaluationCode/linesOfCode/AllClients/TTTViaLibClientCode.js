//Imports
const web3 = require('web3');
const fs = require('fs');

// !!! LoC Counted !!! + Threaded

//Input
const readline = require('readline');
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

//Web3js + Contract
web3js = new web3(new web3.providers.WebsocketProvider("ws://127.0.0.1:8545"));
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualTTTViaLib.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;


//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0"];

//Helpers
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

//Parties
let client = async function(contract, id){
  while(true){
    let moves = await contract.methods.moves().call({from: accounts[0]})
    let parties = await contract.methods.getParties().call({from: accounts[0]})
    let board = await contract.methods.getBoard().call({from: accounts[0]})
    if(moves >= 9){
      printBoard(moves, board, parties);
      console.log("Finished");
      process.exit(1);
    }
    if(moves % 2 == id){
      printBoard(moves, board, parties);
      let x = await new Promise(resolve => rl.question(`X-Position? `, ans => {
          resolve(ans);
      }))
      let y = await new Promise(resolve => rl.question(`Y-Position? `, ans => {
          resolve(ans);
      }))
      await contract.methods.move(x,y).send({from: accounts[id], gas: 6000000})
    }
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds to check again
  }

}

//Deployment
let asyncDeploy = async function(){
  let Contract = new web3js.eth.Contract(abi);
  instance = await Contract.deploy({data: code, arguments: [accounts]}).send({from: accounts[0], gas: 6000000})
  client(instance, 0)
  client(instance, 1)
}

asyncDeploy();
