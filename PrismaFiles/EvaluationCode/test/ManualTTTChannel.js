let names = {
  testName: "ManualTTTChannel",
  testedContract : "ManualTTTChannel",
  disputeMethod: "dispute",
  moveMethod : "move"
}

const Contract = artifacts.require(names.testedContract);
const TTT = require("./TTT.js");

let gasLog = [];

let commitments = async function(_accounts){

  let states = [
    [3,3,[[2,0,2],[0,0,0],[0,0,3]]],
    [4,4,[[2,3,2],[0,0,0],[0,0,3]]],
    [5,5,[[2,3,2],[0,0,0],[2,0,3]]],
    [6,6,[[2,3,2],[0,3,0],[2,0,3]]]
  ]

  let result = [];

  for(let i = 0; i < 4; i++){
    let hash = web3.utils.keccak256(web3.eth.abi.encodeParameter(
        {
            "State": {
              "version"  : 'uint',
              "moves"  : 'uint8',
              "board"  : 'uint8[][]'
            }
        },
        {
          "version"  : states[i][0],
          "moves"  : states[i][1],
          "board"  : states[i][2]
        }
    ));
//    console.log("Hash: " +  hash)
    let signature1 = await web3.eth.accounts.sign(hash, "0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d");
    let signature2 = await web3.eth.accounts.sign(hash, "0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1");

    result.push({
        "state": {
           "version": states[i][0],
           "moves": states[i][1],
           "board": states[i][2]
       }, "sig1": {
            "v": signature1.v,
            "r": signature1.r,
            "s": signature1.s
       }, "sig2": {
           "v": signature2.v,
           "r": signature2.r,
           "s": signature2.s
        }
    });

  }

  return result;


}

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new([accounts[0],accounts[1]]);
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Game - Party 1 wins in seven, dispute in 3 and 4", async() => {

    let instance = await Contract.new([accounts[0],accounts[1]]);

    let tx;
    let moves = TTT.winSevenMoves;

    let data = await commitments(accounts);

    tx = await instance[names.disputeMethod](data[0].state, data[0].sig1, data[0].sig2);
    gasLog.push({
        function: "dispute-1",
        gas: tx.receipt.gasUsed
    });

    tx = await instance[names.disputeMethod](data[1].state, data[1].sig1, data[1].sig2, {from: accounts[1]});
    gasLog.push({
        function: "dispute-2",
        gas: tx.receipt.gasUsed
    });

    await new Promise(r => setTimeout(r, 11000));

    for(let i = 4; i < moves.length; i++){
      if(i % 2 == 0){
        tx = await instance[names.moveMethod](moves[i][0],moves[i][1]);
      } else {
        tx = await instance[names.moveMethod](moves[i][0],moves[i][1], {from: accounts[1]});
      }
      gasLog.push({
        function: "move",
        gas: tx.receipt.gasUsed
      });
    }
  });

  it("Game - Party 2 wins in eight, dispute in 4 and 5", async() => {

    let instance = await Contract.new([accounts[0],accounts[1]]);

    let tx, consumptions = [];
    let moves = TTT.winEightMoves;

    let data = await commitments(accounts);

    tx = await instance[names.disputeMethod](data[1].state, data[1].sig1, data[1].sig2);
    gasLog.push({
        function: "dispute-1",
        gas: tx.receipt.gasUsed
    });

    tx = await instance[names.disputeMethod](data[2].state, data[2].sig1, data[2].sig2, {from: accounts[1]});
    gasLog.push({
        function: "dispute-2",
        gas: tx.receipt.gasUsed
    });

    await new Promise(r => setTimeout(r, 11000));

    for(let i = 5; i < moves.length; i++){
      if(i % 2 == 0){
        tx = await instance[names.moveMethod](moves[i][0],moves[i][1]);
      } else {
        tx = await instance[names.moveMethod](moves[i][0],moves[i][1], {from: accounts[1]});
      }
      gasLog.push({
        function: "move",
        gas: tx.receipt.gasUsed
      });
    }
  });

  it("Game - Draw in 9, dispute in 5 and 6", async() => {

    let instance = await Contract.new([accounts[0],accounts[1]]);

    let tx, consumptions = [];
    let moves = TTT.drawMoves;

    let data = await commitments(accounts);

    tx = await instance[names.disputeMethod](data[2].state, data[2].sig1, data[2].sig2);
    gasLog.push({
        function: "dispute-1",
        gas: tx.receipt.gasUsed
    });

    tx = await instance[names.disputeMethod](data[3].state, data[3].sig1, data[3].sig2, {from: accounts[1]});
    gasLog.push({
        function: "dispute-2",
        gas: tx.receipt.gasUsed
    });

    await new Promise(r => setTimeout(r, 11000));

    for(let i = 6; i < moves.length; i++){
      if(i % 2 == 0){
        tx = await instance[names.moveMethod](moves[i][0],moves[i][1]);
      } else {
        tx = await instance[names.moveMethod](moves[i][0],moves[i][1], {from: accounts[1]});
      }
      gasLog.push({
        function: "move",
        gas: tx.receipt.gasUsed
      });
    }
  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
