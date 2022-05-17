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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualEscrow.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0", "0x22d491Bde2303f2f43325b2108D26f1eAbA1e32b"];
let price = 100;

//Helpers
let printState = async function (instance){
  let phase = await instance.methods.phase().call({from: accounts[0]})
  let buyer = await instance.methods.buyer().call({from: accounts[0]})
  let seller = await instance.methods.seller().call({from: accounts[0]})
  let arbiter = await instance.methods.arbiter().call({from: accounts[0]})
  console.log(`Phase: ${phase}\n  Buyer: ${buyer} \n Seller: ${seller} \n Arbiter: ${arbiter}`);
}

//Parties
let buyer = async function(instance){
  await printState(instance)
  await new Promise(resolve => rl.question("Pay? [Input anything to confirm]", ans => {
      resolve(ans);
  }))

  await instance.methods.pay().send({from: accounts[0], value: price})

  await printState(instance)
  let decision = await new Promise(resolve => rl.question("Confirm delivery? [Yes, No]", ans => {
      resolve(ans);
  }))

  if(decision == "Yes"){
    await instance.methods.confirmDelivery().send({from: accounts[0]})
    console.log("Done");
    process.exit(1);
  }
}

let arbiter = async function(instance){
  await new Promise(resolve => setTimeout(resolve, 10 * 1000))  //Wait for 10 seconds before resolving the dispute

  let phase = await instance.methods.phase().call({from: accounts[2]})
  if(phase != 0){
    await printState(instance)
    let decision = await new Promise(resolve => rl.question("Confirm delivery? [1: Buyer gets money, 0: Seller gets money]", ans => {
        resolve(ans);
    }))
    await instance.methods.arbitrate(parseInt(decision)).send({from:accounts[2]})
  }
  console.log("Done");
  process.exit(1);
}

//Execution
let asyncExec = async function(){
  let Contract = new web3js.eth.Contract(abi);
  let instance = await Contract.deploy({data: code, arguments: [accounts[0], accounts[1], accounts[2], price]}).send({from: accounts[0], gas: 6000000})

  buyer(instance);
  arbiter(instance);
}

asyncExec();
