const { exec } = require("child_process");
const fs = require('fs');

let names = ["TTTChannel", "Token", "Crowdfunding", "Escrow", "Notary", "Hangman", "TTT", "RPS", "MultiSig", "TTTLibrary", "TTTViaLib", "ChineseCheckers"]
let names2 = [{top:"testTTTChannel",down:"TestTTTChannel"},{top:"testToken",down:"TestToken"},{top:"testCrowdfunding",down:"TestCrowdfunding"},{top:"testEscrow",down:"TestEscrow"},{top:"testNotary",down:"TestNotary"},{top:"testHangman",down:"TestHangman"},{top:"testTTT",down:"TestTTT"},{top:"testRPS",down:"TestRPS"},{top:"testMultiSig",down:"TestMultiSig"},{top:"testTTTLibrary",down:"TestTTTLibrary"},{top:"testTTTViaLib",down:"TestTTTViaLib"},{top:"testChineseCheckers",down:"TestChineseCheckers"}]
let json = {}

let callIt = async function(name){

  for (var i = 0; i < names.length; i++){
    let contractLoC = JSON.parse(
      await new Promise(
        resolve => exec("cloc ./../contracts/Manual" + names[i] + ".sol --json", (error, stdout, stderr) => {
          if (error) {
            console.log(`error: ${error.message}`);
            resolve({"SUM": {code: 99999999}})
          } else if (stderr) {
            console.log(`stderr: ${stderr}`);
            resolve({"SUM": {code: 99999999}})
          } else {
            resolve(stdout);
          }
      }))
    )["SUM"].code

    let clientLoC = JSON.parse(
      await new Promise(
        resolve => exec("cloc ./AllClients/" + names[i] + "ClientCode.js --json", (error, stdout, stderr) => {
          if (error) {
            console.log(`error: ${error.message}`);
            resolve({"SUM": {code: 99999999}})
          } else if (stderr) {
            console.log(`stderr: ${stderr}`);
            resolve({"SUM": {code: 99999999}})
          } else {
            resolve(stdout);
          }
      }))
    )["SUM"].code

    let prismaLoC = JSON.parse(
      await new Promise(
        resolve => exec("cloc ./../../CompilerCode/"+ names2[i].top +"/src/main/scala/" + names2[i].down + ".scala --json", (error, stdout, stderr) => {
          if (error) {
            console.log(`error: ${error.message}`);
            resolve({"SUM": {code: 99999999}})
          } else if (stderr) {
            console.log(`stderr: ${stderr}`);
            resolve({"SUM": {code: 99999999}})
          } else {
            resolve(stdout);
          }
      }))
    )["SUM"].code

    let file = await fs.readFileSync("./AllClients/" + names[i] + "ClientCode.js", 'utf8')

    json[names[i]] = {
      prismaLoC: prismaLoC,
      solidityLoC: contractLoC,
      jsLoC: clientLoC,
      diff: clientLoC + contractLoC - prismaLoC,
      perc: prismaLoC / (clientLoC + contractLoC),
      tierJumps: (file.match(/await[ ]+(contract|Contract|instance).(methods|deploy)/g) || []).length,
      interactionPerClientLine: (file.match(/await[ ]+(contract|Contract|instance).(methods|deploy)/g) || []).length / clientLoC
    }
  }

  fs.writeFileSync("./results/countCode.json", JSON.stringify(json, null, "  "));
  console.log("Done");
}

callIt();
