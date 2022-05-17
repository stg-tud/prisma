// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;
pragma experimental "ABIEncoderV2";

// !!! LoC Counted !!!

contract ManualHangman {

    enum Stage {
      Done, WaitingForCommit, WaitingForGuess, WaitingForResponse, WaitingForOpen
    }
    Stage public stage;

    address payable[] players;

    uint public hashedWord;
    uint8[26] guessed = [0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0];
    uint8[] word;
    uint8 public currentGuess;
    uint public missingletters;
    uint public tries = 5;

    constructor (address payable[] memory _players) {
        players = _players;
        stage = Stage.WaitingForCommit;
    }

    function commit(uint _hash, uint _wordLength) public { // !!!! ONLY CAPITAL LETTERS !!!!
        require(stage == Stage.WaitingForCommit,"Not in the right stage for a commitment");
        require(msg.sender == players[0], "Not the right player to act");
        hashedWord = _hash;
        missingletters = _wordLength;
        stage = Stage.WaitingForGuess;
        for(uint i = 0; i < _wordLength; i++){
          word.push(uint8(0));
        }
    }

    function guess(uint8 _letter) public {
        require(stage == Stage.WaitingForGuess,"Not in the right stage for a guess");
        require(msg.sender == players[1], "Not the right player to act");
        require(_letter - 65 < 26, "Not a valid letter");
        require(guessed[_letter - 65] == 0,"Letter has already been guessed");
        currentGuess = _letter;
        stage = Stage.WaitingForResponse;
    }

    function respond(uint[] calldata _hits) public  {
        require(stage == Stage.WaitingForResponse,"Not in the right stage for a response");
        require(msg.sender == players[0], "Not the right player to act");
        for(uint i = 0; i < _hits.length; i++){
            require(_hits[i] < word.length);
            require(word[_hits[i]] == 0, "this letter was hit before");
            word[_hits[i]] = currentGuess;
        }
        guessed[currentGuess - 65] = 1;
        if (_hits.length == 0){
            tries = tries - 1;
        } else {
            missingletters = missingletters - _hits.length;
        }
        if (missingletters == 0){
            stage = Stage.Done;
        } else if (tries == 0) {
            stage = Stage.WaitingForOpen;
        } else {
            stage = Stage.WaitingForGuess;
        }
    }

    function open(uint8[] calldata _openingWord, uint _nonce) public  {
        require(stage == Stage.WaitingForOpen, "Not in the right stage for an opening");
        require(msg.sender == players[0], "Not the right player to act");
        require(_openingWord.length == word.length);
        for(uint i = 0; i < word.length; i++){
          if(_openingWord[i] - 65 >= 26
          || guessed[_openingWord[i] - 65] == 1 && word[i] != _openingWord[i]){
              missingletters = 0;
              stage = Stage.Done;
              return ;
          }
        }
        if(uint(keccak256(abi.encode(_nonce, _openingWord))) != hashedWord){
          missingletters = 0;
          return;
        }
        stage = Stage.Done;
    }
    
    function getGuessed() public view returns(uint8[26] memory){
        return guessed;
    }
    
    function getWord() public view returns(uint8[] memory){
        return word;
    }
    
    function getParties() public view returns(address payable[] memory){
        return players;
    }
}
