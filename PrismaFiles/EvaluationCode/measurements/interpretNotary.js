const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualNotary.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledNotary.json'));

let comparison = {};
let categories = ["deploy", "submit"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonNotary.json", JSON.stringify(comparison, null, "  "));
