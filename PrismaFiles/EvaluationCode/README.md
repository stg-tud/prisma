# How to evaluate

## Setup

- Run ganache-cli in folder Evaluation-code
- Copy compiled contract code into corresponding Solidity file, probably contracts/CompiledHangmanOnchain_v3.sol or contracts/CompiledTTTOnchain_v2.sol
	- Remember not to overwrite the contract name.
	- Remember not to overwrite the Solidity version. (The one of the compiler is to low)
- Run truffle test test/<test name>.js.
	- Name is probably "testCompiledHangmanOnchain_v3.js" or similar
- Run interpreation
	- cd measurements
	- node interpretHangmanResults.js
	- Result of interpretation is in file HangmanComparison
	
## TODO

- Improve this readme file
- Clean up the evaluation code and results


