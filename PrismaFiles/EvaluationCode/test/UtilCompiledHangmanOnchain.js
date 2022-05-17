module.exports = {
    tests(names, executeOpt) {

        let defaultExecute = async function(instance, wordUnformatted, guessesUnformatted, accounts, cheat) {

          //Transform word and guesses to integers
          let word = wordUnformatted.split("").map(c => c.charCodeAt(0));
          let guesses = guessesUnformatted.split("").map(c => c.charCodeAt(0));

          let tx;
          let consumptions = [];
          let nonce = 42;

          let commitment = web3.utils.keccak256(web3.eth.abi.encodeParameter(
              {
                  "UArrU": {
                      "arr": 'uint8[]',
                      "x": 'uint256'
                  }
              },
              {
                  "arr": word,
                  "x": nonce,
              }
          ));
          // let wordHash = BigInt(web3.utils.keccak256(web3.eth.abi.encodeParameters(['uint[]'], [word])));
          // let nonceHash = BigInt(web3.utils.keccak256(web3.eth.abi.encodeParameters(['uint'], [nonce])));
          // let commitment =
          //   'correctHashing' in names
          //   ? web3.utils.keccak256(web3.eth.abi.encodeParameters(['uint', 'uint[]'], [nonce, word]))
          //   : "0x" + ((wordHash + nonceHash) % BigInt(0x10000000000000000000000000000000000000000000000000000000000000000)).toString(16);

          if ('beforeAllMethod' in names) {
            tx = await instance[names.beforeAllMethod]([accounts[0], accounts[1]]);
            consumptions.push({ function: "init", gas: tx.receipt.gasUsed });
          }

          if (names['separateArguments'])
            tx = await instance[names.commitMethod](commitment, word.length);
          else
            tx = await instance[names.commitMethod]([commitment, word.length]);
          consumptions.push({ function: "commit", gas: tx.receipt.gasUsed });

          let occurences = [];
          let found = 0;
          let failed = 0;
          for(var i = 0; i < guesses.length; i++){

            tx = await instance[names.guessMethod](guesses[i], {from: accounts[1]});
            consumptions.push({ function: "guess", gas: tx.receipt.gasUsed });

            occurences = [];
            for(var j = 0; j < word.length; j++) {
              if(guesses[i] == word[j]) {
                occurences.push(j);
              }
            }

            found = found + occurences.length;
            if(occurences.length == 0) failed = failed + 1;
            if(found == word.length || failed == 5) i = guesses.length;
            if(cheat && occurences.length >= 2) occurences.pop();

            tx = await instance[names.respondMethod](occurences);
            consumptions.push({
              function: "respond",
              occurences: occurences.length,
              cheat: cheat,
              gas: tx.receipt.gasUsed
            });
          }

          if(found != word.length){
            if (names['separateArguments'])
              tx = await instance[names.openMethod](word,nonce);
            else
              tx = await instance[names.openMethod]([word,nonce]);
            consumptions.push({
              function: "open",
              gas: tx.receipt.gasUsed
            });
          }
          return consumptions;
        }

        let execute = executeOpt || defaultExecute;
        const HangmanContract = artifacts.require(names.testedHangmanContract);
        let gasLogFine = [];
        let gasLog = [];

        contract("Measure " + names.testName, async accounts => {
          it("Deployment", async () => {

            let instance = names.passPartiesToConstructor
              ? await HangmanContract.new([accounts[0], accounts[1]])
              : await HangmanContract.new();
            let receipt = await web3.eth.getTransactionReceipt(instance.transactionHash);

            gasLog.push({function:"deploy", gas:receipt.gasUsed});
            gasLogFine.push({function:"deploy", gas:receipt.gasUsed});
          });

          it("Guessing 'TEST' successful with error a and n", async() => {

            let instance = names.passPartiesToConstructor
              ? await HangmanContract.new([accounts[0], accounts[1]])
              : await HangmanContract.new();
            let word = "TEST";
            let guesses = "TAENS";
            let expectSuccess = true;
            cheat = false;
            let consumptions = await execute(instance,word,guesses, accounts, cheat);

            let missingletters = await instance[names.missingLetters].call();
            if (expectSuccess){
              assert.equal(missingletters, 0);
            } else {
              assert.notEqual(missingletters, 0);
            }

            gasLogFine.push({
              word: word,
              cheat: cheat,
              guesses: guesses,
              success: expectSuccess,
              consumptions: consumptions
            });
            gasLog = gasLog.concat(consumptions);

          });

          it("Guessing 'HANGMAN' unsuccessfully with error s,t,o,k, and r and correct a and n", async() => {

            let instance = names.passPartiesToConstructor
              ? await HangmanContract.new([accounts[0], accounts[1]])
              : await HangmanContract.new();
            let word = "HANGMAN";
            let guesses = "STOAKNR";
            let expectSuccess = false;
            cheat = false;
            let consumptions = await execute(instance,word,guesses, accounts, cheat);

            let missingletters = await instance[names.missingLetters].call();
            if(expectSuccess){
              assert.equal(missingletters, 0);
            } else {
              assert.notEqual(missingletters, 0);
            }

            gasLogFine.push({
              word: word,
              cheat: cheat,
              guesses: guesses,
              success: expectSuccess,
              consumptions: consumptions
            });
            gasLog = gasLog.concat(consumptions);

          });

          it("Guessing 'HANGMAN' unsuccessfully with error s,t,o,k, and r and correct a and n and challenger cheating at position 5 (not opening the second a)", async() => {

            let instance = names.passPartiesToConstructor
              ? await HangmanContract.new([accounts[0], accounts[1]])
              : await HangmanContract.new();
            let word = "HANGMAN";
            let guesses = "STOAKNR";
            let expectSuccess = true;
            cheat = true;
            let consumptions = await execute(instance,word,guesses, accounts, cheat);

            let missingletters = await instance[names.missingLetters].call();
            if(expectSuccess){
              assert.equal(missingletters, 0);
            } else {
              assert.notEqual(missingletters, 0);
            }

            gasLogFine.push({
              word: word,
              cheat: cheat,
              guesses: guesses,
              success: expectSuccess,
              consumptions: consumptions
            });
            gasLog = gasLog.concat(consumptions);
          });

          it("Guess long word ('UNREALISTICLONGWORD')", async() => {

            let instance = names.passPartiesToConstructor
              ? await HangmanContract.new([accounts[0], accounts[1]])
              : await HangmanContract.new();
            let word = "UNREALISTICLONGWORD";
            let guesses = "UNREALISTCOGWD";
            let expectSuccess = true;
            cheat = false;
            let consumptions = await execute(instance,word,guesses, accounts, cheat);

            let missingletters = await instance[names.missingLetters].call();
            if(expectSuccess){
              assert.equal(missingletters, 0);
            } else {
              assert.notEqual(missingletters, 0);
            }

            gasLogFine.push({
              word: word,
              cheat: cheat,
              guesses: guesses,
              success: expectSuccess,
              consumptions: consumptions
            });
            gasLog = gasLog.concat(consumptions);
          });

          it("Guess longer word ('UNREALISTICLONGWORDSOLONGASNEVERBEFORE')", async() => {

            let instance = names.passPartiesToConstructor
              ? await HangmanContract.new([accounts[0], accounts[1]])
              : await HangmanContract.new();
            let word = "UNREALISTICLONGWORDSOLONGASNEVERBEFOREZ";
            let guesses = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            let expectSuccess = false;
            cheat = false;
            let consumptions = await execute(instance,word,guesses, accounts, cheat);

            let missingletters = await instance[names.missingLetters].call();
            if(expectSuccess){
              assert.equal(missingletters, 0);
            } else {
              assert.notEqual(missingletters, 0);
            }

            gasLogFine.push({
              word: word,
              cheat: cheat,
              guesses: guesses,
              success: expectSuccess,
              consumptions: consumptions
            });
            gasLog = gasLog.concat(consumptions);
          });

          it("Write to file", async () => {
            require('fs').writeFileSync("./measurements/" + names.testName + ".json", JSON.stringify(gasLog, null, "  "));
            require('fs').writeFileSync("./measurements/" + names.testName + "Fine.json", JSON.stringify(gasLogFine, null, "  "));
          });
        });
    }
}
