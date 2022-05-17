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
      "moves": 0,
      "board": [[0,0,0],[0,0,0],[0,0,0]],
      "lastAction": '0x00'
    },
    "isFinal": false
  }
}

let toNextChannelState = function(lastState, newAppState){
  let resultingState = initialChannelState();
  resultingState.version = lastState.version + 1;
  resultingState.appData = newAppState;

  if(newAppState.moves >= 9){
    resultingState.isFinal = true;
    if(newAppState.moves > 9){
      let winner = newAppState.moves - 10;
      for(let i = 0; i < lastState.outcome.assets.length; i++){
        resultingState.outcome.balances[i][winner] = lastState.outcome.balances[i][winner] + lastState.outcome.balances[i][1-winner];
        resultingState.outcome.balances[i][1-winner] = 0;
      }
    }
  }
  return resultingState;
}

let polishAppState = function(state, lastMove){
  return {
    "moves": state.moves,
    "board": state.board,
    "lastAction": lastMove
  }
}

let encodeAppState = function(state) {
  let copy = JSON.parse(JSON.stringify(state));
  copy.appData = web3.eth.abi.encodeParameters([
    {
      "AppState": {
        "moves": 'uint8',
        "board": 'uint8[3][3]',
        "lastAction": 'bytes'
      }
    }
  ], [
    state.appData
  ]);
  return copy;
}

let winSevenMoves = [[0,0],[2,2],[0,2],[0,1],[2,0],[1,1],[1,0]];
let winEightMoves = [[0,0],[2,2],[0,2],[0,1],[2,0],[1,1],[1,2],[2,1]];
let drawMoves =     [[0,0],[2,2],[0,2],[0,1],[2,0],[1,1],[1,2],[1,0],[2,1]];

module.exports = {
  initialChannelState,
  toNextChannelState,
  polishAppState,
  encodeAppState,
  winSevenMoves,
  winEightMoves,
  drawMoves
}
