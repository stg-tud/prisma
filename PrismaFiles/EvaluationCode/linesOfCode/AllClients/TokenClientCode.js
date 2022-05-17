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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualToken.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0", "0x22d491Bde2303f2f43325b2108D26f1eAbA1e32b", "0xE11BA2b4D45Eaed5996Cd0823791E0C93114882d"];

//Deploy
let asyncDeploy = async function(){
  let Contract = new web3js.eth.Contract(abi);
  rl.question(`Initial supply? `, async (resp) => {
    instance = await Contract.deploy({data: code, arguments: [  parseInt(resp)]}).send({from: accounts[0], gas: 6000000})
    asyncLoop(instance);
  });
}

//Execution
let asyncLoop = async function(instance){
  rl.question(`Client? [0,1,2,3] `, async (senderIndex) => {
    rl.question(`Action? [T: Transfer, A: Approve, F: TransferFrom, Q:Quit] `, async (action) => {
      rl.question(`Amount? `, async (amount) => {
        rl.question(`Receiver? [0,1,2,3] `, async (receiverIndex) => {
          if(action == "T"){
            await instance.methods.transfer(accounts[parseInt(receiverIndex)], parseInt(amount)).send({from: accounts[parseInt(senderIndex)]})
            console.log(`Transferring ${amount} tokens from Cl-${senderIndex} to Cl-${receiverIndex}`)
            asyncLoop(instance)
          } else if (action == "A"){
            await instance.methods.approve(accounts[parseInt(receiverIndex)], parseInt(amount)).send({from: accounts[parseInt(senderIndex)]})
            console.log(`Approving ${amount} tokens from Cl-${senderIndex} to Cl-${receiverIndex}`)
            asyncLoop(instance)
          } else if (action == "F") {
            rl.question(`From Who? [0, 1, 2, 3] `, async (fromWho) => {
              await instance.methods.transferFrom(accounts[parseInt(fromWho)], accounts[parseInt(receiverIndex)], parseInt(amount)).send({from: accounts[parseInt(senderIndex)]})
              console.log(`Transferring ${amount} tokens from Cl-${fromWho} to Cl-${receiverIndex}`)
              asyncLoop(instance)
            });
          } else if (action == "Q") {
            console.log("Stopping the application")
            process.exit(1);
          } else {
            asyncLoop(instance)
          }
        });
      });
    });
  });
}

asyncDeploy();
