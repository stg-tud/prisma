let names = {
  testName: "CompiledTTT",
  testedContract : "CompiledTTT",
  //initMethod: "",
  moveMethod : "move",
  passPartiesToConstructor: true,
}

const Contract = artifacts.require(names.testedContract);
const TTT = require("./TTT.js");

let gasLog = [];

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = 'passPartiesToConstructor' in names
       ? await Contract.new([accounts[0], accounts[1]])
       : await Contract.new();
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Game - Party 1 wins in seven", async() => {

    let instance = 'passPartiesToConstructor' in names
       ? await Contract.new([accounts[0], accounts[1]])
       : await Contract.new();

    let tx, tx2;
    let moves = TTT.winSevenMoves;

    // tx = await instance[names.initMethod]();
    // gasLog.push({
    //   function: "init",
    //   gas: tx.receipt.gasUsed
    // });


    for(let i = 0; i < moves.length; i++){
      if(i % 2 == 0){
        tx = await instance[names.moveMethod](moves[i]);
      } else {
        tx = await instance[names.moveMethod](moves[i], {from: accounts[1]});
      }
      gasLog.push({
        function: "move",
        gas: tx.receipt.gasUsed
      });
    }
  });

  it("Game - Party 2 wins in eight", async() => {

    let instance = 'passPartiesToConstructor' in names
       ? await Contract.new([accounts[0], accounts[1]])
       : await Contract.new();

    let tx, tx2;
    let moves = TTT.winEightMoves;

    // tx = await instance[names.initMethod]();
    // gasLog.push({
    //   function: "init",
    //   gas: tx.receipt.gasUsed
    // });

    for(let i = 0; i < moves.length; i++){
      if(i % 2 == 0){
        tx = await instance[names.moveMethod](moves[i]);
      } else {
        tx = await instance[names.moveMethod](moves[i], {from: accounts[1]});
      }
      gasLog.push({
        function: "move",
        gas: tx.receipt.gasUsed
      });
    }
  });

  it("Game - Draw", async() => {

    let instance = 'passPartiesToConstructor' in names
       ? await Contract.new([accounts[0], accounts[1]])
       : await Contract.new();

    let tx, tx2;
    let moves = TTT.drawMoves;

    // tx = await instance[names.initMethod]();
    // gasLog.push({
    //   function: "init",
    //   gas: tx.receipt.gasUsed
    // });

    for(let i = 0; i < moves.length; i++){
      if(i % 2 == 0){
        tx = await instance[names.moveMethod](moves[i]);
      } else {
        tx = await instance[names.moveMethod](moves[i], {from: accounts[1]});
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
