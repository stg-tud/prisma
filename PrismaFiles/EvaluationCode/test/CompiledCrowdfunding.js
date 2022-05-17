let names = {
  testName: "CompiledCrowdfunding",
  testedContract : "CompiledCrowdfunding",
  fund: "allInOne_payable",
  refund : "allInOne_payable",
  claim : "allInOne_payable"
}

const Contract = artifacts.require(names.testedContract);

let gasLog = [];

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new("1000000000000000", Math.floor(Date.now()/1000) + 10);
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Successful campaign", async() => {

    let instance = await Contract.new("1000000000000000", Math.floor(Date.now()/1000) + 5);

    let tx;

    for(let i = 0; i < accounts.length; i ++){

      tx = await instance[names.fund](0,{from: accounts[9-i], value: "200000000000000"});
      gasLog.push({
          function: "fund",
          gas: tx.receipt.gasUsed
      });

    }

    await new Promise(r => setTimeout(r, 6 * 1000));

    tx = await instance[names.claim](0,{from: accounts[0]});
    gasLog.push({
        function: "claim",
        gas: tx.receipt.gasUsed
    });
  });

  it("Failed campaign", async() => {

    let instance = await Contract.new("1000000000000000", Math.floor(Date.now()/1000) + 5);

    let tx;

    for(let i = 1; i < accounts.length; i ++){

      tx = await instance[names.fund](0, {from: accounts[9-i], value: "10000000000000"});
      gasLog.push({
          function: "fund",
          gas: tx.receipt.gasUsed
      });

    }

    await new Promise(r => setTimeout(r, 6 * 1000));

    for(let i = 1; i < accounts.length; i ++){
      tx = await instance[names.refund](0, {from: accounts[9-i]});
      gasLog.push({
          function: "refund",
          gas: tx.receipt.gasUsed
      });

    }
  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
