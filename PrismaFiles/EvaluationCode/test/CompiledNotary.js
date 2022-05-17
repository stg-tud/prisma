let names = {
  testName: "CompiledNotary",
  testedContract : "CompiledNotary",
  submit: "submit"
}

const Contract = artifacts.require(names.testedContract);

let gasLog = [];

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    let instance = await Contract.new();
    let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

    gasLog.push({function:"deploy", gas:receipt.gasUsed});
  });

  it("Measure ascending", async() => {

    let instance = await Contract.new();
    let tx;

    const genRanHex = size => [...Array(size)].map(() => Math.floor(Math.random() * 16).toString(16)).join('');

    for(let i = 0; i < 100; i++){
      tx = await instance[names.submit]("0x" + genRanHex(i * 2),{from: accounts[i % 10]});
      gasLog.push({
          function: "submit-" + i,
          gas: tx.receipt.gasUsed
      });
    }
  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
