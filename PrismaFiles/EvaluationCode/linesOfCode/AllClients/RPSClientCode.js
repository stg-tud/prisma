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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualRPS.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0"];

//Helpers
const genRanHex = size => [...Array(size)].map(() => Math.floor(Math.random() * 16).toString(16)).join('');
let printState = async function (instance){
  let parties = await instance.methods.getParties().call({from: accounts[0]})
  let result = await instance.methods.result().call({from: accounts[0]})
  let commitment = await instance.methods.commitment().call({from: accounts[0]})
  let rps2 = await instance.methods.rps2().call({from: accounts[0]})
  console.log(`parties: ${parties}\n  result: ${result} \n commitment: ${commitment} \n move2: ${rps2}`);
}

//Parties
let party1 = async function(instance){

  await printState(instance);

  let move = parseInt(await new Promise(resolve => rl.question(`P1 Move? [1-3] `, ans => {
      resolve(ans);
  })))
  let nonce = "0x" + genRanHex(64);
  let commitment = web3.utils.keccak256(web3js.eth.abi.encodeParameters(['uint8', 'uint'], [move, nonce]));
  await instance.methods.commit(commitment).send({from: accounts[0]});

  while(true){
    let result = await instance.methods.result().call({from: accounts[0]})
    if(result < 4){
      console.log("Finished");
      break;
    } else if( result == 4){
      await printState(instance)
      let dec = await new Promise(resolve => rl.question(`Open it now? [Yes, No] `, ans => {
          resolve(ans);
      }))
      if(dec == "Yes"){
        await instance.methods.open(move, nonce).send({from: accounts[0]})
      } else {
        await new Promise(resolve => setTimeout(resolve, 30 * 1000))  //Wait for 2 seconds to check again
      }
    }
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds to check again
  }
}

let party2 = async function(instance){
  while(true){
    let result = await instance.methods.result().call({from: accounts[0]})
    let deadline = await instance.methods.deadline().call({from: accounts[0]})
    if(result < 4){
      await printState(instance);
      console.log("Finished");
      process.exit(1);
    } else if(result == 5){
      await printState(instance)
      let move = await new Promise(resolve => rl.question(`P2 Move? [1-3] `, ans => {
          resolve(ans);
      }))
      await instance.methods.move(parseInt(move)).send({from: accounts[1]})
    } else if (result == 4 && deadline < Math.floor(Date.now() / 1000)) {
      await new Promise(resolve => rl.question(`Trigger timeout [Press anything to confirm] `, ans => {
          resolve(ans);
      }))
      await instance.methods.timeout().send({from: accounts[1]})
    }
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds to check again
  }
}

let start = async function(){
  let Contract = new web3js.eth.Contract(abi);
  let instance = await Contract.deploy({data: code, arguments: [accounts]}).send({from: accounts[0], gas: 6000000})

  party1(instance);
  party2(instance);
}

start();
