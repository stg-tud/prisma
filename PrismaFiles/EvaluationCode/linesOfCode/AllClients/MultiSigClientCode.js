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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualMultiSig.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0", "0x22d491Bde2303f2f43325b2108D26f1eAbA1e32b"];
let privateKeys = ["0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d", "0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1", "0x6370fd033278c143179d81c5526140625662b8daa446c22ee2d73db3707e620c"];
let target = "0x5b1869D9A4C187F2EAa108f3062412ecf0526b24";

let txData = [
  "0x276afec6",
  "0x0864c21e000000000000000000000000000000000000000000000000000000000000002a",
  "0xaec5aa5b000000000000000000000000000000000000000000000000000000000000002a0000000000000000000000000000000000000000000000000000000000000fa0",
  "0x98f1529c0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000cabcdefabcdefabcdefabcdef0000000000000000000000000000000000000000",
  "0x98f1529c00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000024abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef00000000000000000000000000000000000000000000000000000000",
  "0x83d35345"
]

//Helper

let prefix = function (x, length){
  return "0x" + x.substring(2).padStart(length * 2, "0")
}

//Execution
let asyncLoop = async function(instance){
  rl.question(`Action? [1,2,3,4,5,6, Q = Quit]  `, async (action) => {
    if(action == "Q"){
      console.log("Stopping the application")
      process.exit(1);
    } else if (parseInt(action) > 0 && parseInt(action) < 7){
      nonce = await instance.methods.nonce().call({from: accounts[0]})
      let timeout = (Date.now() / 1000 | 0) + 60 * 60;
      let value = (parseInt(action) == 6 ? 10000 : 0)
      let encoded = prefix(instance._address, 16) + prefix("0x" + nonce.toString(16), 32).substring(2) + prefix("0x" + timeout.toString(16), 32).substring(2) + prefix(target, 16).substring(2) + prefix("0x" + value.toString(16), 32).substring(2) + txData[parseInt(action-1)].substring(2);
      let hash = web3.utils.sha3(encoded);
      let signature1 = await web3js.eth.accounts.sign(hash, privateKeys[0]);
      let signature2 = await web3js.eth.accounts.sign(hash, privateKeys[1]);
      await instance.methods.execute([signature1, signature2], [0,1], timeout, target, value, txData[parseInt(action-1)]).send({from: accounts[0]})
      console.log("Successful submission");
      asyncLoop(instance);
    } else {
      asyncLoop(instance);
    }
  });
}

//Deployment
let asyncDeploy = async function(){
  let Contract = new web3js.eth.Contract(abi);
  let instance = await Contract.deploy({data: code, arguments: [2, accounts]}).send({from: accounts[0], gas: 6000000})
  asyncLoop(instance)
}

asyncDeploy();
