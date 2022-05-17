const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualTTT.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledTTT.json'));

let comparison = {};
let categories = ["deploy", "move"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonTTT.json", JSON.stringify(comparison, null, "  "));
