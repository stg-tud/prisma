contract("Determine constant overhead", async accounts => {
  it("Deployment", async () => {

    for(let i = 1; i <= 4; i++){
      let instance = await artifacts.require("Test" + i).new();
      let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);
      // console.log("Deployment of Test"+i);
      // console.log(receipt.gasUsed);
    }

  });
});
