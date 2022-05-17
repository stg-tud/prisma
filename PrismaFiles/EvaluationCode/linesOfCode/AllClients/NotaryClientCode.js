//Imports
const web3 = require('web3');
const fs = require('fs');

// !!! LoC Counted !!! + No need to thread because Prisma doesn't Thread as well

//Input
const readline = require('readline');
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

//Web3js + Contract
web3js = new web3(new web3.providers.WebsocketProvider("ws://127.0.0.1:8545"));
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualNotary.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0"];
let count = 0

//Execution
let asyncLoop = async function(instance){
  if(count < 10){
    rl.question(`Please provide some hex-encoded input for Cl-${count % 2} [without "0x"]:  `, async (resp) => {
      if(resp.length % 2 == 1) resp = "0" + resp
      let newIndex = web3.utils.keccak256(accounts[count % 2] + resp)
      await instance.methods.submit("0x" + resp).send({from: accounts[count % 2]})
      let stored = await instance.methods.getData(newIndex).call({from: accounts[0]})
      console.log("Input-Array: " + resp);
      console.log("Stored-Array: " + stored.data);
      count = count + 1
      asyncLoop(instance);
    });
  } else {
    console.log("Done!")
    process.exit(1);
  }
}

//Deployment
let asyncDeploy = async function(){
  let Contract = new web3js.eth.Contract(abi);
  instance = await Contract.deploy({data: code, arguments: []}).send({from: accounts[0], gas: 6000000})
  asyncLoop(instance)
}

asyncDeploy();
