const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualHangman.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledHangman.json'));

let comparison = {};
let categories = ["deploy", "init", "commit", "guess", "respond", "open"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonHangman.json", JSON.stringify(comparison, null, "  "));
