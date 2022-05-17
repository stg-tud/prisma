const fs = require('fs');
let latexTuples = []

let names = ["TTTChannel", "Token", "Crowdfunding", "Escrow", "Notary", "Hangman", "TTT", "RPS", "MultiSig", "TTTLibrary", "TTTViaLib", "ChineseCheckers"]
let results = JSON.parse(fs.readFileSync('./results/countCode.json'));

let humanReadable = "# Complexity evaluation in terms of LoC and cross-tier control flow jumps \n \n";

for (let name in results){

  humanReadable = humanReadable + "## " + name + "\n" + "\n";
  humanReadable = humanReadable + "- Prisma: " + results[name].prismaLoC + "\n";
  humanReadable = humanReadable + "- Solidity: " + results[name].solidityLoC + "\n";
  humanReadable = humanReadable + "- JavaScript: " + results[name].jsLoC + "\n";
  humanReadable = humanReadable + "- Difference: " + results[name].diff + "\n";
  humanReadable = humanReadable + "- Cross-tier control flow jumps: " + results[name].tierJumps + "\n \n";

  latexTuples.push({
    name: "var" + name + "LoCPrisma",
    val: results[name].prismaLoC
  })
  latexTuples.push({
    name: "var" + name + "LoCSol",
    val: results[name].solidityLoC
  })
  latexTuples.push({
    name: "var" + name + "LoCJS",
    val: results[name].jsLoC
  })

  latexTuples.push({
    name: "var" + name + "Diff",
    val: results[name].diff
  })

  latexTuples.push({
    name: "var" + name + "RC",
    val: results[name].tierJumps
  })
}

var texString = "";
for(let i in latexTuples){
  texString = texString + "\\newcommand\\" + latexTuples[i].name + "{" +  latexTuples[i].val + "}\n";
}

fs.writeFileSync("./results/codeResults.tex",texString);
fs.writeFileSync("./results/humanReadableCodeResults.md",humanReadable);
console.log("Done");
