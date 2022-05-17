let names = {
  testName: "ManualTTTViaLib",
  testedContract : "ManualTTTViaLib",
  moveMethod : "move",
  withdrawMethod : "payout"
}

const Contract = artifacts.require(names.testedContract);
const TTT = require("./TTT.js");

let gasLog = [];

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new([accounts[0], accounts[1]]);
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Game - Party 1 wins in seven", async() => {

    let instance = await Contract.new([accounts[0], accounts[1]]);

    let tx;
    let moves = TTT.winSevenMoves;

    for(let i = 0; i < moves.length; i++){
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

    assert.equal(10, await instance.moves.call());
  });

  it("Game - Party 2 wins in eight", async() => {

    let instance = await Contract.new([accounts[0], accounts[1]]);

    let tx, consumptions = [];
    let moves = TTT.winEightMoves;

    for(let i = 0; i < moves.length; i++){
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

    assert.equal(11, await instance.moves.call());
  });

  it("Game - Draw", async() => {

    let instance = await Contract.new([accounts[0], accounts[1]]);

    let tx, consumptions = [];
    let moves = TTT.drawMoves;

    for(let i = 0; i < moves.length; i++){
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

    assert.equal(9, await instance.moves.call());
  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
