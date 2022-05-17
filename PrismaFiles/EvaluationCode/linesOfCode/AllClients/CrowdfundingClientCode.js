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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualCrowdfunding.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0"];
let decision = "No"

//Helpers
let printState = async function (instance){
  let owner = await instance.methods.owner().call({from: accounts[0]})
  let goal = await instance.methods.goal().call({from: accounts[0]})
  let balance = await web3js.eth.getBalance(instance.options.address)
  let deadline = await instance.methods.deadline().call({from: accounts[0]})
  console.log(`Owner: ${owner}\n  Goal: ${goal} \n Balance: ${balance} \n Deadline: ${deadline}`);
}

//Parties
let owner = async function(instance){
  await printState(instance)
  let goal = await instance.methods.goal().call({from: accounts[0]})

  if(decision == "Yes"){
    console.log("Owner funds up to the goal")
    await instance.methods.fund().send({from: accounts[0], value: goal - 10})
  }

  await new Promise(resolve => setTimeout(resolve, 11 * 1000))  //Wait for 10 seconds before checking what to do

  await printState(instance)
  let balance = await web3js.eth.getBalance(instance.options.address)
  goal = await instance.methods.goal().call({from: accounts[0]})
  if(balance >= goal){
    console.log("Owner claims")
    await instance.methods.claim().send({from: accounts[0]})
    console.log("Done");
    process.exit(1);
  }
}

let funder = async function(instance){

  await printState(instance)
  console.log("Second party always funds 2 wei")

  await instance.methods.fund().send({from: accounts[1], value: 10})

  await new Promise(resolve => setTimeout(resolve, 11 * 1000))  //Wait for 10 seconds before checking what to do

  await printState(instance)
  let balance = await web3js.eth.getBalance(instance.options.address)
  let goal = await instance.methods.goal().call({from: accounts[1]})
  if(balance > 0 && balance < goal){
    console.log("Second party refunds");
    await instance.methods.refund().send({from: accounts[1]})
    console.log("Done");
    process.exit(1);
  }
}

//Execution
let asyncExec = async function(){
  let Contract = new web3js.eth.Contract(abi);
  let instance = await Contract.deploy({data: code, arguments: [100, Math.floor(Date.now() / 1000) + 10]}).send({from: accounts[0], gas: 6000000})

  decision = await new Promise(resolve => rl.question("Should the goal be reached? [Yes, No]?", ans => {
      resolve(ans);
  }))

  owner(instance);
  funder(instance);
}

asyncExec();
