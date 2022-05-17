module.exports = {
    tests(names) {

        const Contract = artifacts.require(names.testedContract);
        let gasLog = [];

        contract("Measure " + names.testName, async accounts => {

          it("Deployment", async () => {

            let instance = await Contract.new("100000000");
            let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

            gasLog.push({function:"deploy", gas:receipt.gasUsed});
          });

          it("Transfers", async() => {

            let instance = await Contract.new("1000000");

            let tx;

            for(let i = 1; i < accounts.length; i ++){
              tx = await instance[names.transfer](accounts[i], 100, {from: accounts[0]});
              gasLog.push({
                  function: "transfer",
                  gas: tx.receipt.gasUsed
              });
            }

            for(let i = 0; i < accounts.length; i ++){
              for(let j = 0; j < accounts.length; j ++){
                if(i == j) continue;
                tx = await instance[names.transfer](accounts[j], 10, {from: accounts[i]});
                gasLog.push({
                    function: "transfer",
                    gas: tx.receipt.gasUsed
                });
              }
            }
          });

          it("Approve and transfer from", async() => {

            let instance = await Contract.new("1000000");

            let tx;

            for(let i = 1; i < accounts.length; i ++){
              tx = await instance[names.transfer](accounts[i], 10000, {from: accounts[0]});
              gasLog.push({
                  function: "transfer",
                  gas: tx.receipt.gasUsed
              });
            }

            for(let i = 0; i < accounts.length; i ++){
              for(let j = 0; j < accounts.length; j ++){
                if(i == j) continue;
                tx = await instance[names.approve](accounts[j], 10, {from: accounts[i]});
                gasLog.push({
                    function: "approve",
                    gas: tx.receipt.gasUsed
                });
              }
            }

            for(let i = 0; i < accounts.length; i ++){
              for(let j = 0; j < accounts.length; j ++){
                if(i == j) continue;
                tx = await instance[names.transferFrom](accounts[j], accounts[(j+1)%10], 10, {from: accounts[i]});
                gasLog.push({
                    function: "transferFrom",
                    gas: tx.receipt.gasUsed
                });
              }
            }
          });

          it("Write to file", async () => {
            require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
          });
        });
    }
}
