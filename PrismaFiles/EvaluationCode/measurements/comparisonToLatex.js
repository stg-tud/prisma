const fs = require('fs');
const Helper = require("./helper.js");

let useCasesNames = ["TTTChannel", "TTT", "RPS", "Token", "MultiSig", "Escrow", "Crowdfunding", "Hangman", "Notary", "TTTLibrary", "TTTViaLib", "ChineseCheckers"];
let latexTuples = []

let humanReadable = "# Cost evaluation in terms of gas \n";

for(let i = 0; i < useCasesNames.length; i++){

  humanReadable = humanReadable + "## " + useCasesNames[i] + "\n" + "\n";

  let manual = JSON.parse(fs.readFileSync('./Manual' + useCasesNames[i] + '.json'));
  let compiled = JSON.parse(fs.readFileSync('./Compiled' + useCasesNames[i] + '.json'));

  let comparison = Helper.searchGrouped(manual, compiled, useCasesNames[i]);

  for(var field in comparison){

    humanReadable = humanReadable + "### " + field + "\n" + "\n";
    humanReadable = humanReadable + "- Minimal costs: " + comparison[field].min + "\n";
    humanReadable = humanReadable + "- Maximal costs: " + comparison[field].max + "\n";
    humanReadable = humanReadable + "- Average costs: " + comparison[field].max + "\n";
    humanReadable = humanReadable + "- Box plot: (" + comparison[field].plot.lw + "," + comparison[field].plot.lq + "," + comparison[field].plot.m + "," + comparison[field].plot.uq + "," + comparison[field].plot.uw + ")\n";
    humanReadable = humanReadable + "\n";

    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "Min",
      val: comparison[field].min
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "Max",
      val: comparison[field].max
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "Avg",
      val: comparison[field].average
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "BoxLW",
      val: comparison[field].plot.lw
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "BoxLQ",
      val: comparison[field].plot.lq
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "BoxM",
      val: comparison[field].plot.m
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "BoxUQ",
      val: comparison[field].plot.uq
    })
    latexTuples.push({
      name: "var" + useCasesNames[i] + field + "BoxUW",
      val: comparison[field].plot.uw
    })
  }
}

var texString = "";
for(let i in latexTuples){
  texString = texString + "\\newcommand\\" + latexTuples[i].name + "{" +  latexTuples[i].val + "}\n";
}

fs.writeFileSync("measurementResults.tex",texString);
fs.writeFileSync("humanReadableMeasurementResults.md",humanReadable);
