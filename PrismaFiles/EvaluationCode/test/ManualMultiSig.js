let names = {
  testName: "ManualMultiSig",
  testedContract : "ManualMultiSig",
  targetContract : "DummyMultisigReceiver",
  executeMethod : "execute"
}

const Contract = artifacts.require(names.testedContract);
const Target = artifacts.require(names.targetContract);

let privateKeys = [
  "0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d",
  "0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1",
  "0x6370fd033278c143179d81c5526140625662b8daa446c22ee2d73db3707e620c",
  "0x646f1ce2fdad0e6deeeb5c7e8e5543bdde65e86029e2fd9fc169899c440a7913",
  "0xadd53f9a7e588d003326d1cbf9e4a43c061aadd9bc938c843a79e7b4fd2ad743",
  "0x395df67f0c2d2d9fe1ad08d1bc8b6627011959b79c53d7dd6a3536a33ab8a4fd",
  "0xe485d098507f54e7733a205420dfddbe58db035fa577fc294ebd14db90767a52",
  "0xa453611d9419d0e56f499079478fd72c37b251a94bfde4d19872c44cf65386e3",
  "0x829e924fdf021ba3dbbc4225edfece9aca04b929d6e75613329ca6f1d31c0bb4",
  "0xb0057716d5917badaf911b193b12b910811c1497b5bada8d7711f758981c3773"
];

let shortBytes = "0xABCDEFABCDEFABCDEFABCDEF";
let longBytes = "0xABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCDEF";

let gasLog = [];

let oneIteration = async function(instance, target, threshold, accounts, surfix){

  await submitMultiSigTx(instance, target, "receiveNone", null, null, threshold, 0, accounts, "execute-none-" + surfix);
  await submitMultiSigTx(instance, target, "receiveOne", 42, null, threshold, 0, accounts, "execute-one-"+ surfix);
  await submitMultiSigTx(instance, target, "receiveTwo", 42, 4000, threshold, 0, accounts, "execute-two-"+ surfix);
  await submitMultiSigTx(instance, target, "receiveMore", shortBytes, null, threshold, 0, accounts, "execute-short-bytes-"+ surfix);
  await submitMultiSigTx(instance, target, "receiveMore", longBytes, null, threshold, 0, accounts, "execute-long-bytes-"+ surfix);
  await instance.send(100000000);
  await submitMultiSigTx(instance, target, "receiveCoins", null, null, threshold, 100000000, accounts, "execute-coins-"+ surfix);

}

let submitMultiSigTx = async function(instance, target, functionName, data1, data2, threshold, value, accounts, logName){

  let txData = encodeData(target, functionName, data1, data2);
  let timeout = (Date.now() / 1000 | 0) + 60 * 60;
  let unsorted = [];
  let signatures = [];
  let indices = [];
  let nonce = await instance.nonce.call();

  let prefix = function (x, length){
    return "0x" + x.substring(2).padStart(length * 2, "0")
  }

  let encodePacked = prefix(instance.address, 16) + prefix("0x" + nonce.toString(16), 32).substring(2) + prefix("0x" + timeout.toString(16), 32).substring(2) + prefix(target.address, 16).substring(2) + prefix("0x" + value.toString(16), 32).substring(2) + txData.substring(2);

  let hash = web3.utils.keccak256(encodePacked);

  for (let i = 0; i < threshold; i++){
    let signature = await web3.eth.accounts.sign(hash, privateKeys[i]);
    unsorted.push({
      address: accounts[i],
      signature: signature,
      index: i
    })
  }

  let sorted = unsorted.sort(function(left,right){
    if(left.address == right.address){
      return 0;
    } else if(parseInt(left.address,16) < parseInt(right.address)){
      return -1;
    } else {
      return 1;
    }
  });

  for(let i = 0; i < threshold; i++){
    signatures.push(sorted[i].signature);
    indices.push(sorted[i].index);
  }

  let tx = await instance[names.executeMethod](signatures, indices, timeout, target.address, value, txData);
  gasLog.push({
      function: logName,
      gas: tx.receipt.gasUsed
  });

}

let encodeData = function(target, name, data1, data2){
  if (!data1){
    return target.contract.methods[name]().encodeABI();
  } else if(data2){
    return target.contract.methods[name](data1, data2).encodeABI();
  } else {
    return target.contract.methods[name](data1).encodeABI();
  }
}

contract("Measure " + names.testName, async accounts => {
  it("Deployment", async () => {

    for(let i = 2; i < 11; i++){
      let instance = await Contract.new(Math.round(i / 2), accounts.slice(0, i));
      let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);
      gasLog.push({function:"deploy", gas:receipt.gasUsed});
    }
  });

  it("Execute(2-out-of-3)", async() => {

    let instance = await Contract.new(2, accounts.slice(0, 3));
    let target = await Target.new();

    await oneIteration(instance, target, 2,  accounts, "2of3");
  });

  it("Execute(2-out-of-4)", async() => {

    let instance = await Contract.new(2, accounts.slice(0, 4));
    let target = await Target.new();

    await oneIteration(instance, target, 2,  accounts, "2of4");
  });

  it("Execute(3-out-of-4)", async() => {

    let instance = await Contract.new(3, accounts.slice(0, 4));
    let target = await Target.new();

    await oneIteration(instance, target, 3,  accounts, "3of4");
  });

  it("Execute(3-out-of-5)", async() => {

    let instance = await Contract.new(3, accounts.slice(0, 5));
    let target = await Target.new();

    await oneIteration(instance, target, 3,  accounts, "3of5");
  });

  it("Execute(4-out-of-5)", async() => {

    let instance = await Contract.new(4, accounts.slice(0, 5));
    let target = await Target.new();

    await oneIteration(instance, target, 4,  accounts, "4of5");
  });

  it("Execute(5-out-of-8)", async() => {

    let instance = await Contract.new(5, accounts.slice(0, 8));
    let target = await Target.new();

    await oneIteration(instance, target, 5,  accounts, "5of8");
  });

  it("Execute(8-out-of-8)", async() => {

    let instance = await Contract.new(8, accounts.slice(0, 8));
    let target = await Target.new();

    await oneIteration(instance, target, 8,  accounts, "8of8");
  });

  it("Execute(10-out-of-10)", async() => {

    let instance = await Contract.new(10, accounts.slice(0, 10));
    let target = await Target.new();

    await oneIteration(instance, target, 10,  accounts, "10of10");
  });

  it("Write to file", async () => {
    require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
  });
});
