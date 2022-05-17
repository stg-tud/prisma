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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualChineseCheckers.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;


//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0", "0x22d491Bde2303f2f43325b2108D26f1eAbA1e32b"];

//Helpers
let printIt = function (moves, positions){
  console.log(`
    moves: ${moves}
    positions: ${positions}`);
}

//Parties
let client = async function(contract, id){
  while(true){
    let moves = await contract.methods.moves().call({from: accounts[0]})
    let positions = await contract.methods.getPositions().call({from: accounts[0]})
    if(moves >= 5000){
      printIt(moves, positions);
      console.log("Finished");
      process.exit(1);
    }
    if(moves % 3 == id){
      printIt(moves, positions);
      let root = await new Promise(resolve => rl.question(`Root? `, ans => {
          resolve(ans);
      }))
      let dimString = await new Promise(resolve => rl.question(`Dimensions? [separated by comma] `, ans => {
          resolve(ans);
      }))
      await contract.methods.move(root,dimString.split(",")).send({from: accounts[id], gas: 6000000})

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
  client(instance, 2)
}

asyncDeploy();
