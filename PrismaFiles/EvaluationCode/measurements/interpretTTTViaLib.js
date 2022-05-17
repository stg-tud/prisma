const fs = require('fs');
const Helper = require("./helper.js");

var manual = JSON.parse(fs.readFileSync('./ManualTTTViaLib.json'));
var compiled = JSON.parse(fs.readFileSync('./CompiledTTTViaLib.json'));

let comparison = {};
let categories = ["deploy", "move"];

Helper.search(manual, compiled, comparison, categories, "startsWith", "", "");

fs.writeFileSync("./comparisonTTTviaLib.json", JSON.stringify(comparison, null, "  "));
