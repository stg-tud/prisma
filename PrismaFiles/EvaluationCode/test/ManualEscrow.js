let names = {
  testName: "ManualEscrow",
  testedContract : "ManualEscrow",
  pay: "pay",
  confirmDelivery : "confirmDelivery",
  arbitrate : "arbitrate"
}

const Contract = artifacts.require(names.testedContract);

let gasLog = [];

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new(accounts[0], accounts[1], accounts[2], "1000000000000000000");
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Pay + Confirm", async() => {

    let instance = await Contract.new(accounts[0], accounts[1], accounts[2], "1000000000000000000");
    let tx;

    tx = await instance[names.pay]({from: accounts[0], value: "1000000000000000000"});
    gasLog.push({
        function: names.pay,
        gas: tx.receipt.gasUsed
    });

    tx = await instance[names.confirmDelivery]({from: accounts[0]});
    gasLog.push({
        function: names.confirmDelivery,
        gas: tx.receipt.gasUsed
    });

  });

  it("Pay + Arbiter enforces transfer", async() => {

    let instance = await Contract.new(accounts[0], accounts[1], accounts[2], "1000000000000000000");
    let tx;

    tx = await instance[names.pay]({from: accounts[0], value: "1000000000000000000"});
    gasLog.push({
        function: names.pay,
        gas: tx.receipt.gasUsed
    });

    tx = await instance[names.arbitrate](0, {from: accounts[2]});
    gasLog.push({
        function: names.arbitrate,
        gas: tx.receipt.gasUsed
    });

  });

  it("Pay + Arbiter enforces refund", async() => {

    let instance = await Contract.new(accounts[0], accounts[1], accounts[2], "1000000000000000000");
    let tx;

    tx = await instance[names.pay]({from: accounts[0], value: "1000000000000000000"});
    gasLog.push({
        function: names.pay,
        gas: tx.receipt.gasUsed
    });

    tx = await instance[names.arbitrate](1, {from: accounts[2]});
    gasLog.push({
        function: names.arbitrate,
        gas: tx.receipt.gasUsed
    });

  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
