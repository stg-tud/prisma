const Migrations = artifacts.require("Migrations");
const Dummy = artifacts.require("DummyMultisigReceiver");
const ManLib = artifacts.require("ManualTTTLibrary");
const ComLib = artifacts.require("CompiledTTTLibrary");


module.exports = function (deployer, network, accounts) {
  deployer.deploy(Migrations);
  deployer.deploy(Dummy);
  deployer.deploy(ManLib);
  deployer.deploy(ComLib);
};
