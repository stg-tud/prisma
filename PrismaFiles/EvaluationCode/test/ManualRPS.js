let names = {
  testName: "ManualRPS",
  testedContract : "ManualRPS",
  commit: "commit",
  move : "move",
  open : "open",
  timeout: "timeout"
}

const Contract = artifacts.require(names.testedContract);

let gasLog = [];

let oneRun = async function(_move1, _move2, _nonce, _expection, accounts, timeout = false){

  let instance = await Contract.new([accounts[0], accounts[1]]);
  let tx;

  let commitment = web3.utils.sha3(web3.eth.abi.encodeParameters(['uint8','uint'], [_move1, _nonce]));

  tx = await instance[names.commit](commitment);
  gasLog.push({
      function: "commit",
      gas: tx.receipt.gasUsed
  });

  tx = await instance[names.move](_move2, {from: accounts[1]});
  gasLog.push({
      function: "move",
      gas: tx.receipt.gasUsed
  });

  if(timeout){

    await new Promise(r => setTimeout(r, 10 * 1000));

    tx = await instance[names.timeout]({from: accounts[1]});
    gasLog.push({
        function: "timeout",
        gas: tx.receipt.gasUsed
    });

  } else {
    tx = await instance[names.open](_move1, _nonce);
    gasLog.push({
        function: "open",
        gas: tx.receipt.gasUsed
    });

  }

  assert.equal(_expection, await instance.result.call());

}

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new([accounts[0], accounts[1]]);
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Player 1 wins with rock", async() => {

    await oneRun(1,0,42,1,accounts);

  });

  it("Player 1 loses with rock", async() => {

    await oneRun(1,2,42,2,accounts);

  });

  it("Draw with rock", async() => {

    await oneRun(1,1,42,3,accounts);

  });

  it("Player 1 wins with rock (high submission)", async() => {

    await oneRun(7,6,42,1,accounts);

  });

  it("Player 1 loses with rock (high submission)", async() => {

    await oneRun(7,11,42,2,accounts);

  });

  it("Draw with rock (high submission)", async() => {

    await oneRun(7,13,42,3,accounts);

  });

  it("Player 1 wins with scissor", async() => {

    await oneRun(0,2,42,1,accounts);

  });

  it("Player 1 loses with scissor", async() => {

    await oneRun(0,1,42,2,accounts);

  });

  it("Draw with scissor", async() => {

    await oneRun(0,0,42,3,accounts);

  });

  it("Player 1 wins with paper", async() => {

    await oneRun(2,1,42,1,accounts);

  });

  it("Player 1 loses with paper", async() => {

    await oneRun(2,0,42,2,accounts);

  });

  it("Draw with paper", async() => {

    await oneRun(2,2,42,3,accounts);

  });

  it("Timeout from draw", async() => {

    await oneRun(2,2,42,2,accounts, true);

  });

  it("Timeout from P1 wins", async() => {

    await oneRun(0,2,42,2,accounts, true);

  });

  it("Timeout from P2 wins", async() => {

    await oneRun(2,0,42,2,accounts, true);

  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
