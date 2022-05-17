let names = {
  testName: "CompiledTTTLibrary",
  testedContract : "CompiledTTTLibrary"
}

const Contract = artifacts.require(names.testedContract);

let gasLog = [];

contract("Measure " + names.testName, async accounts => {

  it("Deployment", async () => {

    let instance = await Contract.new();
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
    gasLog.push({function:"exec", gas: 0})
  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });

});
