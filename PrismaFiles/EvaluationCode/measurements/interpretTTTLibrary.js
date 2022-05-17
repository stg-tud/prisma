const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualTTTLibrary.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledTTTLibrary.json'));

let comparison = {};
let categories = ["deploy", "exec"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonTTTLibrary.json", JSON.stringify(comparison, null, "  "));
