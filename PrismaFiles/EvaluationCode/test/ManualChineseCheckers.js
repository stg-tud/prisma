let names = {
  testName: "ManualChineseCheckers",
  testedContract : "ManualChineseCheckers",
  moveMethod : "move"
}

const Contract = artifacts.require(names.testedContract);
const CC = require("./CCMoves.js");

let gasLog = [];

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new([accounts[0], accounts[1], accounts[2]]);
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Game - one full execution", async() => {

    let instance = await Contract.new([accounts[0], accounts[1], accounts[2]]);

    let tx;
    let moves = CC.getMoves();

    for(let i = 0; i < moves.length; i++){
      tx = await instance[names.moveMethod](moves[i].root, moves[i].hops, {from: accounts[i % 3]});
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
