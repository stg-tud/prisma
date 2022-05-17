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
let compiledContract = JSON.parse(fs.readFileSync('./../build/contracts/ManualHangman.json'))
let abi = compiledContract.abi;
let code = compiledContract.bytecode;

//Variables + Parameters
let accounts = ["0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1","0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0"];

//Helpers
let printState = async function(contract){
  let stage = await contract.methods.stage().call({from: accounts[0]})
  let word = await contract.methods.getWord().call({from: accounts[0]})
  let hashedWord = await contract.methods.hashedWord().call({from: accounts[0]})
  let currentGuess = await contract.methods.currentGuess().call({from: accounts[0]})
  let missingLetters = await contract.methods.missingletters().call({from: accounts[0]})
  let tries = await contract.methods.tries().call({from: accounts[0]})
  let guessed = await contract.methods.getGuessed().call({from: accounts[0]})
  let parties = await contract.methods.getParties().call({from: accounts[0]})
  console.log(`Stage:            ${stage} \n Parties:          ${parties} \n Word:             ${word} \n Hashed word:      ${hashedWord} \n
CurrentGuess:     ${currentGuess} \n Missing letters:  ${missingLetters} \n Attempts left:    ${tries} \n Guessed:          ${guessed}`);
}

//Parties
let challenger = async function(contract){
  let secretWord = [], nonce;
  while(true){
    let stage = await contract.methods.stage().call({from: accounts[0]})
    if(stage == 0){
      await printState(contract);
      let word = await new Promise(resolve => rl.question(`What is your secret word? `, ans => {
          resolve(ans);
      }))
      word.split("").forEach((item, i) => {
        secretWord.push(item.charCodeAt(0));
      });
      nonce = Math.floor((Math.random() * 10000000) + 1);
      commitment = web3.utils.keccak256(web3js.eth.abi.encodeParameters(['uint', 'uint[]'], [nonce, secretWord]));
      await instance.methods.commit(commitment,word.length).send({from: accounts[0], gas: 6000000})
    } else if(stage == 2){
      await printState(contract);
      let resp = await new Promise(resolve => rl.question(`Your answer? `, ans => {
          resolve(ans);
      }))
      await contract.methods.respond(JSON.parse(resp)).send({from: accounts[0], gas: 6000000})
    }
     else if (stage == 3){
      await printState(contract);
      await new Promise(resolve => rl.question(`Open it? [Input anything to confirm] `, ans => {
          resolve(ans);
      }))
      await contract.methods.open(secretWord, nonce).send({from: accounts[0], gas: 6000000})
    } else if (stage >= 4) {
      console.log("Finished");
      break;
    }
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds to check again
  }
}

let guesser = async function(contract){
  while(true){
    let stage = await contract.methods.stage().call({from: accounts[0]})
    if(stage == 1){
      await printState(contract)
      let guess = await new Promise(resolve => rl.question(`Your guess? `, ans => {
          resolve(ans);
      }))
      await contract.methods.guess(guess.charCodeAt(0)).send({from: accounts[1]})
    } else if(stage >= 4){
      printState(contract);
      console.log("Finished");
      process.exit(1);
    }
    await new Promise(resolve => setTimeout(resolve, 2 * 1000))  //Wait for 2 seconds to check again
  }
}

//Deployment + Commit
let asyncStart = async function(){
  let Contract = new web3js.eth.Contract(abi);
  instance = await Contract.deploy({data: code, arguments: [accounts]}).send({from: accounts[0], gas: 6000000})

  challenger(instance);
  guesser(instance);
}

asyncStart();
