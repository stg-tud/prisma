// SPDX-License-Identifier: UNLICENSED
// compiled with GUARD_ERROR_MESSAGES=false
pragma solidity >=0.8.0;
pragma abicoder v2;

contract CompiledTTT  {
  address payable[] private players;
  struct UU {
    uint8 x;
    uint8 y;
  } 
  uint8 public moves;
  uint8[3][3] private board;
  function move2(uint8 x, uint8 y)  private {
    require((board[uint256(x)][uint256(y)] == uint8(0)), "Field already occupied");
    uint8 pos = ((moves % uint8(2)) + uint8(2));
    (board[uint256(x)][uint256(y)]) = pos;
    if (((((board[uint256(x)][uint256(0)] == board[uint256(x)][uint256(1)]) && (board[uint256(x)][uint256(1)] == board[uint256(x)][uint256(2)])) || ((board[uint256(0)][uint256(y)] == board[uint256(1)][uint256(y)]) && (board[uint256(1)][uint256(y)] == board[uint256(2)][uint256(y)]))) || ((board[uint256(1)][uint256(1)] == pos) && (((board[uint256(0)][uint256(2)] == board[uint256(1)][uint256(1)]) && (board[uint256(2)][uint256(0)] == board[uint256(1)][uint256(1)])) || ((board[uint256(0)][uint256(0)] == board[uint256(1)][uint256(1)]) && (board[uint256(2)][uint256(2)] == board[uint256(1)][uint256(1)])))))) {
      (moves) = (uint8(10) + pos);
    } else {
      (moves) = (moves + uint8(1));
    } 
  } 
  function move(UU memory pair)  public {
    require(((state == uint32(0x3c1d1878)) && (players[uint256((moves % uint8(2)))] == payable(msg.sender))), "access control or flow guard failed");
    move2(pair.x, pair.y);
    private$whil$macro$3();
  } 
  function private$whil$macro$3()  private {
    if ((moves < uint8(9))) {
      (state) = uint32(0x3c1d1878);
    } else {
      (state) = 0;
    } 
  } 
  constructor(address payable[] memory players$) {
    players = players$;
    moves = uint8(0);
    board = [[uint8(0), uint8(0), uint8(0)], [uint8(0), uint8(0), uint8(0)], [uint8(0), uint8(0), uint8(0)]];
    private$whil$macro$3();
    uint8(0);
    
  } 
  function get_players()  public view returns (address payable[] memory result) {
    address payable[] memory tmp = players;
    return(tmp);
  } 
  function get_board()  public view returns (uint8[3][3] memory result) {
    uint8[3][3] memory tmp = board;
    return(tmp);
  } 
  
  uint32 public state;
  
  
  
  
  

} 