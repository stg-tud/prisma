const { exec } = require("child_process");
const fs = require('fs');

let names = ["TTTChannel", "Token", "Crowdfunding", "Escrow", "Notary", "Hangman", "TTT", "RPS", "MultiSig", "TTTLibrary", "TTTViaLib", "ChineseCheckers"]

let code;
let aggregated = []

for (var i = 0; i < names.length; i++){
	code = JSON.parse(fs.readFileSync('./../build/contracts/Manual' + names[i]  + '.json')).bytecode
	console.log(names[i] + " - Manual - " + ((code.length - 2) / 2))
	code = JSON.parse(fs.readFileSync('./../build/contracts/Compiled' + names[i]  + '.json')).bytecode
	console.log(names[i] + " - Compiled - " + ((code.length - 2) / 2))
	aggregated.push(((code.length - 2) / 2))
}

aggregated.sort()
console.log(aggregated)
