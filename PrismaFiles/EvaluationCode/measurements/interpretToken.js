const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualToken.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledToken.json'));

let comparison = {};
let categories = ["deploy", "transfer", "approve", "transferFrom"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonToken.json", JSON.stringify(comparison, null, "  "));
