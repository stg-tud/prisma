const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualRPS.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledRPS.json'));

let comparison = {};
let categories = ["deploy", "commit", "move", "open", "timeout"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "*");

fs.writeFileSync("./comparisonRPS.json", JSON.stringify(comparison, null, "  "));
