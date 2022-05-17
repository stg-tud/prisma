let initialChannelState = function(){
  return {
    "channelID": '0x0000000000000000000000000000000000000000000000000000000000000000',
    "version": 0,
    "outcome": {
                "assets": ["0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0", "0x22d491Bde2303f2f43325b2108D26f1eAbA1e32b"],
                "balances": [[1,1],[1,1]],
                "locked": []
            },
    "appData": {
      "hashedWord": '0x0000000000000000000000000000000000000000000000000000000000000000',
      "guessed": [false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],
      "word": '0xAA',
      "currentGuess": '0xBB',
      "missingLetters": 0,
      "tries": 0,
      "stage": 0,
      "lastActionType": 0,
      "lastAction": '0xCCCC'
    },
    "isFinal": false
  }
}

let toNextChannelState = function(lastState, newAppState){
  let resultingState = initialChannelState();
  resultingState.version = lastState.version + 1;
  resultingState.appData = newAppState;

  if(newAppState.stage > 3){      //Failed = 4, Success = 5
    resultingState.isFinal = true;
    let winner = 0;
    if (newAppState.stage == 5){
      winner = 1;
    }
    for(let i = 0; i < lastState.outcome.assets.length; i++){
      resultingState.outcome.balances[i][winner] = lastState.outcome.balances[i][winner] + lastState.outcome.balances[i][1-winner];
      resultingState.outcome.balances[i][1-winner] = 0;
    }
  }

  return resultingState;
}

let polishAppState = function(state, lastMove, lastMoveType){
  return {
    "hashedWord": state.hashedWord,
    "guessed": state.guessed,
    "word": state.word,
    "currentGuess": state.currentGuess,
    "missingLetters": state.missingLetters,
    "tries": state.tries,
    "stage": state.stage,
    "lastActionType": lastMoveType,
    "lastAction": lastMove
  }
}

let encodeAppState = function(state) {
  let copy = JSON.parse(JSON.stringify(state));
  copy.appData = web3.eth.abi.encodeParameters([
    {
      "AppState": {
        "hashedWord": 'bytes32',
        "guessed": 'bool[26]',
        "word": 'bytes',
        "currentGuess": 'bytes1',
        "missingLetters": 'uint256',
        "tries": 'uint256',
        "stage": 'uint8',
        "lastActionType": 'uint8',
        "lastAction": 'bytes'
      }
    }
  ], [
    state.appData
  ]);
  return copy;
}

let getSuccessfulGuessingMoves = function(){

  let nonce = web3.utils.keccak256(web3.eth.abi.encodeParameters(['string'], ['The-nonce']));
  let commitment = web3.utils.keccak256(web3.eth.abi.encodeParameters(['string', 'bytes32'], ["TEST", nonce]));

  return [
    {
      type: 0,
      moveEncoding: ['uint','bytes32'],
      moveParams: [4,commitment]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "T".charCodeAt(0).toString(16)]
    },
    {
      type: 2,
      moveEncoding: ['uint8[]'],
      moveParams: [[0,3]]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "A".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "E".charCodeAt(0).toString(16)]
    },
    {
      type: 2,
      moveEncoding: ['uint8[]'],
      moveParams: [[1]]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "N".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "S".charCodeAt(0).toString(16)]
    },
    {
      type: 2,
      moveEncoding: ['uint8[]'],
      moveParams: [[2]]
    }
  ];
}

let getFailedGuessingMoves = function(){

  let nonce = web3.utils.keccak256(web3.eth.abi.encodeParameters(['string'], ['The-nonce']));
  let commitment = web3.utils.keccak256(web3.eth.abi.encodeParameters(['string', 'bytes32'], ["HANGMAN", nonce]));

  return [
    {
      type: 0,
      moveEncoding: ['uint','bytes32'],
      moveParams: [7,commitment]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "S".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "T".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "O".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "A".charCodeAt(0).toString(16)]
    },
    {
      type: 2,
      moveEncoding: ['uint8[]'],
      moveParams: [[1,5]]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "K".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "N".charCodeAt(0).toString(16)]
    },
    {
      type: 2,
      moveEncoding: ['uint8[]'],
      moveParams: [[2,6]]
    },
    {
      type: 1,
      moveEncoding: ['bytes1'],
      moveParams: ["0x" + "R".charCodeAt(0).toString(16)]
    },
    {
      type: 3,
      moveEncoding: ['bytes1'],
      moveParams: ["0x00"]
    },
    {
      type: 4,
      moveEncoding: ['string', 'bytes32'],
      moveParams: ["HANGMAN", nonce]
    }
  ];
}

module.exports = {
  initialChannelState,
  toNextChannelState,
  polishAppState,
  encodeAppState,
  getSuccessfulGuessingMoves,
  getFailedGuessingMoves
}
